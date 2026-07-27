package com.algotradex.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BacktestRequest {
    private String startDate; // ISO 8601 string (e.g. 2026-07-01T00:00:00Z)
    private String endDate;   // ISO 8601 string
    private String interval;  // e.g., 1m, 5m, 15m, 1h, 1d
    private BigDecimal initialCapital;
}
