package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_token_families",
        indexes = {
                @Index(
                        name = "idx_refresh_token_families_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_refresh_token_families_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_refresh_token_families_created_at",
                        columnList = "created_at"
                )
        }
)
public class RefreshTokenFamily {

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

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private RefreshTokenFamilyStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "compromised_at")
    private OffsetDateTime compromisedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected RefreshTokenFamily() {
    }

    public RefreshTokenFamily(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User must not be null"
            );
        }

        this.user = user;
        this.status = RefreshTokenFamilyStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public RefreshTokenFamilyStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public OffsetDateTime getCompromisedAt() {
        return compromisedAt;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isActive() {
        return status == RefreshTokenFamilyStatus.ACTIVE;
    }

    public boolean isCompromised() {
        return status == RefreshTokenFamilyStatus.COMPROMISED;
    }

    public boolean isRevoked() {
        return status == RefreshTokenFamilyStatus.REVOKED;
    }

    public void revoke() {

        if (status == RefreshTokenFamilyStatus.REVOKED) {
            return;
        }

        status = RefreshTokenFamilyStatus.REVOKED;

        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now();
        }
    }

    public void compromise() {

        if (status == RefreshTokenFamilyStatus.COMPROMISED) {
            return;
        }

        status = RefreshTokenFamilyStatus.COMPROMISED;

        OffsetDateTime now = OffsetDateTime.now();

        compromisedAt = now;
        revokedAt = now;
    }
}