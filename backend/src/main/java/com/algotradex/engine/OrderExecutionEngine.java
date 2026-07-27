package com.algotradex.engine;

import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Instrument;
import com.algotradex.model.Order;
import com.algotradex.model.Strategy;
import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.*;
import com.algotradex.repository.BrokerAccountRepository;
import com.algotradex.repository.InstrumentRepository;
import com.algotradex.repository.OrderRepository;
import com.algotradex.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutionEngine {

    private final InstrumentRepository instrumentRepository;
    private final BrokerAccountRepository brokerAccountRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final com.algotradex.repository.PositionRepository positionRepository;

    @Transactional
    public boolean executeSignal(Strategy strategy, TradeSignal signal, MarketTickEvent tick) {
        log.info("Executing {} signal for {} on strategy {}", signal.getSignalType(), tick.getSymbol(), strategy.getName());

        try {
            // Find instrument
            String targetSymbol = signal.getTargetSymbol() != null ? signal.getTargetSymbol() : tick.getSymbol();
            String baseSymbol = targetSymbol.replace("-EQ", "");
            
            Optional<Instrument> instrumentOpt = instrumentRepository.findByTradingSymbol(targetSymbol);
            if (instrumentOpt.isEmpty()) {
                instrumentOpt = instrumentRepository.findByTradingSymbol(baseSymbol);
            }
            if (instrumentOpt.isEmpty()) {
                instrumentOpt = instrumentRepository.findByTradingSymbol(targetSymbol + "-EQ");
            }
            
            if (instrumentOpt.isEmpty()) {
                if (targetSymbol.contains("-") || targetSymbol.contains("2026")) {
                    Instrument dynamicInst = Instrument.builder()
                            .tradingSymbol(targetSymbol)
                            .symbol(targetSymbol)
                            .name(targetSymbol)
                            .instrumentType(targetSymbol.endsWith("CE") ? InstrumentType.CE : InstrumentType.PE)
                            .segment("OPT_IDX")
                            .exchange(targetSymbol.startsWith("SENSEX") ? ExchangeType.BSE : ExchangeType.NSE)
                            .tickSize(new BigDecimal("0.05"))
                            .lotSize(targetSymbol.startsWith("SENSEX") ? 20 : 65)
                            .isActive(true)
                            .build();
                    instrumentOpt = Optional.of(instrumentRepository.save(dynamicInst));
                    log.info("Created dynamic instrument for {}", targetSymbol);
                } else {
                    log.error("Could not execute signal: Instrument not found for symbol {}", targetSymbol);
                    return false;
                }
            }
            
            Instrument instrument = instrumentOpt.get();
            signal.setInstrumentId(instrument.getId());

            // Extract quantity from config
            int quantity = 1;
            
            if (strategy.isPaperTrading()) {
                // Calculate dynamic quantity
                List<com.algotradex.model.Position> positions = positionRepository.findByUserId(strategy.getUser().getId());
                double startingCapital = 100000.0;
                double totalRealized = 0.0;
                double investedMargin = 0.0;
                for (com.algotradex.model.Position pos : positions) {
                    if (pos.getTradingMode() == TradingMode.PAPER) {
                        if (pos.getStatus() == PositionStatus.CLOSED && pos.getRealizedPnl() != null) {
                            totalRealized += pos.getRealizedPnl().doubleValue();
                        } else if (pos.getStatus() == PositionStatus.OPEN && pos.getAvgEntryPrice() != null) {
                            double lotSize = 1.0;
                            String sym = pos.getInstrument().getTradingSymbol();
                            if (sym.contains("NIFTY") || sym.startsWith("^NSEI")) lotSize = 65.0;
                            else if (sym.contains("SENSEX") || sym.startsWith("^BSESN") || sym.contains("BSESN")) lotSize = 20.0;
                            investedMargin += pos.getAvgEntryPrice().doubleValue() * pos.getQuantity() * lotSize;
                        }
                    }
                }
                double availableMargin = startingCapital + totalRealized - investedMargin;
                if (availableMargin > 0) {
                    double confidence = signal.getAiConfidenceScore() != null ? signal.getAiConfidenceScore() : 100.0;
                    double allocatedAmount = availableMargin * (confidence / 100.0);
                    
                    double lotSize = 1.0;
                    String sym = instrument.getTradingSymbol();
                    if (sym.contains("NIFTY") || sym.startsWith("^NSEI")) lotSize = 65.0;
                    else if (sym.contains("SENSEX") || sym.startsWith("^BSESN") || sym.contains("BSESN")) lotSize = 20.0;
                    
                    quantity = (int) (allocatedAmount / (tick.getLastPrice().doubleValue() * lotSize));
                    if (quantity < 1) quantity = 1;
                }
            } else if (strategy.getConfig() != null && strategy.getConfig().containsKey("quantity")) {
                try {
                    quantity = ((Number) strategy.getConfig().get("quantity")).intValue();
                } catch (Exception e) {
                    log.warn("Invalid quantity in strategy config. Defaulting to 1", e);
                }
            }

            // Build Order
            Order order = Order.builder()
                    .user(strategy.getUser())
                    .strategyId(strategy.getId())
                    .instrument(instrument)
                    .side((signal.getSignalType() == SignalType.BUY || signal.getSignalType() == SignalType.BUY_CALL || signal.getSignalType() == SignalType.BUY_PUT) ? OrderSide.BUY : OrderSide.SELL)
                    .orderType(OrderType.MARKET)
                    .productType(ProductType.INTRADAY)
                    .quantity(quantity)
                    .price(tick.getLastPrice())
                    .status(OrderStatus.PENDING)
                    .tradingMode(strategy.isPaperTrading() ? TradingMode.PAPER : TradingMode.LIVE)
                    .placedAt(ZonedDateTime.now())
                    .build();

            if (strategy.isPaperTrading()) {
                // Paper Trading: Simulate Instant Fill
                order.setStatus(OrderStatus.COMPLETE);
                order.setFilledQty(quantity);
                order.setAvgFillPrice(tick.getLastPrice());
                order.setFilledAt(ZonedDateTime.now());
                
                Order savedOrder = orderRepository.save(order);
                updatePaperPosition(strategy, instrument, savedOrder, tick.getLastPrice(), signal.getAiConfidenceScore());
                log.info("Paper order completed successfully. Order ID: {}", savedOrder.getId());
                return true;
            } else {
                // Live Trading: Fetch and set BrokerAccount
                String brokerAccountIdStr = (String) strategy.getConfig().get("brokerAccountId");
                if (brokerAccountIdStr == null) {
                    log.error("Live order failed: No brokerAccountId found in strategy config");
                    return false;
                }
                
                UUID brokerAccountId = UUID.fromString(brokerAccountIdStr);
                Optional<BrokerAccount> brokerAccountOpt = brokerAccountRepository.findById(brokerAccountId);
                if (brokerAccountOpt.isEmpty()) {
                    log.error("Live order failed: Broker account {} not found", brokerAccountId);
                    return false;
                }
                
                order.setBrokerAccount(brokerAccountOpt.get());
                
                // Delegate to OrderService to place the order with Dhan/Upstox
                Order placedOrder = orderService.placeOrder(order);
                log.info("Live order placed via adapter. Order ID: {}, Status: {}, Error: {}", 
                        placedOrder.getId(), placedOrder.getStatus(), placedOrder.getErrorMessage());
                
                return placedOrder.getStatus() == OrderStatus.OPEN || 
                       placedOrder.getStatus() == OrderStatus.COMPLETE || 
                       placedOrder.getStatus() == OrderStatus.PENDING;
            }

        } catch (Exception e) {
            log.error("Error executing signal in execution engine", e);
            return false;
        }
    }

    private void updatePaperPosition(Strategy strategy, Instrument instrument, Order order, BigDecimal price, Double confidence) {
        Optional<com.algotradex.model.Position> optPos = positionRepository.findByStrategyIdAndInstrument_IdAndStatus(
                strategy.getId(), instrument.getId(), PositionStatus.OPEN);
        
        if (optPos.isPresent()) {
            com.algotradex.model.Position pos = optPos.get();
            if (pos.getSide() == order.getSide()) {
                pos.setQuantity(pos.getQuantity() + order.getQuantity());
                // Update target and SL if confidence is provided
                if (confidence != null) {
                    double targetPct = (confidence / 100.0) * 0.60;
                    double slPct = (confidence / 100.0) * 0.30;
                    if (pos.getSide() == OrderSide.BUY) {
                        pos.setTarget(price.multiply(BigDecimal.valueOf(1 + targetPct)));
                        pos.setStopLoss(price.multiply(BigDecimal.valueOf(1 - slPct)));
                    } else {
                        pos.setTarget(price.multiply(BigDecimal.valueOf(1 - targetPct)));
                        pos.setStopLoss(price.multiply(BigDecimal.valueOf(1 + slPct)));
                    }
                }
            } else {
                int newQty = pos.getQuantity() - order.getQuantity();
                if (newQty == 0) {
                    pos.setStatus(PositionStatus.CLOSED);
                    pos.setClosedAt(ZonedDateTime.now());
                    
                    BigDecimal lotSize = BigDecimal.ONE;
                    String symbol = instrument.getTradingSymbol();
                    if (symbol.contains("NIFTY") || symbol.startsWith("^NSEI")) lotSize = BigDecimal.valueOf(65);
                    else if (symbol.contains("SENSEX") || symbol.startsWith("^BSESN") || symbol.contains("BSESN")) lotSize = BigDecimal.valueOf(20);
                    
                    BigDecimal pnl;
                    if (pos.getSide() == OrderSide.BUY) {
                        pnl = price.subtract(pos.getAvgEntryPrice()).multiply(BigDecimal.valueOf(order.getQuantity())).multiply(lotSize);
                    } else {
                        pnl = pos.getAvgEntryPrice().subtract(price).multiply(BigDecimal.valueOf(order.getQuantity())).multiply(lotSize);
                    }
                    pos.setRealizedPnl(pos.getRealizedPnl() == null ? pnl : pos.getRealizedPnl().add(pnl));
                } else {
                    pos.setQuantity(Math.abs(newQty));
                    if (newQty < 0) {
                        pos.setSide(order.getSide());
                        pos.setAvgEntryPrice(price);
                        if (confidence != null) {
                            double targetPct = (confidence / 100.0) * 0.60;
                            double slPct = (confidence / 100.0) * 0.30;
                            if (pos.getSide() == OrderSide.BUY) {
                                pos.setTarget(price.multiply(BigDecimal.valueOf(1 + targetPct)));
                                pos.setStopLoss(price.multiply(BigDecimal.valueOf(1 - slPct)));
                            } else {
                                pos.setTarget(price.multiply(BigDecimal.valueOf(1 - targetPct)));
                                pos.setStopLoss(price.multiply(BigDecimal.valueOf(1 + slPct)));
                            }
                        }
                    }
                }
            }
            pos.setCurrentPrice(price);
            positionRepository.save(pos);
        } else {
            com.algotradex.model.Position pos = com.algotradex.model.Position.builder()
                    .user(strategy.getUser())
                    .strategyId(strategy.getId())
                    .instrument(instrument)
                    .tradingMode(TradingMode.PAPER)
                    .side(order.getSide())
                    .quantity(order.getQuantity())
                    .avgEntryPrice(price)
                    .currentPrice(price)
                    .productType(ProductType.INTRADAY)
                    .status(PositionStatus.OPEN)
                    .openedAt(ZonedDateTime.now())
                    .build();
            
            if (confidence != null) {
                double targetPct = (confidence / 100.0) * 0.60;
                double slPct = (confidence / 100.0) * 0.30;
                if (order.getSide() == OrderSide.BUY) {
                    pos.setTarget(price.multiply(BigDecimal.valueOf(1 + targetPct)));
                    pos.setStopLoss(price.multiply(BigDecimal.valueOf(1 - slPct)));
                } else {
                    pos.setTarget(price.multiply(BigDecimal.valueOf(1 - targetPct)));
                    pos.setStopLoss(price.multiply(BigDecimal.valueOf(1 + slPct)));
                }
            }

            positionRepository.save(pos);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 10000)
    @Transactional
    public void enforceRiskManagement() {
        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(istZone);
        int dayOfWeek = now.getDayOfWeek().getValue();
        if (dayOfWeek > 5) return;
        int timeInMinutes = now.getHour() * 60 + now.getMinute();
        if (timeInMinutes < 555 || timeInMinutes > 930) return;
        
        List<com.algotradex.model.Position> openPositions = positionRepository.findAll().stream()
                .filter(p -> p.getStatus() == PositionStatus.OPEN)
                .collect(java.util.stream.Collectors.toList());

        for (com.algotradex.model.Position pos : openPositions) {
            if (pos.getAvgEntryPrice() == null || pos.getCurrentPrice() == null) continue;
            
            BigDecimal entryPrice = pos.getAvgEntryPrice();
            BigDecimal currentPrice = pos.getCurrentPrice();
            
            boolean takeProfitHit = false;
            boolean stopLossHit = false;
            
            if (pos.getTarget() != null) {
                if (pos.getSide() == OrderSide.BUY && currentPrice.compareTo(pos.getTarget()) >= 0) {
                    takeProfitHit = true;
                } else if (pos.getSide() == OrderSide.SELL && currentPrice.compareTo(pos.getTarget()) <= 0) {
                    takeProfitHit = true;
                }
            }
            
            if (pos.getStopLoss() != null) {
                if (pos.getSide() == OrderSide.BUY && currentPrice.compareTo(pos.getStopLoss()) <= 0) {
                    stopLossHit = true;
                } else if (pos.getSide() == OrderSide.SELL && currentPrice.compareTo(pos.getStopLoss()) >= 0) {
                    stopLossHit = true;
                }
            } else {
                // Fallback to 30% drop if no SL is set
                double dropPct;
                if (pos.getSide() == OrderSide.BUY) {
                    dropPct = (entryPrice.doubleValue() - currentPrice.doubleValue()) / entryPrice.doubleValue();
                } else {
                    dropPct = (currentPrice.doubleValue() - entryPrice.doubleValue()) / entryPrice.doubleValue();
                }
                if (dropPct >= 0.30) stopLossHit = true;
            }

            if (stopLossHit || takeProfitHit) { 
                if (stopLossHit) {
                    log.warn("STOP LOSS HIT for Position ID {}. Liquidating.", pos.getId());
                } else {
                    log.info("TAKE PROFIT HIT for Position ID {}. Target reached {}. Liquidating.", pos.getId(), pos.getTarget());
                }
                
                // Create an opposite order to close it
                Order exitOrder = Order.builder()
                        .user(pos.getUser())
                        .strategyId(pos.getStrategyId())
                        .instrument(pos.getInstrument())
                        .side(pos.getSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY)
                        .orderType(OrderType.MARKET)
                        .productType(ProductType.INTRADAY)
                        .quantity(pos.getQuantity())
                        .price(currentPrice)
                        .status(OrderStatus.PENDING)
                        .tradingMode(pos.getTradingMode())
                        .placedAt(ZonedDateTime.now())
                        .build();
                
                if (pos.getTradingMode() == TradingMode.PAPER) {
                    exitOrder.setStatus(OrderStatus.COMPLETE);
                    exitOrder.setFilledQty(pos.getQuantity());
                    exitOrder.setAvgFillPrice(currentPrice);
                    exitOrder.setFilledAt(ZonedDateTime.now());
                    Order savedOrder = orderRepository.save(exitOrder);
                    
                    Strategy proxyStrategy = new Strategy();
                    proxyStrategy.setId(pos.getStrategyId());
                    proxyStrategy.setUser(pos.getUser());
                    updatePaperPosition(proxyStrategy, pos.getInstrument(), savedOrder, currentPrice, null);
                } else if (pos.getTradingMode() == TradingMode.LIVE && pos.getBrokerAccount() != null) {
                    exitOrder.setBrokerAccount(pos.getBrokerAccount());
                    try {
                        orderService.placeOrder(exitOrder);
                    } catch (Exception e) {
                        log.error("Failed to place Stop Loss order for Live Position {}", pos.getId(), e);
                    }
                }
            }
        }
    }
}
