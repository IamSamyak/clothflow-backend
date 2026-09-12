package com.clothflow.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "security_audit_events",
        indexes = {
                @Index(
                        name = "idx_security_audit_events_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_security_audit_events_event_type",
                        columnList = "event_type"
                ),
                @Index(
                        name = "idx_security_audit_events_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_security_audit_events_user_created",
                        columnList = "user_id, created_at"
                )
        }
)
public class SecurityAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            length = 50
    )
    private SecurityAuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "outcome",
            nullable = false,
            length = 20
    )
    private SecurityAuditOutcome outcome;

    @Column(
            name = "ip_address",
            length = 45
    )
    private String ipAddress;

    @Column(
            name = "user_agent",
            length = 1000
    )
    private String userAgent;

    @Column(
            name = "correlation_id",
            length = 100
    )
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "details",
            columnDefinition = "jsonb"
    )
    private String details;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    protected SecurityAuditEvent() {
    }

    public SecurityAuditEvent(
            UUID userId,
            SecurityAuditEventType eventType,
            SecurityAuditOutcome outcome,
            String ipAddress,
            String userAgent,
            String correlationId,
            String details
    ) {
        this.userId = userId;
        this.eventType = eventType;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.correlationId = correlationId;
        this.details = details;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public SecurityAuditEventType getEventType() {
        return eventType;
    }

    public SecurityAuditOutcome getOutcome() {
        return outcome;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetails() {
        return details;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}