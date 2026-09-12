package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uk_users_username",
                        columnNames = "username"
                )
        },
        indexes = {
                @Index(
                        name = "idx_users_status",
                        columnList = "status"
                )
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            nullable = false,
            length = 320
    )
    private String email;

    @Column(
            nullable = false,
            length = 100
    )
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private UserStatus status;

    @Version
    @Column(
            nullable = false
    )
    private Long version;

    @Column(
            name = "security_version",
            nullable = false
    )
    private Long securityVersion;

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

    @OneToOne(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private UserCredential credential;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(
                    name = "user_id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id"
            )
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY
    )
    private List<RefreshToken> refreshTokens =
            new ArrayList<>();


    protected User() {
    }

    public User(
            String email,
            String username
    ) {
        this.email = email;
        this.username = username;
        this.status = UserStatus.ACTIVE;

        this.securityVersion = 0L;

        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UserCredential getCredential() {
        return credential;
    }

    public Long getSecurityVersion() {
        return securityVersion;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public List<RefreshToken> getRefreshTokens() {
        return Collections.unmodifiableList(
                refreshTokens
        );
    }

    public void incrementSecurityVersion() {
        this.securityVersion++;
    }

    public void setCredential(
            UserCredential credential
    ) {
        if (credential == null) {
            throw new IllegalArgumentException(
                    "Credential must not be null"
            );
        }

        this.credential = credential;
    }
    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }


    public void activate() {

        if (status != UserStatus.ACTIVE) {
            status = UserStatus.ACTIVE;
            incrementSecurityVersion();
        }
    }

    public void lock() {

        if (status != UserStatus.LOCKED) {
            status = UserStatus.LOCKED;
            incrementSecurityVersion();
        }
    }

    public void disable() {

        if (status != UserStatus.DISABLED) {
            status = UserStatus.DISABLED;
            incrementSecurityVersion();
        }
    }

    public void addRole(
            Role role
    ) {

        if (roles.add(role)) {
            incrementSecurityVersion();
        }
    }

    public void removeRole(
            Role role
    ) {

        if (roles.remove(role)) {
            incrementSecurityVersion();
        }
    }
}