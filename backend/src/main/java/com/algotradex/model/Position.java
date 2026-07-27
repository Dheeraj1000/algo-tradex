package com.algotradex.model;

import com.algotradex.model.enums.OrderSide;
import com.algotradex.model.enums.PositionStatus;
import com.algotradex.model.enums.ProductType;
import com.algotradex.model.enums.TradingMode;
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
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(name = "strategy_id")
    private UUID strategyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_account_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BrokerAccount brokerAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instrument_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trading_mode", nullable = false)
    private TradingMode tradingMode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "avg_entry_price", nullable = false)
    private BigDecimal avgEntryPrice;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "unrealized_pnl")
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl")
    private BigDecimal realizedPnl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PositionStatus status;

    @Column(name = "stop_loss")
    private BigDecimal stopLoss;

    private BigDecimal target;

    @Column(name = "trailing_sl")
    private BigDecimal trailingSl;

    @Column(name = "opened_at", nullable = false)
    private ZonedDateTime openedAt;

    @Column(name = "closed_at")
    private ZonedDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
