package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_roles_name",
                        columnNames = "name"
                )
        }
)
public class Role {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoleName name;

    protected Role() {
    }

    public Role(
            UUID id,
            RoleName name
    ) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }
}