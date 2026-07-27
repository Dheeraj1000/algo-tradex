package com.algotradex.model;

import com.algotradex.model.enums.OrderSide;
import com.algotradex.model.enums.TradingMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "strategy_id")
    private UUID strategyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
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

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal brokerage;

    private BigDecimal taxes;

    private BigDecimal pnl;

    @Column(name = "pnl_percent")
    private BigDecimal pnlPercent;

    @Column(name = "is_entry", nullable = false)
    private boolean isEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_trade_id")
    private Trade linkedTrade;

    private String notes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] tags;

    @Column(name = "executed_at", nullable = false)
    private ZonedDateTime executedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
