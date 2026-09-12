package com.clothflow.user.service;

import com.clothflow.user.entity.LoginAttemptState;
import com.clothflow.user.entity.User;
import com.clothflow.user.entity.UserStatus;
import com.clothflow.user.repository.LoginAttemptStateRepository;
import com.clothflow.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final LoginAttemptStateRepository
            loginAttemptStateRepository;

    private final UserRepository userRepository;

    private final AccountSecurityService accountSecurityService;

    public LoginAttemptService(
            LoginAttemptStateRepository loginAttemptStateRepository,
            UserRepository userRepository, AccountSecurityService accountSecurityService
    ) {
        this.loginAttemptStateRepository =
                loginAttemptStateRepository;

        this.userRepository = userRepository;
        this.accountSecurityService = accountSecurityService;
    }

    @Transactional
    public void recordFailure(
            User user
    ) {

        LoginAttemptState state =
                getOrCreateStateForUpdate(
                        user.getId()
                );

        OffsetDateTime now =
                OffsetDateTime.now();

        state.recordFailure(now);

        if (state.getFailedAttempts()
                >= MAX_FAILED_ATTEMPTS) {

            accountSecurityService.lockAccountIfActive(
                    user
            );
        }
    }

    @Transactional
    public void reset(UUID userId) {

        LoginAttemptState state =
                getOrCreateStateForUpdate(userId);

        state.reset();
    }

    @Transactional(readOnly = true)
    public boolean isLocked(UUID userId) {

        return loginAttemptStateRepository
                .findById(userId)
                .map(state ->
                        state.isTemporarilyLocked(
                                OffsetDateTime.now()
                        )
                )
                .orElse(false);
    }

    private LoginAttemptState getOrCreateStateForUpdate(
            UUID userId
    ) {

        return loginAttemptStateRepository
                .findByUserIdForUpdate(userId)
                .orElseGet(() ->
                        loginAttemptStateRepository.save(
                                new LoginAttemptState(userId)
                        )
                );
    }
}