package com.algotradex.model;

import com.algotradex.model.enums.BrokerStatus;
import com.algotradex.model.enums.BrokerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "broker_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "api_key_enc", columnDefinition = "TEXT")
    private String apiKeyEnc;

    @Column(name = "api_secret_enc", columnDefinition = "TEXT")
    private String apiSecretEnc;

    @Column(name = "pin_enc", columnDefinition = "TEXT")
    private String pinEnc;

    @Column(name = "totp_secret_enc", columnDefinition = "TEXT")
    private String totpSecretEnc;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expiry")
    private ZonedDateTime tokenExpiry;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private BrokerStatus status;

    @Column(name = "last_connected")
    private ZonedDateTime lastConnected;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;
}
