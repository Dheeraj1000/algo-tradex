package com.algotradex.engine;

import com.algotradex.dto.BacktestResult;
import com.algotradex.dto.CandleDTO;
import com.algotradex.model.Strategy;
import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.SignalType;
import com.algotradex.service.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestEngine {

    private final HistoricalDataService historicalDataService;
    private final SignalGenerator signalGenerator;
    private final com.algotradex.repository.BrokerAccountRepository brokerAccountRepository;

    public BacktestResult runBacktest(Strategy strategy, ZonedDateTime start, ZonedDateTime end, String interval, BigDecimal initialCapital) {
        String symbol = (String) strategy.getConfig().get("tradingSymbol");
        if (symbol == null) {
            throw new IllegalArgumentException("Strategy does not have a trading symbol configured");
        }
        
        com.algotradex.model.BrokerAccount brokerAccount = brokerAccountRepository.findByUserIdAndDeletedAtIsNull(strategy.getUser().getId())
                .stream()
                .filter(a -> a.getStatus() == com.algotradex.model.enums.BrokerStatus.CONNECTED)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active BrokerAccount found for user to fetch Dhan data"));
        
        int quantity = 1;
        if (strategy.getConfig().containsKey("quantity")) {
            quantity = ((Number) strategy.getConfig().get("quantity")).intValue();
        }

        List<CandleDTO> candles = historicalDataService.fetchHistoricalData(symbol, start, end, interval, brokerAccount);
        if (candles.isEmpty()) {
            throw new RuntimeException("No historical data found for symbol: " + symbol);
        }

        Map<String, List<Double>> isolatedPriceHistory = new ConcurrentHashMap<>();
        
        BigDecimal currentCapital = initialCapital;
        BigDecimal maxCapital = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        
        int winningTrades = 0;
        int losingTrades = 0;
        int positionQty = 0;
        BigDecimal averageEntryPrice = BigDecimal.ZERO;
        
        List<BacktestResult.EquityPoint> equityCurve = new ArrayList<>();
        List<BacktestResult.TradeLog> tradeLogs = new ArrayList<>();
        
        for (CandleDTO candle : candles) {
            ZonedDateTime tickTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(candle.getTime()), ZoneId.systemDefault());
            MarketTickEvent mockTick = new MarketTickEvent(symbol, candle.getClose(), candle.getVolume(), tickTime);
            
            TradeSignal signal = signalGenerator.evaluateStrategy(strategy, mockTick, isolatedPriceHistory);
            
            // Mark to market equity for the curve
            BigDecimal unrealizedPnL = BigDecimal.ZERO;
            if (positionQty > 0) {
                unrealizedPnL = candle.getClose().subtract(averageEntryPrice).multiply(BigDecimal.valueOf(positionQty));
            } else if (positionQty < 0) {
                unrealizedPnL = averageEntryPrice.subtract(candle.getClose()).multiply(BigDecimal.valueOf(Math.abs(positionQty)));
            }
            
            BigDecimal currentEquity = currentCapital.add(unrealizedPnL);
            equityCurve.add(new BacktestResult.EquityPoint(candle.getTime() * 1000, currentEquity)); // JS timestamp in ms
            
            // Execute trade logic
            if ((signal.getSignalType() == SignalType.BUY || signal.getSignalType() == SignalType.BUY_CALL || signal.getSignalType() == SignalType.BUY_PUT) && positionQty <= 0) {
                // If short, cover first
                if (positionQty < 0) {
                    BigDecimal realizedPnL = averageEntryPrice.subtract(candle.getClose()).multiply(BigDecimal.valueOf(Math.abs(positionQty)));
                    currentCapital = currentCapital.add(realizedPnL);
                    if (realizedPnL.compareTo(BigDecimal.ZERO) > 0) winningTrades++;
                    else losingTrades++;
                    
                    tradeLogs.add(new BacktestResult.TradeLog("COVER", candle.getClose(), Math.abs(positionQty), candle.getTime() * 1000, realizedPnL));
                    positionQty = 0;
                }
                
                // Go long
                positionQty = quantity;
                averageEntryPrice = candle.getClose();
                tradeLogs.add(new BacktestResult.TradeLog("BUY", candle.getClose(), quantity, candle.getTime() * 1000, BigDecimal.ZERO));
                
            } else if (signal.getSignalType() == SignalType.SELL && positionQty >= 0) {
                // If long, sell first
                if (positionQty > 0) {
                    BigDecimal realizedPnL = candle.getClose().subtract(averageEntryPrice).multiply(BigDecimal.valueOf(positionQty));
                    currentCapital = currentCapital.add(realizedPnL);
                    if (realizedPnL.compareTo(BigDecimal.ZERO) > 0) winningTrades++;
                    else losingTrades++;
                    
                    tradeLogs.add(new BacktestResult.TradeLog("SELL", candle.getClose(), positionQty, candle.getTime() * 1000, realizedPnL));
                    positionQty = 0;
                }
                
                // Go short (optional, depends on strategy. For simplicity, we just sell/square off)
                // If we want short selling:
                // positionQty = -quantity;
                // averageEntryPrice = candle.getClose();
                // tradeLogs.add(new BacktestResult.TradeLog("SHORT", candle.getClose(), quantity, candle.getTime() * 1000, BigDecimal.ZERO));
            }
            
            // Calculate Drawdown
            if (currentEquity.compareTo(maxCapital) > 0) {
                maxCapital = currentEquity;
            } else {
                BigDecimal drawdown = maxCapital.subtract(currentEquity);
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }
        
        // Square off remaining position at the end of backtest
        if (positionQty != 0) {
            CandleDTO lastCandle = candles.get(candles.size() - 1);
            BigDecimal realizedPnL = BigDecimal.ZERO;
            String action = "";
            if (positionQty > 0) {
                realizedPnL = lastCandle.getClose().subtract(averageEntryPrice).multiply(BigDecimal.valueOf(positionQty));
                action = "SELL (SQUARE OFF)";
            } else {
                realizedPnL = averageEntryPrice.subtract(lastCandle.getClose()).multiply(BigDecimal.valueOf(Math.abs(positionQty)));
                action = "COVER (SQUARE OFF)";
            }
            
            currentCapital = currentCapital.add(realizedPnL);
            if (realizedPnL.compareTo(BigDecimal.ZERO) > 0) winningTrades++;
            else losingTrades++;
            
            tradeLogs.add(new BacktestResult.TradeLog(action, lastCandle.getClose(), Math.abs(positionQty), lastCandle.getTime() * 1000, realizedPnL));
            positionQty = 0;
        }

        int totalTrades = winningTrades + losingTrades;
        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100.0 : 0.0;
        BigDecimal totalPnL = currentCapital.subtract(initialCapital);

        return BacktestResult.builder()
                .strategyId(strategy.getId())
                .symbol(symbol)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .winRate(winRate)
                .totalPnL(totalPnL.setScale(2, RoundingMode.HALF_UP))
                .maxDrawdown(maxDrawdown.setScale(2, RoundingMode.HALF_UP))
                .initialCapital(initialCapital.setScale(2, RoundingMode.HALF_UP))
                .finalCapital(currentCapital.setScale(2, RoundingMode.HALF_UP))
                .equityCurve(equityCurve)
                .tradeLogs(tradeLogs)
                .build();
    }
}
