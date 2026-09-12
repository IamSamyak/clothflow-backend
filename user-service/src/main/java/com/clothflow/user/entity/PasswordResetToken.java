package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "password_reset_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_password_reset_tokens_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_password_reset_tokens_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_password_reset_tokens_expires_at",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_password_reset_tokens_consumed_at",
                        columnList = "consumed_at"
                )
        }
)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "token_hash",
            nullable = false,
            length = 255
    )
    private String tokenHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "invalidated_at")
    private OffsetDateTime invalidatedAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(
            User user,
            String tokenHash,
            OffsetDateTime expiresAt
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User must not be null"
            );
        }

        if (tokenHash == null ||
                tokenHash.isBlank()) {

            throw new IllegalArgumentException(
                    "Token hash must not be blank"
            );
        }

        if (expiresAt == null) {

            throw new IllegalArgumentException(
                    "Expiration time must not be null"
            );
        }

        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {

        return !OffsetDateTime.now()
                .isBefore(expiresAt);
    }

    public boolean isConsumed() {

        return consumedAt != null;
    }

    public void consume() {

        if (isConsumed()) {

            throw new IllegalStateException(
                    "Password reset token has already been consumed"
            );
        }

        this.consumedAt =
                OffsetDateTime.now();
    }

    public OffsetDateTime getInvalidatedAt() {
        return invalidatedAt;
    }

    public boolean isInvalidated() {
        return invalidatedAt != null;
    }

    public boolean isUsable() {
        return !isExpired()
                && !isConsumed()
                && !isInvalidated();
    }

    public void invalidate() {
        if (!isInvalidated() && !isConsumed()) {
            this.invalidatedAt = OffsetDateTime.now();
        }
    }

}