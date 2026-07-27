package com.algotradex.model;

import com.algotradex.model.enums.ExchangeType;
import com.algotradex.model.enums.InstrumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "trading_symbol", nullable = false)
    private String tradingSymbol;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ExchangeType exchange;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "instrument_type", nullable = false)
    private InstrumentType instrumentType;

    private String segment;

    @Column(name = "lot_size", nullable = false)
    private int lotSize;

    @Column(name = "tick_size", nullable = false)
    private BigDecimal tickSize;

    private LocalDate expiry;

    private BigDecimal strike;

    @Column(name = "option_type")
    private String optionType;

    private String isin;

    private String token;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
