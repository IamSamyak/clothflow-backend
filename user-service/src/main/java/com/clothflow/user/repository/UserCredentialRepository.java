package com.clothflow.user.repository;

import com.clothflow.user.entity.UserCredential;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository
        extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByUserId(
            UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from UserCredential credential
            where credential.user.id = :userId
            """)
    Optional<UserCredential> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}