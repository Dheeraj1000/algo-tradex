package com.algotradex.controller;

import com.algotradex.model.TradeSignal;
import com.algotradex.model.enums.OrderStatus;
import com.algotradex.model.enums.SignalType;
import com.algotradex.repository.TradeSignalRepository;
import com.algotradex.service.HistoricalDataService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.algotradex.model.Strategy;
import com.algotradex.model.User;
import com.algotradex.repository.StrategyRepository;
import com.algotradex.repository.UserRepository;
import java.util.Map;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class CryptoPortfolioController {

    private final TradeSignalRepository tradeSignalRepository;
    private final HistoricalDataService historicalDataService;
    private final StrategyRepository strategyRepository;
    private final UserRepository userRepository;

    private static final double STARTING_BALANCE = 100000.0 / 96.14; // exactly 1 Lakh INR


    @Data
    @Builder
    public static class ActiveTradeDto {
        private String id;
        private String symbol;
        private String type; // BUY / SELL
        private Double entryPrice;
        private Double currentPrice;
        private Double unrealizedPnl;
        private Double unrealizedPnlPercent;
        private Double stopLoss;
        private Double targetPrice;
        private Double confidence;
        private Double quantity;
        private Double investedAmount;
        private Integer leverage;
    }

    @Data
    @Builder
    public static class ClosedTradeDto {
        private String id;
        private String symbol;
        private String type;
        private Double entryPrice;
        private Double exitPrice;
        private Double realizedPnl;
        private Double realizedPnlPercent;
        private Double quantity;
        private Double investedAmount;
        private String exitReason;
        private String timestamp;
        private Integer leverage;
    }

    @Data
    @Builder
    public static class CryptoStatsDto {
        private Double accountBalance;
        private Double realizedPnl;
        private Double unrealizedPnl;
        private List<ActiveTradeDto> activeTrades;
        private List<ClosedTradeDto> closedTrades;
    }

    @GetMapping("/crypto-stats")
    public ResponseEntity<CryptoStatsDto> getCryptoStats() {
        List<TradeSignal> allTrades = tradeSignalRepository.findAll();
        
        // Sort chronologically for accurate compounding
        allTrades.sort((t1, t2) -> {
            if (t1.getTimestamp() == null || t2.getTimestamp() == null) return 0;
            return t1.getTimestamp().compareTo(t2.getTimestamp());
        });
        
        double walletBalance = STARTING_BALANCE;
        double realizedPnl = 0.0;
        double unrealizedPnl = 0.0;
        List<ActiveTradeDto> activeTrades = new ArrayList<>();
        List<ClosedTradeDto> closedTrades = new ArrayList<>();

        for (TradeSignal trade : allTrades) {
            // Filter only crypto trades
            if (trade.getTargetSymbol() == null || !trade.getTargetSymbol().endsWith("USDT")) {
                continue;
            }

            double entry = trade.getEntryPrice() != null ? trade.getEntryPrice() : 0.0;
            if (entry == 0.0) continue;

            // Determine investment size based on what was available at the time of trade
            // For simplicity in paper trading, we assume we invest 100% of available balance per trade
            double invested = walletBalance; 
            if (invested <= 0) continue; // Out of money!
            
            int leverage = trade.getLeverage() != null ? trade.getLeverage() : 1;
            double quantity = (invested * leverage) / entry;

            if (trade.getExecutionStatus() == OrderStatus.COMPLETE) {
                // Calculate Realized PnL
                double exit = trade.getExitPrice() != null ? trade.getExitPrice() : entry;
                
                double pnl = (exit - entry) * quantity;
                double pnlPercent = ((exit - entry) / entry) * 100;
                if (trade.getSignalType() == SignalType.SELL) {
                    pnl = (entry - exit) * quantity;
                    pnlPercent = ((entry - exit) / entry) * 100;
                }
                realizedPnl += pnl;
                walletBalance += pnl; // Compound the balance
                
                closedTrades.add(ClosedTradeDto.builder()
                        .id(trade.getId().toString())
                        .symbol(trade.getTargetSymbol())
                        .type(trade.getSignalType().name())
                        .entryPrice(entry)
                        .exitPrice(exit)
                        .realizedPnl(pnl)
                        .realizedPnlPercent(pnlPercent)
                        .quantity(quantity)
                        .investedAmount(invested)
                        .leverage(leverage)
                        .exitReason(trade.getExitReason() != null ? trade.getExitReason() : "Target / Stop Loss Hit")
                        .timestamp(trade.getTimestamp() != null ? trade.getTimestamp().toString() : "")
                        .build());
                
            } else if (trade.getExecutionStatus() == OrderStatus.OPEN) {
                // The money is locked in the trade
                walletBalance -= invested;
                
                // Calculate Unrealized PnL
                BigDecimal livePriceBd = historicalDataService.getBinanceLtp(trade.getTargetSymbol());
                double livePrice = livePriceBd != null ? livePriceBd.doubleValue() : entry;
                
                double pnl = (livePrice - entry) * quantity;
                double pnlPercent = ((livePrice - entry) / entry) * 100;
                if (trade.getSignalType() == SignalType.SELL) {
                    pnl = (entry - livePrice) * quantity;
                    pnlPercent = ((entry - livePrice) / entry) * 100;
                }
                unrealizedPnl += pnl;
                
                activeTrades.add(ActiveTradeDto.builder()
                        .id(trade.getId().toString())
                        .symbol(trade.getTargetSymbol())
                        .type(trade.getSignalType().name())
                        .entryPrice(entry)
                        .currentPrice(livePrice)
                        .unrealizedPnl(pnl)
                        .unrealizedPnlPercent(pnlPercent)
                        .stopLoss(trade.getCurrentStopLoss())
                        .targetPrice(trade.getTargetPrice())
                        .confidence(trade.getAiConfidenceScore())
                        .quantity(quantity)
                        .investedAmount(invested)
                        .leverage(leverage)
                        .build());
            }
        }

        return ResponseEntity.ok(CryptoStatsDto.builder()
                .accountBalance(walletBalance)
                .realizedPnl(realizedPnl)
                .unrealizedPnl(unrealizedPnl)
                .activeTrades(activeTrades)
                .closedTrades(closedTrades)
                .build());
    }

    @Data
    public static class CryptoSettingsRequest {
        private Integer leverage;
    }

    @PostMapping("/crypto-settings")
    public ResponseEntity<Void> updateCryptoSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CryptoSettingsRequest request) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        List<Strategy> strategies = strategyRepository.findAll();
        for (Strategy strategy : strategies) {
            if (strategy.getUser().getId().equals(user.getId()) && strategy.isPaperTrading()) {
                Map<String, Object> config = strategy.getConfig();
                if (config != null && "CRYPTO_AI".equals(config.get("indicatorType"))) {
                    config.put("leverage", request.getLeverage());
                    strategyRepository.save(strategy);
                }
            }
        }
        return ResponseEntity.ok().build();
    }
}
