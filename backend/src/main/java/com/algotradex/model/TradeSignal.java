package com.algotradex.model;

import com.algotradex.model.enums.SignalType;
import com.algotradex.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "strategy_id", nullable = false)
    private UUID strategyId;

    @Column(name = "instrument_id")
    private UUID instrumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    @Column(name = "target_symbol")
    private String targetSymbol;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "execution_status", nullable = false)
    private OrderStatus executionStatus; // Using OrderStatus as it has PENDING, REJECTED, EXECUTED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // AI Trade Management Engine Fields
    @Column(name = "recovery_probability")
    private Double recoveryProbability;

    @Column(name = "exit_reason")
    private String exitReason;

    @Column(name = "ai_explanation")
    private String aiExplanation;

    @Column(name = "current_stop_loss")
    private Double currentStopLoss;

    @Column(name = "leverage", nullable = false)
    @lombok.Builder.Default
    private Integer leverage = 1;

    @Column(name = "target_price")
    private Double targetPrice;

    @Column(name = "max_profit_reached")
    private Double maxProfitReached;

    @Column(name = "entry_price")
    private Double entryPrice;

    @Column(name = "exit_price")
    private Double exitPrice;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private ZonedDateTime timestamp;
}
