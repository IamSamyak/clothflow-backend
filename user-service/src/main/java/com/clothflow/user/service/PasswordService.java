package com.clothflow.user.service;

import com.clothflow.user.dto.request.ChangePasswordRequest;
import com.clothflow.user.entity.User;
import com.clothflow.user.entity.UserCredential;
import com.clothflow.user.entity.SecurityAuditEventType;
import com.clothflow.user.entity.SecurityAuditOutcome;
import com.clothflow.user.exception.AuthenticationFailedException;
import com.clothflow.user.repository.UserCredentialRepository;
import com.clothflow.user.repository.UserRepository;
import com.clothflow.user.security.LoginContext;
import com.clothflow.user.security.SecurityAuditCommand;
import com.clothflow.user.security.SecurityAuditPublisher;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSecurityService userSecurityService;
    private final SecurityAuditPublisher securityAuditPublisher;

    public PasswordService(
            UserRepository userRepository,
            UserCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            UserSecurityService userSecurityService,
            SecurityAuditPublisher securityAuditPublisher
    ) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSecurityService = userSecurityService;
        this.securityAuditPublisher = securityAuditPublisher;
    }

    @Transactional
    public void changePassword(
            UUID userId,
            ChangePasswordRequest request,
            LoginContext context
    ) {

        User user =
                userRepository.findByIdForUpdate(userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "User not found"
                                )
                        );

        /*
         * Lock the credential row.
         *
         * This serializes concurrent password changes
         * for the same user.
         */
        UserCredential credential =
                credentialRepository.findByUserIdForUpdate(
                                userId
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Credentials not found"
                                )
                        );

        /*
         * Verify the CURRENT password.
         */
        boolean currentPasswordMatches =
                passwordEncoder.matches(
                        request.currentPassword(),
                        credential.getPasswordHash()
                );

        if (!currentPasswordMatches) {
            throw new AuthenticationFailedException(
                    "Current password is incorrect"
            );
        }

        /*
         * Prevent password reuse.
         */
        if (passwordEncoder.matches(
                request.newPassword(),
                credential.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password"
            );
        }

        /*
         * Hash the new password.
         */
        String newPasswordHash =
                passwordEncoder.encode(
                        request.newPassword()
                );

        credential.changePassword(
                newPasswordHash
        );

        /*
         * Invalidate all existing authentication state.
         *
         * This increments securityVersion and revokes
         * every refresh-token family.
         */
        userSecurityService.invalidateAllSessions(
                user
        );

        /*
         * Register the audit event for AFTER the
         * password-change transaction commits.
         *
         * No password, password hash, access token,
         * or refresh token is included.
         */
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        userId,
                        SecurityAuditEventType.PASSWORD_CHANGED,
                        SecurityAuditOutcome.SUCCESS,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"method\":\"password\"}"
                )
        );
    }
}