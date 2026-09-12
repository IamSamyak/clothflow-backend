package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refresh_tokens_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_refresh_tokens_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_refresh_tokens_family_id",
                        columnList = "family_id"
                ),
                @Index(
                        name = "idx_refresh_tokens_expires_at",
                        columnList = "expires_at"
                )
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "family_id",
            nullable = false
    )
    private RefreshTokenFamily family;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(
            name = "revoked_at"
    )
    private OffsetDateTime revokedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "last_used_at"
    )
    private OffsetDateTime lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by")
    private RefreshToken replacedBy;

    protected RefreshToken() {
    }

    public RefreshToken(
            User user,
            String tokenHash,
            RefreshTokenFamily family,
            OffsetDateTime expiresAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User must not be null"
            );
        }

        if (family == null) {
            throw new IllegalArgumentException(
                    "Refresh token family must not be null"
            );
        }

        if (tokenHash == null || tokenHash.isBlank()) {
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
        this.family = family;
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

    public RefreshTokenFamily getFamily() {
        return family;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public boolean isExpired() {
        return OffsetDateTime.now()
                .isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable() {
        return !isExpired() && !isRevoked();
    }

    public boolean wasReplaced() {
        return replacedBy != null;
    }

    public void markUsed() {
        this.lastUsedAt = OffsetDateTime.now();
    }

    public void revoke() {
        if (!isRevoked()) {
            this.revokedAt = OffsetDateTime.now();
        }
    }

    public RefreshToken getReplacedBy() {
        return replacedBy;
    }

    public void markReplacedBy(RefreshToken replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException(
                    "Replacement token must not be null"
            );
        }

        this.replacedBy = replacement;
        this.revoke();
    }
}