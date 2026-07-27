package com.algotradex.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_management_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeManagementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trade_signal_id", nullable = false)
    private UUID tradeSignalId;

    @Column(name = "action_taken", nullable = false)
    private String actionTaken; // HOLD, EXIT, PARTIAL_EXIT, MOVE_STOP_LOSS

    @Column(name = "recovery_probability")
    private Double recoveryProbability;

    @Column(name = "explanation", length = 1000)
    private String explanation;

    @Column(name = "spot_price")
    private Double spotPrice;

    @Column(name = "option_price")
    private Double optionPrice;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private ZonedDateTime timestamp;
}
