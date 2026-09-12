package com.clothflow.user.repository;

import com.clothflow.user.entity.LoginAttemptState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoginAttemptStateRepository
        extends JpaRepository<LoginAttemptState, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select state
            from LoginAttemptState state
            where state.userId = :userId
            """)
    Optional<LoginAttemptState> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}