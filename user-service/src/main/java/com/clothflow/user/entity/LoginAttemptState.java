package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "login_attempts"
)
public class LoginAttemptState {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "first_failed_at")
    private OffsetDateTime firstFailedAt;

    @Column(name = "last_failed_at")
    private OffsetDateTime lastFailedAt;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Version
    @Column(nullable = false)
    private Long version;

    protected LoginAttemptState() {
    }

    public LoginAttemptState(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID must not be null"
            );
        }

        this.userId = userId;
        this.failedAttempts = 0;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public OffsetDateTime getFirstFailedAt() {
        return firstFailedAt;
    }

    public OffsetDateTime getLastFailedAt() {
        return lastFailedAt;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public Long getVersion() {
        return version;
    }

    public void recordFailure(
            OffsetDateTime now
    ) {

        if (firstFailedAt == null) {
            firstFailedAt = now;
        }

        failedAttempts++;
        lastFailedAt = now;
    }

    public void reset() {

        failedAttempts = 0;
        firstFailedAt = null;
        lastFailedAt = null;
        lockedUntil = null;
    }

    public void lockUntil(
            OffsetDateTime lockedUntil
    ) {

        this.lockedUntil = lockedUntil;
    }

    public boolean isTemporarilyLocked(
            OffsetDateTime now
    ) {

        return lockedUntil != null
                && now.isBefore(lockedUntil);
    }
}