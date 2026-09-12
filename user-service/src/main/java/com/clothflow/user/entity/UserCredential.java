package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_credentials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_credentials_user",
                        columnNames = "user_id"
                )
        }
)
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    @Column(
            name = "password_changed_at",
            nullable = false
    )
    private OffsetDateTime passwordChangedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    protected UserCredential() {
    }

    public UserCredential(
            User user,
            String passwordHash
    ) {
        this.user = user;
        this.passwordHash = passwordHash;

        OffsetDateTime now = OffsetDateTime.now();

        this.passwordChangedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void changePassword(String newPasswordHash) {

        if (newPasswordHash == null ||
                newPasswordHash.isBlank()) {

            throw new IllegalArgumentException(
                    "Password hash must not be blank"
            );
        }

        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}