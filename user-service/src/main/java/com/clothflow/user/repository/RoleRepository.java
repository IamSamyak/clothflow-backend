package com.clothflow.user.repository;

import com.clothflow.user.entity.Role;
import com.clothflow.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository
        extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}