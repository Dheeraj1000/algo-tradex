package com.algotradex.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "position_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trade_signal_id", nullable = false, unique = true)
    private UUID tradeSignalId;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "partial_exits_count")
    private Integer partialExitsCount;

    @Column(name = "reentry_count")
    private Integer reentryCount;

    @Column(name = "is_in_recovery", nullable = false)
    private boolean isInRecovery;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
