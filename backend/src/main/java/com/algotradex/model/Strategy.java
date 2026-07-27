package com.algotradex.model;

import com.algotradex.model.enums.StrategyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "strategies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private StrategyStatus status;

    @Column(name = "max_daily_loss")
    private BigDecimal maxDailyLoss;

    @Column(name = "max_exposure")
    private BigDecimal maxExposure;

    @Column(name = "ai_threshold")
    private Double aiThreshold; // 0.0 to 100.0

    @com.fasterxml.jackson.annotation.JsonProperty("isPaperTrading")
    @Column(name = "is_paper_trading", nullable = false)
    private boolean paperTrading;

    @lombok.Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private java.util.Map<String, Object> config = new java.util.HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
