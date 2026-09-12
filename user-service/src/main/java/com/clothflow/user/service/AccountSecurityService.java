package com.clothflow.user.service;

import com.clothflow.user.entity.SecurityAuditEventType;
import com.clothflow.user.entity.SecurityAuditOutcome;
import com.clothflow.user.entity.User;
import com.clothflow.user.repository.UserRepository;
import com.clothflow.user.security.SecurityAuditCommand;
import com.clothflow.user.security.SecurityAuditPublisher;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountSecurityService {

    private final UserRepository userRepository;

    private final UserSecurityService userSecurityService;

    private final SecurityAuditPublisher securityAuditPublisher;

    public AccountSecurityService(
            UserRepository userRepository,
            UserSecurityService userSecurityService,
            SecurityAuditPublisher securityAuditPublisher
    ) {
        this.userRepository =
                userRepository;

        this.userSecurityService =
                userSecurityService;

        this.securityAuditPublisher =
                securityAuditPublisher;
    }

    @Transactional
    public void lockAccountIfActive(
            UUID userId
    ) {

        User user =
                userRepository.findByIdForUpdate(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "User not found"
                                )
                        );

        /*
         * Idempotent operation.
         *
         * If another request already locked the account,
         * there is nothing left to do.
         */
        if (!user.canAuthenticate()) {
            return;
        }

        user.lock();

        /*
         * Locking the account is also a security-state
         * mutation.
         *
         * Existing access tokens become invalid because
         * securityVersion changes.
         *
         * Existing refresh sessions become invalid because
         * all refresh-token families are revoked.
         */
        userSecurityService.invalidateAllSessions(
                user
        );

        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        user.getId(),
                        SecurityAuditEventType.ACCOUNT_LOCKED,
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "{\"reason\":\"failed_login_attempts\"}"
                )
        );
    }

    @Transactional
    public void lockAccountIfActive(
            User user
    ) {

        if (!user.canAuthenticate()) {
            return;
        }

        user.lock();

        userSecurityService.invalidateAllSessions(
                user
        );

        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        user.getId(),
                        SecurityAuditEventType.ACCOUNT_LOCKED,
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "{\"reason\":\"failed_login_attempts\"}"
                )
        );
    }

    @Transactional
    public void disableAccount(
            UUID userId
    ) {

        User user =
                userRepository.findByIdForUpdate(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "User not found"
                                )
                        );

        if (user.getStatus().name().equals("DISABLED")) {
            return;
        }

        user.disable();

        /*
         * Disable immediately invalidates all authentication
         * sessions.
         */
        userSecurityService.invalidateAllSessions(
                user
        );

        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        user.getId(),
                        SecurityAuditEventType.ACCOUNT_DISABLED,
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "{\"reason\":\"account_disabled\"}"
                )
        );
    }

    @Transactional
    public void activateAccount(
            UUID userId
    ) {

        User user =
                userRepository.findByIdForUpdate(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "User not found"
                                )
                        );

        user.activate();

        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        user.getId(),
                        SecurityAuditEventType.ACCOUNT_ACTIVATED,
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        null,
                        null,
                        "{\"reason\":\"account_activated\"}"
                )
        );
    }
}