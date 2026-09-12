package com.clothflow.user.service;

import com.clothflow.user.dto.request.ForgotPasswordRequest;
import com.clothflow.user.dto.request.ResetPasswordRequest;
import com.clothflow.user.dto.response.ForgotPasswordResponse;
import com.clothflow.user.entity.PasswordResetToken;
import com.clothflow.user.entity.SecurityAuditEventType;
import com.clothflow.user.entity.SecurityAuditOutcome;
import com.clothflow.user.entity.User;
import com.clothflow.user.entity.UserCredential;
import com.clothflow.user.exception.AuthenticationFailedException;
import com.clothflow.user.exception.TooManyRequestsException;
import com.clothflow.user.repository.PasswordResetTokenRepository;
import com.clothflow.user.repository.UserCredentialRepository;
import com.clothflow.user.repository.UserRepository;
import com.clothflow.user.security.*;
import com.clothflow.user.security.PasswordResetRateLimitService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PasswordResetService {

    private static final String GENERIC_MESSAGE =
            "If the account exists, a password reset link has been sent.";

    private static final String INVALID_TOKEN_MESSAGE =
            "Invalid or expired password reset token";

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository
            passwordResetTokenRepository;

    private final PasswordResetTokenGenerator tokenGenerator;

    private final TokenHashService tokenHashService;

    private final UserCredentialRepository credentialRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserSecurityService userSecurityService;

    private final PasswordResetRateLimitService
            passwordResetRateLimitService;

    private final OutboxEventService outboxEventService;

    private final SecurityAuditPublisher securityAuditPublisher;

    private final PasswordResetAttemptRateLimitService
            passwordResetAttemptRateLimitService;

    private final long tokenExpiration;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            UserCredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            UserSecurityService userSecurityService,
            PasswordResetRateLimitService passwordResetRateLimitService,
            OutboxEventService outboxEventService,
            SecurityAuditPublisher securityAuditPublisher, PasswordResetAttemptRateLimitService passwordResetAttemptRateLimitService,
            @Value("${password-reset.token-expiration}")
            long tokenExpiration
    ) {
        this.userRepository =
                userRepository;

        this.passwordResetTokenRepository =
                passwordResetTokenRepository;

        this.tokenGenerator =
                tokenGenerator;

        this.tokenHashService =
                tokenHashService;

        this.credentialRepository =
                credentialRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.userSecurityService =
                userSecurityService;

        this.passwordResetRateLimitService =
                passwordResetRateLimitService;

        this.outboxEventService =
                outboxEventService;

        this.securityAuditPublisher =
                securityAuditPublisher;
        this.passwordResetAttemptRateLimitService = passwordResetAttemptRateLimitService;

        this.tokenExpiration =
                tokenExpiration;
    }

    /**
     * Creates a password reset request.
     *
     * Security properties:
     *
     * - Does not reveal whether the account exists.
     * - Applies IP/email rate limiting.
     * - Locks the user before modifying active reset tokens.
     * - Invalidates previous active reset tokens.
     * - Stores only the token hash.
     * - Sends the raw token only through the outbox event.
     * - Token and outbox event are created in the same transaction.
     */
    @Transactional
    public ForgotPasswordResponse requestPasswordReset(
            ForgotPasswordRequest request,
            String clientIp
    ) {

        String email =
                normalizeEmail(
                        request.email()
                );

        PasswordResetRateLimitDecision decision =
                passwordResetRateLimitService.check(
                        clientIp,
                        email
                );

        if (decision ==
                PasswordResetRateLimitDecision.IP_BLOCKED) {

            throw new TooManyRequestsException(
                    "Too many password reset requests"
            );
        }

        if (decision ==
                PasswordResetRateLimitDecision.EMAIL_BLOCKED) {

            /*
             * Never reveal whether this email belongs
             * to an existing account.
             */
            return new ForgotPasswordResponse(
                    GENERIC_MESSAGE
            );
        }

        /*
         * Redis rate limiting currently fails open.
         *
         * PostgreSQL remains the source of truth.
         */
        User user =
                userRepository.findByEmail(email)
                        .orElse(null);

        /*
         * Unknown email gets exactly the same response
         * as an existing account.
         */
        if (user == null) {

            return new ForgotPasswordResponse(
                    GENERIC_MESSAGE
            );
        }

        /*
         * Lock the User row before modifying reset-token
         * state.
         *
         * This serializes concurrent reset requests for
         * the same account.
         */
        User lockedUser =
                userRepository.findByIdForUpdate(
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "User not found"
                                )
                        );

        /*
         * Re-check account state after acquiring the lock.
         *
         * The initially loaded User could be stale.
         */
        if (!lockedUser.canAuthenticate()) {

            return new ForgotPasswordResponse(
                    GENERIC_MESSAGE
            );
        }

        /*
         * Lock all currently active reset tokens.
         *
         * User row is already locked, so reset requests
         * for this user are serialized.
         */
        List<PasswordResetToken> activeTokens =
                passwordResetTokenRepository
                        .findActiveByUserIdForUpdate(
                                lockedUser.getId()
                        );

        /*
         * A new reset request supersedes previous
         * active reset tokens.
         */
        activeTokens.forEach(
                PasswordResetToken::invalidate
        );

        /*
         * Generate the raw token only in memory.
         *
         * NEVER persist this value.
         */
        String rawToken =
                tokenGenerator.generate();

        String tokenHash =
                tokenHashService.hash(
                        rawToken
                );

        OffsetDateTime expiresAt =
                OffsetDateTime.now()
                        .plusSeconds(
                                tokenExpiration
                        );

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        lockedUser,
                        tokenHash,
                        expiresAt
                );

        passwordResetTokenRepository.save(
                resetToken
        );

        /*
         * The raw token exists only for constructing
         * the asynchronous notification event.
         *
         * The password_reset_tokens table contains
         * only the hash.
         */
        outboxEventService.createPasswordResetRequestedEvent(
                lockedUser.getId(),
                lockedUser.getEmail(),
                rawToken
        );

        return new ForgotPasswordResponse(
                GENERIC_MESSAGE
        );
    }

    /**
     * Consumes a password reset token and changes
     * the user's password.
     *
     * Transactional security boundary:
     *
     * 1. Lock reset token
     * 2. Validate token
     * 3. Lock User
     * 4. Lock Credential
     * 5. Change password
     * 6. Consume reset token
     * 7. Increment security version
     * 8. Revoke refresh-token families
     * 9. Commit
     * 10. Audit after commit
     */
    @Transactional
    public void resetPassword(
            ResetPasswordRequest request,
            LoginContext context
    ) {

        PasswordResetAttemptRateLimitDecision rateLimitDecision =
                passwordResetAttemptRateLimitService.check(
                        context.clientIp(),
                        request.token()
                );

        if (rateLimitDecision ==
                PasswordResetAttemptRateLimitDecision.IP_BLOCKED) {

            throw new TooManyRequestsException(
                    "Too many password reset attempts"
            );
        }

        if (rateLimitDecision ==
                PasswordResetAttemptRateLimitDecision.TOKEN_BLOCKED) {

            throw new AuthenticationFailedException(
                    "Invalid or expired password reset token"
            );
        }

        String tokenHash =
                tokenHashService.hash(
                        request.token()
                );

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Invalid or expired password reset token"
                                )
                        );

        /*
         * Token row is already pessimistically locked.
         *
         * Therefore concurrent attempts using the same
         * reset token are serialized.
         */
        if (!resetToken.isUsable()) {

            throw new AuthenticationFailedException(
                    "Invalid or expired password reset token"
            );
        }

        /*
         * Lock the User BEFORE Credential.
         *
         * This establishes the security lock ordering:
         *
         * User
         *   ↓
         * Credential
         *   ↓
         * Refresh-token families
         */
        User user =
                userRepository.findByIdForUpdate(
                                resetToken.getUser().getId()
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Invalid or expired password reset token"
                                )
                        );

        /*
         * Always validate the current locked state.
         */
        if (!user.canAuthenticate()) {

            throw new AuthenticationFailedException(
                    "Invalid or expired password reset token"
            );
        }

        UserCredential credential =
                credentialRepository
                        .findByUserIdForUpdate(
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Invalid or expired password reset token"
                                )
                        );

        /*
         * Don't allow a reset to simply reuse the
         * existing password.
         */
        if (passwordEncoder.matches(
                request.newPassword(),
                credential.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "New password must be different from the current password"
            );
        }

        String newPasswordHash =
                passwordEncoder.encode(
                        request.newPassword()
                );

        credential.changePassword(
                newPasswordHash
        );

        /*
         * Mark the reset capability as consumed.
         */
        resetToken.consume();

        /*
         * Invalidate every existing authentication session.
         *
         * This:
         *
         * 1. increments securityVersion
         * 2. revokes all refresh-token families
         */
        userSecurityService.invalidateAllSessions(
                user
        );

        /*
         * Audit only AFTER the transaction commits.
         */
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        user.getId(),
                        SecurityAuditEventType.PASSWORD_RESET,
                        SecurityAuditOutcome.SUCCESS,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"method\":\"password_reset\"}"
                )
        );
    }

    private String normalizeEmail(
            String email
    ) {

        return email
                .trim()
                .toLowerCase();
    }
}