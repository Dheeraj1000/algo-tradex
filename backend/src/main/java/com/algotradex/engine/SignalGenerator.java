package com.algotradex.engine;

import com.algotradex.model.Strategy;
import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.SignalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.algotradex.client.AiServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalGenerator {

    private final AiServiceClient aiServiceClient;
    private final com.algotradex.repository.BrokerAccountRepository brokerAccountRepository;
    private final Random random = new Random();
    
    // Cache to hold last 100 prices for each symbol
    private final Map<String, List<Double>> priceHistory = new ConcurrentHashMap<>();

    public TradeSignal evaluateStrategy(Strategy strategy, MarketTickEvent tick) {
        return evaluateStrategy(strategy, tick, this.priceHistory);
    }

    public TradeSignal evaluateStrategy(Strategy strategy, MarketTickEvent tick, Map<String, List<Double>> historyMap) {
        TradeSignal defaultSignal = TradeSignal.builder().signalType(SignalType.NO_TRADE).strategyId(strategy.getId()).build();
        
        if (strategy.getConfig() == null) {
            return defaultSignal;
        }

        String strategySymbol = (String) strategy.getConfig().get("tradingSymbol");
        if (strategySymbol == null) {
            return defaultSignal;
        }

        // Normalize symbols (e.g. RELIANCE vs RELIANCE-EQ)
        String tickSymbolBase = tick.getSymbol().replace("-EQ", "");
        String stratSymbolBase = strategySymbol.replace("-EQ", "");

        if (!tickSymbolBase.equalsIgnoreCase(stratSymbolBase)) {
            return defaultSignal;
        }

        // Append price to history
        List<Double> prices = historyMap.computeIfAbsent(stratSymbolBase, k -> new CopyOnWriteArrayList<>());
        prices.add(tick.getLastPrice().doubleValue());
        if (prices.size() > 100) {
            prices.remove(0);
        }

        String indicatorType = (String) strategy.getConfig().get("indicatorType");
        if (indicatorType == null) {
            return defaultSignal;
        }

        try {
            if ("SMA_CROSSOVER".equalsIgnoreCase(indicatorType)) {
                int shortPeriod = ((Number) strategy.getConfig().getOrDefault("shortPeriod", 9)).intValue();
                int longPeriod = ((Number) strategy.getConfig().getOrDefault("longPeriod", 21)).intValue();

                if (prices.size() < longPeriod + 1) {
                    return defaultSignal;
                }

                double currentShort = calculateSMA(prices, shortPeriod);
                double currentLong = calculateSMA(prices, longPeriod);

                List<Double> prevPrices = prices.subList(0, prices.size() - 1);
                double prevShort = calculateSMA(prevPrices, shortPeriod);
                double prevLong = calculateSMA(prevPrices, longPeriod);

                if (prevShort <= prevLong && currentShort > currentLong) {
                    log.info("SMA Golden Cross (BUY) for {}! Short SMA: {}, Long SMA: {}", stratSymbolBase, currentShort, currentLong);
                    return TradeSignal.builder().signalType(SignalType.BUY).strategyId(strategy.getId()).build();
                } else if (prevShort >= prevLong && currentShort < currentLong) {
                    log.info("SMA Death Cross (SELL) for {}! Short SMA: {}, Long SMA: {}", stratSymbolBase, currentShort, currentLong);
                    return TradeSignal.builder().signalType(SignalType.SELL).strategyId(strategy.getId()).build();
                }
            } else if ("RSI_REVERSAL".equalsIgnoreCase(indicatorType)) {
                int period = ((Number) strategy.getConfig().getOrDefault("rsiPeriod", 14)).intValue();
                double oversold = ((Number) strategy.getConfig().getOrDefault("rsiOversold", 30)).doubleValue();
                double overbought = ((Number) strategy.getConfig().getOrDefault("rsiOverbought", 70)).doubleValue();

                if (prices.size() < period + 2) {
                    return defaultSignal;
                }

                double currentRSI = calculateRSI(prices, period);
                List<Double> prevPrices = prices.subList(0, prices.size() - 1);
                double prevRSI = calculateRSI(prevPrices, period);

                if (prevRSI >= oversold && currentRSI < oversold) {
                    log.info("RSI Oversold BUY signal for {}! RSI crossed below {}: {}", stratSymbolBase, oversold, currentRSI);
                    return TradeSignal.builder().signalType(SignalType.BUY).strategyId(strategy.getId()).build();
                } else if (prevRSI <= overbought && currentRSI > overbought) {
                    log.info("RSI Overbought SELL signal for {}! RSI crossed above {}: {}", stratSymbolBase, overbought, currentRSI);
                    return TradeSignal.builder().signalType(SignalType.SELL).strategyId(strategy.getId()).build();
                }
            } else if ("AI_DRIVEN".equalsIgnoreCase(indicatorType)) {
                // Fetch AI prediction
                String clientId = null;
                String accessToken = null;
                java.util.List<com.algotradex.model.BrokerAccount> accounts = brokerAccountRepository.findByUserIdAndDeletedAtIsNull(strategy.getUser().getId());
                for (com.algotradex.model.BrokerAccount account : accounts) {
                    if (com.algotradex.model.enums.BrokerType.DHAN.equals(account.getBrokerType())) {
                        clientId = account.getClientId();
                        accessToken = account.getAccessToken();
                        break;
                    }
                }
                
                java.util.Optional<com.algotradex.client.AiServiceClient.PredictionResponse> optPred = aiServiceClient.getPrediction(stratSymbolBase, clientId, accessToken);
                if (optPred.isPresent()) {
                    com.algotradex.client.AiServiceClient.PredictionResponse pred = optPred.get();
                    if (pred.getConfidence_score() != null) {
                        double confidence = pred.getConfidence_score();
                        String signalStr = pred.getSignal();
                        if ("BUY_CALL".equals(signalStr)) {
                            log.info("AI Signal (BUY_CALL) for {} targeting {}", stratSymbolBase, pred.getTarget_symbol());
                            return TradeSignal.builder().signalType(SignalType.BUY_CALL).strategyId(strategy.getId()).targetSymbol(pred.getTarget_symbol()).aiConfidenceScore(confidence).build();
                        } else if ("BUY_PUT".equals(signalStr)) {
                            log.info("AI Signal (BUY_PUT) for {} targeting {}", stratSymbolBase, pred.getTarget_symbol());
                            return TradeSignal.builder().signalType(SignalType.BUY_PUT).strategyId(strategy.getId()).targetSymbol(pred.getTarget_symbol()).aiConfidenceScore(confidence).build();
                        } else if ("SELL".equals(signalStr)) {
                            return TradeSignal.builder().signalType(SignalType.SELL).strategyId(strategy.getId()).aiConfidenceScore(confidence).build();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error generating signal for strategy {}", strategy.getId(), e);
        }

        return defaultSignal;
    }

    private double calculateSMA(List<Double> prices, int period) {
        if (prices.size() < period) return 0.0;
        double sum = 0.0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    private double calculateRSI(List<Double> prices, int period) {
        if (prices.size() < period + 1) return 50.0;
        
        double gains = 0.0;
        double losses = 0.0;

        for (int i = prices.size() - period; i < prices.size(); i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            if (diff > 0) {
                gains += diff;
            } else {
                losses -= diff;
            }
        }

        if (losses == 0.0) return 100.0;
        double rs = gains / losses;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
