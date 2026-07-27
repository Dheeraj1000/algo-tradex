package com.algotradex.engine;

import com.algotradex.dto.AiAlertDto;
import com.algotradex.model.Strategy;
import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.OrderStatus;
import com.algotradex.model.enums.SignalType;
import com.algotradex.repository.StrategyRepository;
import com.algotradex.repository.TradeSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoStrategyEvaluator {

    private final StrategyRepository strategyRepository;
    private final TradeSignalRepository tradeSignalRepository;
    private final OrderExecutionEngine executionEngine;

    @Transactional
    public void evaluateAlert(AiAlertDto alert) {
        log.info("Evaluating AI Alert for {}: {}", alert.getSymbol(), alert.getSignal());
        
        List<Strategy> activeStrategies = strategyRepository.findAll();
        
        for (Strategy strategy : activeStrategies) {
            if (!strategy.isPaperTrading()) {
                continue; // ONLY paper trading supported for AI alerts currently
            }

            Map<String, Object> config = strategy.getConfig();
            if (config == null) {
                continue;
            }
            
            String indicatorType = (String) config.get("indicatorType");
            if (!"CRYPTO_AI".equals(indicatorType) && !"AI_DRIVEN".equals(indicatorType)) {
                continue;
            }

            String stratSymbol = (String) config.get("tradingSymbol");
            if (stratSymbol != null) {
                if (stratSymbol.contains("-EQ")) stratSymbol = stratSymbol.replace("-EQ", "");
                if (stratSymbol.equalsIgnoreCase("NIFTY 50")) stratSymbol = "NIFTY";
                if (stratSymbol.equalsIgnoreCase("SENSEX 50")) stratSymbol = "SENSEX";
            }
            if (!alert.getSymbol().equalsIgnoreCase(stratSymbol)) {
                continue;
            }

            if (alert.getConfidence() >= strategy.getAiThreshold()) {
                String executionSymbol = alert.getTargetOption() != null ? alert.getTargetOption() : alert.getSymbol();
                
                // Check if an open trade already exists for this symbol
                List<TradeSignal> existingTrades = tradeSignalRepository.findAll();
                boolean hasOpenTrade = existingTrades.stream()
                        .anyMatch(t -> t.getExecutionStatus() == OrderStatus.OPEN && t.getTargetSymbol().equalsIgnoreCase(executionSymbol));
                
                if (hasOpenTrade) {
                    log.info("Strategy {} already has an OPEN trade for {}. Skipping duplicate signal.", strategy.getName(), executionSymbol);
                    continue;
                }

                log.info("Strategy {} triggered on {} with confidence {}%", strategy.getName(), alert.getSymbol(), alert.getConfidence());
                
                SignalType signalType;
                if (alert.getSignal().contains("BUY_CALL")) signalType = SignalType.BUY_CALL;
                else if (alert.getSignal().contains("BUY_PUT")) signalType = SignalType.BUY_PUT;
                else if (alert.getSignal().contains("BUY")) signalType = SignalType.BUY;
                else signalType = SignalType.SELL;
                
                int leverage = 1;
                if (config.containsKey("leverage")) {
                    try {
                        leverage = Integer.parseInt(config.get("leverage").toString());
                    } catch (Exception e) {
                        log.warn("Invalid leverage in strategy config, defaulting to 1");
                    }
                }
                
                TradeSignal tradeSignal = TradeSignal.builder()
                        .strategyId(strategy.getId())
                        .targetSymbol(executionSymbol)
                        .signalType(signalType)
                        .leverage(leverage)
                        .aiConfidenceScore(alert.getConfidence())
                        .executionStatus(OrderStatus.OPEN)
                        .currentStopLoss(alert.getStopLoss())
                        .targetPrice(alert.getTargetProfit())
                        .entryPrice(alert.getRecommendedEntry() > 0 ? alert.getRecommendedEntry() : alert.getSpotPrice())
                        .build();
                
                tradeSignalRepository.save(tradeSignal);
                
                // Now actually execute the trade!
                MarketTickEvent tick = new MarketTickEvent();
                tick.setSymbol(executionSymbol);
                tick.setLastPrice(BigDecimal.valueOf(alert.getTargetLtp() > 0 ? alert.getTargetLtp() : alert.getSpotPrice()));
                tick.setTimestamp(ZonedDateTime.now());
                tick.setVolume(1);
                
                boolean success = executionEngine.executeSignal(strategy, tradeSignal, tick);
                if (success) {
                    log.info("Successfully executed PAPER trade signal for {}", executionSymbol);
                } else {
                    log.error("Failed to execute trade signal for {}", executionSymbol);
                    tradeSignal.setExecutionStatus(OrderStatus.REJECTED);
                    tradeSignalRepository.save(tradeSignal);
                }
            }
        }
    }
}
