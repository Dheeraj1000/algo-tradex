package com.algotradex.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketTickEvent {
    private String symbol;
    private BigDecimal lastPrice;
    private long volume;
    private ZonedDateTime timestamp;
}
