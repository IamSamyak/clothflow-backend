package com.clothflow.user.repository;

import com.clothflow.user.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(UUID userId);

    List<RefreshToken> findByFamilyId(UUID familyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rt
            from RefreshToken rt
            where rt.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = CURRENT_TIMESTAMP
        where rt.family.id = :familyId
          and rt.revokedAt is null
        """)
    int revokeFamily(
            @Param("familyId") UUID familyId
    );
}