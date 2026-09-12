package com.clothflow.user.repository;

import com.clothflow.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Modifying
    @Query("""
        DELETE FROM OutboxEvent event
        WHERE event.eventType =
              'PASSWORD_RESET_REQUESTED'
          AND event.status =
              'PUBLISHED'
          AND event.publishedAt < :cutoff
        """)
    int deletePublishedPasswordResetEventsBefore(
            @Param("cutoff")
            OffsetDateTime cutoff
    );

    @Query("""
        select u.securityVersion
        from User u
        where u.id = :userId
        """)
    Optional<Long> findSecurityVersionById(
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from User u
        where u.id = :userId
        """)
    Optional<User> findByIdForUpdate(
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from User u
        where u.email = :email
        """)
    Optional<User> findByEmailForUpdate(
            @Param("email") String email
    );
}