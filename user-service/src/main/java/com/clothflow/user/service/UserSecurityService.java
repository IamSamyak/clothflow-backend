package com.clothflow.user.service;

import com.clothflow.user.entity.User;
import com.clothflow.user.repository.RefreshTokenFamilyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSecurityService {

    private final RefreshTokenFamilyRepository
            refreshTokenFamilyRepository;

    public UserSecurityService(
            RefreshTokenFamilyRepository refreshTokenFamilyRepository
    ) {
        this.refreshTokenFamilyRepository =
                refreshTokenFamilyRepository;
    }

    @Transactional
    public void invalidateAllSessions(User user) {

        /*
         * SECURITY INVARIANT:
         *
         * The caller must already own the pessimistic
         * User lock.
         *
         * We intentionally do not query the User again.
         *
         * Incrementing securityVersion invalidates existing
         * access JWTs.
         *
         * Revoking all refresh-token families prevents the
         * user from obtaining new access tokens through
         * previously issued refresh tokens.
         */
        user.incrementSecurityVersion();

        refreshTokenFamilyRepository
                .revokeAllByUserId(
                        user.getId()
                );
    }
}