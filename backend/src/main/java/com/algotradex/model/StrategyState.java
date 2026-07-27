package com.algotradex.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "strategy_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", unique = true, nullable = false)
    private Strategy strategy;

    @Column(name = "daily_realized_pnl")
    private BigDecimal dailyRealizedPnL;

    @Column(name = "daily_trade_count", nullable = false)
    private int dailyTradeCount;

    @Column(name = "last_evaluation_time")
    private ZonedDateTime lastEvaluationTime;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
