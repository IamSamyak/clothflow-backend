package com.clothflow.user.service;

import com.clothflow.user.dto.request.LoginRequest;
import com.clothflow.user.dto.request.RefreshTokenRequest;
import com.clothflow.user.dto.response.LoginResponse;
import com.clothflow.user.entity.*;
import com.clothflow.user.exception.AuthenticationFailedException;
import com.clothflow.user.exception.TooManyRequestsException;
import com.clothflow.user.repository.RefreshTokenFamilyRepository;
import com.clothflow.user.repository.RefreshTokenRepository;
import com.clothflow.user.repository.UserCredentialRepository;
import com.clothflow.user.repository.UserRepository;
import com.clothflow.user.security.JwtService;
import com.clothflow.user.security.LoginContext;
import com.clothflow.user.security.RefreshTokenGenerator;
import com.clothflow.user.security.SecurityAuditCommand;
import com.clothflow.user.security.SecurityAuditPublisher;
import com.clothflow.user.security.TokenHashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;

    private final UserCredentialRepository credentialRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final RefreshTokenFamilyRepository
            refreshTokenFamilyRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final RefreshTokenGenerator refreshTokenGenerator;

    private final TokenHashService tokenHashService;

    private final LoginAttemptService loginAttemptService;

    private final LoginRateLimitService loginRateLimitService;

    private final long refreshTokenExpiration;

    private final SecurityAuditPublisher securityAuditPublisher;

    public AuthenticationService(
            UserRepository userRepository,
            UserCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenFamilyRepository refreshTokenFamilyRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenGenerator refreshTokenGenerator,
            TokenHashService tokenHashService,
            LoginAttemptService loginAttemptService,
            LoginRateLimitService loginRateLimitService,
            @Value("${jwt.refresh-token-expiration}")
            long refreshTokenExpiration,
            SecurityAuditPublisher securityAuditPublisher
    ) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenFamilyRepository =
                refreshTokenFamilyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.tokenHashService = tokenHashService;
        this.loginAttemptService = loginAttemptService;
        this.loginRateLimitService = loginRateLimitService;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.securityAuditPublisher = securityAuditPublisher;
    }

    private void auditLoginFailure(
            UUID userId,
            LoginContext context,
            String reason
    ) {
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        userId,
                        SecurityAuditEventType.LOGIN_FAILURE,
                        SecurityAuditOutcome.FAILURE,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"reason\":\"" + reason + "\"}"
                )
        );
    }

    private void auditLoginSuccess(
            UUID userId,
            LoginContext context
    ) {
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        userId,
                        SecurityAuditEventType.LOGIN_SUCCESS,
                        SecurityAuditOutcome.SUCCESS,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"method\":\"password\"}"
                )
        );
    }

    @Transactional(
            noRollbackFor = AuthenticationFailedException.class
    )
    public LoginResponse login(
            LoginRequest request,
            LoginContext loginContext
    ) {

        String email =
                normalizeEmail(
                        request.email()
                );

        RateLimitDecision rateLimitDecision =
                loginRateLimitService.check(
                        loginContext.clientIp(),
                        email
                );

        if (rateLimitDecision == RateLimitDecision.IP_BLOCKED) {

            auditLoginFailure(
                    null,
                    loginContext,
                    "ip_rate_limit_exceeded"
            );

            throw new TooManyRequestsException(
                    "Too many requests"
            );
        }

        if (rateLimitDecision == RateLimitDecision.EMAIL_BLOCKED) {

            auditLoginFailure(
                    null,
                    loginContext,
                    "email_rate_limit_exceeded"
            );

            throw new AuthenticationFailedException(
                    "Invalid email or password"
            );
        }

        /*
         * IMPORTANT:
         *
         * Lock the User row before making the final
         * authentication/account-state decision.
         *
         * This prevents concurrent account-security
         * mutations from changing the account state while
         * authentication is being evaluated.
         */
        User user =
                userRepository.findByEmailForUpdate(email)
                        .orElse(null);

        if (user == null) {

            auditLoginFailure(
                    null,
                    loginContext,
                    "invalid_credentials"
            );

            throw new AuthenticationFailedException(
                    "Invalid email or password"
            );
        }

        /*
         * Account state is checked while the User row
         * is pessimistically locked.
         */
        if (!user.canAuthenticate()) {

            auditLoginFailure(
                    user.getId(),
                    loginContext,
                    "account_unavailable"
            );

            throw new AuthenticationFailedException(
                    "Invalid email or password"
            );
        }

        /*
         * Credential is only read during login.
         *
         * The User row is already locked, so there is no
         * need to pessimistically lock the credential row.
         */
        var credential =
                credentialRepository.findByUserId(
                                user.getId()
                        )
                        .orElse(null);

        if (credential == null) {

            auditLoginFailure(
                    user.getId(),
                    loginContext,
                    "credentials_unavailable"
            );

            throw new AuthenticationFailedException(
                    "Invalid email or password"
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        credential.getPasswordHash()
                );

        if (!passwordMatches) {

            /*
             * The User entity is already locked by this
             * transaction.
             *
             * LoginAttemptService therefore operates on
             * the same locked User instance.
             */
            loginAttemptService.recordFailure(
                    user
            );

            auditLoginFailure(
                    user.getId(),
                    loginContext,
                    "invalid_credentials"
            );

            throw new AuthenticationFailedException(
                    "Invalid email or password"
            );
        }

        /*
         * Successful authentication resets the failed
         * login-attempt state.
         */
        loginAttemptService.reset(
                user.getId()
        );

        String accessToken =
                jwtService.generateAccessToken(user);

        String rawRefreshToken =
                refreshTokenGenerator.generate();

        String refreshTokenHash =
                tokenHashService.hash(
                        rawRefreshToken
                );

        OffsetDateTime expiresAt =
                OffsetDateTime.now()
                        .plusSeconds(
                                refreshTokenExpiration
                        );

        RefreshTokenFamily family =
                refreshTokenFamilyRepository.save(
                        new RefreshTokenFamily(user)
                );

        RefreshToken refreshToken =
                new RefreshToken(
                        user,
                        refreshTokenHash,
                        family,
                        expiresAt
                );

        refreshTokenRepository.save(
                refreshToken
        );

        /*
         * Audit is published only after the authentication
         * transaction successfully commits.
         */
        auditLoginSuccess(
                user.getId(),
                loginContext
        );

        return new LoginResponse(
                user.getId(),
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpiration()
        );
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @Transactional(
            noRollbackFor = AuthenticationFailedException.class
    )
    public LoginResponse refresh(
            RefreshTokenRequest request,
            LoginContext context
    ) {

        String tokenHash =
                tokenHashService.hash(
                        request.refreshToken()
                );

        /*
         * Lock the exact refresh-token row.
         *
         * Concurrent refresh requests using the same token
         * must serialize here.
         */
        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Invalid refresh token"
                                )
                        );

        /*
         * Lock the token family as well.
         *
         * The family is the security boundary for reuse
         * detection and mass revocation.
         */
        RefreshTokenFamily family =
                refreshTokenFamilyRepository
                        .findByIdForUpdate(
                                refreshToken
                                        .getFamily()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        "Invalid refresh token"
                                )
                        );

        /*
         * If the entire family is inactive, no token in
         * that family can be used anymore.
         */
        if (!family.isActive()) {

            auditRefreshFailure(
                    refreshToken.getUser().getId(),
                    context,
                    "refresh_family_inactive"
            );

            throw new AuthenticationFailedException(
                    "Refresh token family is no longer active"
            );
        }

        /*
         * A revoked token that is presented again indicates
         * refresh-token reuse.
         *
         * This is different from a normal expired token.
         */
        if (refreshToken.isRevoked()) {

            UUID userId =
                    refreshToken.getUser().getId();

            /*
             * Compromise the complete token family.
             */
            family.compromise();

            /*
             * Revoke every remaining active token belonging
             * to this family.
             */
            refreshTokenRepository.revokeFamily(
                    family.getId()
            );

            auditRefreshReuseDetected(
                    userId,
                    context,
                    family.getId()
            );

            throw new AuthenticationFailedException(
                    "Refresh token reuse detected"
            );
        }

        /*
         * Expiration is a normal refresh failure, not
         * token reuse.
         */
        if (refreshToken.isExpired()) {

            refreshToken.revoke();

            auditRefreshFailure(
                    refreshToken.getUser().getId(),
                    context,
                    "refresh_token_expired"
            );

            throw new AuthenticationFailedException(
                    "Refresh token has expired"
            );
        }

        User user =
                refreshToken.getUser();

        /*
         * Account status must still be checked during
         * refresh because an account may have been disabled
         * after the original login.
         */
        if (!user.canAuthenticate()) {

            refreshToken.revoke();

            auditRefreshFailure(
                    user.getId(),
                    context,
                    "account_unavailable"
            );

            throw new AuthenticationFailedException(
                    "Account is not available for authentication"
            );
        }

        /*
         * Generate a new short-lived access token.
         */
        String newAccessToken =
                jwtService.generateAccessToken(user);

        /*
         * Generate a completely new opaque refresh token.
         */
        String newRawRefreshToken =
                refreshTokenGenerator.generate();

        String newRefreshTokenHash =
                tokenHashService.hash(
                        newRawRefreshToken
                );

        OffsetDateTime newExpiresAt =
                OffsetDateTime.now()
                        .plusSeconds(
                                refreshTokenExpiration
                        );

        /*
         * New refresh token remains in the same family.
         */
        RefreshToken newRefreshToken =
                new RefreshToken(
                        user,
                        newRefreshTokenHash,
                        family,
                        newExpiresAt
                );

        /*
         * Flush first so the replacement token receives
         * its database identity before the old token points
         * to it.
         */
        newRefreshToken =
                refreshTokenRepository.saveAndFlush(
                        newRefreshToken
                );

        /*
         * Rotation:
         *
         * Old token -> replaced by -> New token
         */
        refreshToken.markReplacedBy(
                newRefreshToken
        );

        refreshToken.markUsed();

        /*
         * Audit only after the refresh transaction commits.
         */
        auditRefreshSuccess(
                user.getId(),
                context,
                family.getId()
        );

        return new LoginResponse(
                user.getId(),
                newAccessToken,
                newRawRefreshToken,
                jwtService.getAccessTokenExpiration()
        );
    }

    private void auditRefreshSuccess(
            UUID userId,
            LoginContext context,
            UUID familyId
    ) {
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        userId,
                        SecurityAuditEventType.REFRESH_SUCCESS,
                        SecurityAuditOutcome.SUCCESS,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"family_id\":\"" + familyId + "\"}"
                )
        );
    }

    private void auditRefreshFailure(
            UUID userId,
            LoginContext context,
            String reason
    ) {
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        userId,
                        SecurityAuditEventType.REFRESH_FAILURE,
                        SecurityAuditOutcome.FAILURE,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"reason\":\"" + reason + "\"}"
                )
        );
    }

    private void auditRefreshReuseDetected(
            UUID userId,
            LoginContext context,
            UUID familyId
    ) {
        securityAuditPublisher.publishAfterCommit(
                new SecurityAuditCommand(
                        userId,
                        SecurityAuditEventType.REFRESH_REUSE_DETECTED,
                        SecurityAuditOutcome.FAILURE,
                        context.clientIp(),
                        context.userAgent(),
                        context.correlationId(),
                        "{\"family_id\":\"" + familyId + "\"}"
                )
        );
    }

    @Transactional(
            noRollbackFor = AuthenticationFailedException.class
    )
    public void logout(
            String rawRefreshToken,
            LoginContext context
    ) {

        String tokenHash =
                tokenHashService.hash(
                        rawRefreshToken
                );

        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(refreshToken -> {

                    UUID userId =
                            refreshToken.getUser().getId();

                    RefreshTokenFamily family =
                            refreshTokenFamilyRepository
                                    .findByIdForUpdate(
                                            refreshToken
                                                    .getFamily()
                                                    .getId()
                                    )
                                    .orElse(null);

                    /*
                     * Logout revokes the entire refresh-token
                     * family rather than only the presented
                     * token.
                     */
                    if (family != null) {
                        family.revoke();
                    }

                    refreshToken.revoke();

                    /*
                     * Audit only after the logout transaction
                     * commits successfully.
                     */
                    securityAuditPublisher.publishAfterCommit(
                            new SecurityAuditCommand(
                                    userId,
                                    SecurityAuditEventType.LOGOUT,
                                    SecurityAuditOutcome.SUCCESS,
                                    context.clientIp(),
                                    context.userAgent(),
                                    context.correlationId(),
                                    "{\"method\":\"refresh_token\"}"
                            )
                    );
                });
    }
}