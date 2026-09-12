package com.clothflow.user.repository;

import com.clothflow.user.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(
            String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from PasswordResetToken token
            where token.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Query("""
            select token
            from PasswordResetToken token
            where token.user.id = :userId
              and token.consumedAt is null
              and token.invalidatedAt is null
              and token.expiresAt > CURRENT_TIMESTAMP
            """)
    List<PasswordResetToken> findActiveByUserId(
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from PasswordResetToken token
            where token.user.id = :userId
              and token.consumedAt is null
              and token.invalidatedAt is null
              and token.expiresAt > CURRENT_TIMESTAMP
            """)
    List<PasswordResetToken> findActiveByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}