package com.clothflow.user.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String SECURITY_VERSION_CLAIM =
            "security_version";

    private static final String ROLES_CLAIM =
            "roles";

    private final JwtService jwtService;
    private final UserSecurityStateService userSecurityStateService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserSecurityStateService userSecurityStateService
    ) {
        this.jwtService = jwtService;
        this.userSecurityStateService = userSecurityStateService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        /*
         * No Authorization header or non-Bearer authentication.
         *
         * We do not reject the request here.
         * Spring Security will decide later whether the endpoint
         * requires authentication.
         */
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();

        /*
         * Empty Bearer token is treated as an invalid token.
         */
        if (token.isBlank()) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {

            /*
             * JwtService performs the cryptographic and standard
             * JWT validations:
             *
             * - signature
             * - key ID
             * - issuer
             * - audience
             * - expiration
             * - clock skew
             * - token_type
             */
            Claims claims =
                    jwtService.parseAndValidate(token);

            /*
             * Subject is our User UUID.
             */
            UUID userId =
                    extractUserId(claims);

            /*
             * Extract the security version that was present when
             * this access token was issued.
             */
            long tokenSecurityVersion =
                    extractSecurityVersion(claims);

            /*
             * Compare the token's security version with the
             * CURRENT value in the database.
             *
             * Example:
             *
             * JWT = 0
             * DB  = 1
             *
             * -> token is stale
             * -> authentication is rejected
             */
            boolean securityVersionValid =
                    userSecurityStateService.isSecurityVersionValid(
                            userId,
                            tokenSecurityVersion
                    );

            if (!securityVersionValid) {
                throw new IllegalArgumentException(
                        "JWT security state is no longer valid"
                );
            }

            /*
             * Extract roles only after the JWT itself and the
             * security state have been validated.
             */
            List<String> roles =
                    extractRoles(claims);

            var authorities =
                    roles.stream()
                            .map(role ->
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                            .toList();

            /*
             * Authentication principal is the User UUID.
             *
             * We deliberately do not put the password,
             * refresh token, or other sensitive information
             * into the SecurityContext.
             */
            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (Exception ex) {

            /*
             * Never allow a partially validated JWT to remain
             * authenticated.
             *
             * Invalid JWT
             * malformed JWT
             * stale security version
             * unknown user
             * invalid roles
             * invalid UUID
             * etc.
             *
             * all result in an unauthenticated request.
             */
            SecurityContextHolder.clearContext();
        }

        /*
         * Continue the filter chain.
         *
         * If authentication failed and the endpoint requires
         * authentication, Spring Security will invoke the existing
         * AuthenticationEntryPoint and return 401.
         */
        filterChain.doFilter(request, response);
    }

    private UUID extractUserId(Claims claims) {

        String subject =
                claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT subject is missing"
            );
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "JWT subject is not a valid user ID",
                    ex
            );
        }
    }

    private long extractSecurityVersion(Claims claims) {

        Object value =
                claims.get(SECURITY_VERSION_CLAIM);

        if (value == null) {
            throw new IllegalArgumentException(
                    "JWT security_version claim is missing"
            );
        }

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                    "JWT security_version claim is invalid"
            );
        }

        long securityVersion =
                number.longValue();

        if (securityVersion < 0) {
            throw new IllegalArgumentException(
                    "JWT security_version claim cannot be negative"
            );
        }

        return securityVersion;
    }

    private List<String> extractRoles(Claims claims) {

        Object value =
                claims.get(ROLES_CLAIM);

        if (value == null) {
            throw new IllegalArgumentException(
                    "JWT roles claim is missing"
            );
        }

        if (!(value instanceof List<?> roles)) {
            throw new IllegalArgumentException(
                    "JWT roles claim is invalid"
            );
        }

        return roles.stream()
                .map(this::validateRole)
                .toList();
    }

    private String validateRole(Object role) {

        if (!(role instanceof String roleName)) {
            throw new IllegalArgumentException(
                    "JWT contains an invalid role"
            );
        }

        if (roleName.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT contains a blank role"
            );
        }

        return roleName;
    }
}