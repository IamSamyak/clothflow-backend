package com.clothflow.user.repository;

import com.clothflow.user.entity.ServiceClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceClientRepository
        extends JpaRepository<ServiceClient, UUID> {

    Optional<ServiceClient> findByClientId(
            String clientId
    );
}