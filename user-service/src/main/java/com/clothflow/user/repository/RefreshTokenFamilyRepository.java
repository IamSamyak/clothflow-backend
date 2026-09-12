package com.clothflow.user.repository;

import com.clothflow.user.entity.RefreshTokenFamily;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenFamilyRepository
        extends JpaRepository<RefreshTokenFamily, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select family
            from RefreshTokenFamily family
            where family.id = :familyId
            """)
    Optional<RefreshTokenFamily> findByIdForUpdate(
            @Param("familyId") UUID familyId
    );

    @Modifying
    @Query("""
        update RefreshTokenFamily family
        set family.status =
                com.clothflow.user.entity.RefreshTokenFamilyStatus.REVOKED,
            family.revokedAt = CURRENT_TIMESTAMP
        where family.user.id = :userId
          and family.status =
                com.clothflow.user.entity.RefreshTokenFamilyStatus.ACTIVE
        """)
    int revokeAllByUserId(
            @Param("userId") UUID userId
    );
}