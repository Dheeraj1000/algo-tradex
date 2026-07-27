package com.algotradex.model;

import com.algotradex.model.enums.*;
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
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

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
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(nullable = false)
    private int quantity;

    private BigDecimal price;

    @Column(name = "trigger_price")
    private BigDecimal triggerPrice;

    @Column(name = "disclosed_qty")
    private Integer disclosedQty;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "broker_order_id")
    private String brokerOrderId;

    @Column(name = "filled_qty", nullable = false)
    private int filledQty;

    @Column(name = "avg_fill_price")
    private BigDecimal avgFillPrice;

    private String tag;

    private String reason;

    @Column(name = "error_message")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    private Order parentOrder;

    @Column(name = "placed_at")
    private ZonedDateTime placedAt;

    @Column(name = "filled_at")
    private ZonedDateTime filledAt;

    @Column(name = "cancelled_at")
    private ZonedDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
