package com.algotradex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResult {
    private UUID strategyId;
    private String symbol;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private double winRate;
    private BigDecimal totalPnL;
    private BigDecimal maxDrawdown;
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    
    // Time -> Equity mapping for charting
    private List<EquityPoint> equityCurve;
    
    // Executed trades log
    private List<TradeLog> tradeLogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquityPoint {
        private long timestamp;
        private BigDecimal equity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradeLog {
        private String type; // BUY or SELL
        private BigDecimal price;
        private int quantity;
        private long timestamp;
        private BigDecimal realizedPnL;
    }
}
