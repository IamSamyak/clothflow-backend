package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "service_clients",
        indexes = {
                @Index(
                        name = "idx_service_clients_client_id",
                        columnList = "client_id"
                )
        }
)
public class ServiceClient {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(
            name = "client_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String clientId;

    @Column(
            name = "client_secret_hash",
            nullable = false,
            length = 255
    )
    private String clientSecretHash;

    @Column(
            name = "service_name",
            nullable = false,
            length = 100
    )
    private String serviceName;

    @Column(
            nullable = false
    )
    private boolean enabled = true;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    protected ServiceClient() {
    }

    public ServiceClient(
            String clientId,
            String clientSecretHash,
            String serviceName
    ) {
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.serviceName = serviceName;
        this.enabled = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}