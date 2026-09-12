package com.clothflow.user.security;

import com.clothflow.user.entity.Role;
import com.clothflow.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM =
            "token_type";

    private static final String ACCESS_TOKEN_TYPE =
            "access";

    private static final String SERVICE_TOKEN_TYPE =
            "service";

    private static final String INTERNAL_AUDIENCE =
            "clothflow-internal";

    private final JwtKeyProvider keyProvider;

    private final JwtVerificationKeyLocator
            verificationKeyLocator;

    private final long accessTokenExpiration;

    private final String issuer;

    private final String audience;

    private final long clockSkewSeconds;

    public JwtService(
            JwtKeyProvider keyProvider,
            JwtVerificationKeyLocator verificationKeyLocator,
            @Value("${jwt.access-token-expiration}")
            long accessTokenExpiration,
            @Value("${jwt.issuer}")
            String issuer,
            @Value("${jwt.audience}")
            String audience,
            @Value("${jwt.clock-skew-seconds}")
            long clockSkewSeconds
    ) {

        this.keyProvider =
                keyProvider;

        this.verificationKeyLocator =
                verificationKeyLocator;

        this.accessTokenExpiration =
                accessTokenExpiration;

        this.issuer =
                issuer;

        this.audience =
                audience;

        this.clockSkewSeconds =
                clockSkewSeconds;
    }

    public String generateServiceToken(
            String serviceName,
            List<String> scopes,
            long expirationSeconds
    ) {

        Instant now =
                Instant.now();

        Instant expiration =
                now.plusSeconds(
                        expirationSeconds
                );

        return Jwts.builder()

                .header()
                .keyId(
                        keyProvider
                                .getCurrentKeyId()
                )
                .and()

                .issuer(
                        issuer
                )

                .audience()
                .add(
                        INTERNAL_AUDIENCE
                )
                .and()

                .subject(
                        serviceName
                )

                .id(
                        UUID.randomUUID()
                                .toString()
                )

                .claim(
                        "scope",
                        scopes
                )

                .claim(
                        TOKEN_TYPE_CLAIM,
                        SERVICE_TOKEN_TYPE
                )

                .issuedAt(
                        Date.from(now)
                )

                .expiration(
                        Date.from(expiration)
                )

                .signWith(
                        keyProvider
                                .getCurrentPrivateKey(),
                        Jwts.SIG.RS256
                )

                .compact();
    }

    public String generateAccessToken(
            User user
    ) {

        Instant now =
                Instant.now();

        Instant expiration =
                now.plusSeconds(
                        accessTokenExpiration
                );

        List<String> roles =
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .map(Enum::name)
                        .toList();

        return Jwts.builder()

                .header()
                .keyId(
                        keyProvider
                                .getCurrentKeyId()
                )
                .and()

                .issuer(
                        issuer
                )

                .audience()
                .add(
                        audience
                )
                .and()

                .subject(
                        user.getId()
                                .toString()
                )

                .id(
                        UUID.randomUUID()
                                .toString()
                )

                .claim(
                        "email",
                        user.getEmail()
                )

                .claim(
                        "username",
                        user.getUsername()
                )

                .claim(
                        "roles",
                        roles
                )

                .claim(
                        "security_version",
                        user.getSecurityVersion()
                )

                .claim(
                        TOKEN_TYPE_CLAIM,
                        ACCESS_TOKEN_TYPE
                )

                .issuedAt(
                        Date.from(now)
                )

                .expiration(
                        Date.from(expiration)
                )

                .signWith(
                        keyProvider
                                .getCurrentPrivateKey(),
                        Jwts.SIG.RS256
                )

                .compact();
    }

    public Claims parseAndValidate(
            String token
    ) {

        return Jwts.parser()

                /*
                 * JJWT reads the protected JWT header,
                 * extracts kid, and delegates key
                 * resolution to our locator.
                 */
                .keyLocator(
                        verificationKeyLocator
                )

                .requireIssuer(
                        issuer
                )

                .requireAudience(
                        audience
                )

                .require(
                        TOKEN_TYPE_CLAIM,
                        ACCESS_TOKEN_TYPE
                )

                .clockSkewSeconds(
                        clockSkewSeconds
                )

                .build()

                .parseSignedClaims(
                        token
                )

                .getPayload();
    }

    public UUID extractUserId(
            String token
    ) {

        Claims claims =
                parseAndValidate(
                        token
                );

        return UUID.fromString(
                claims.getSubject()
        );
    }

    public List<String> extractRoles(
            String token
    ) {

        Claims claims =
                parseAndValidate(
                        token
                );

        return claims.get(
                "roles",
                List.class
        );
    }

    public long getAccessTokenExpiration() {

        return accessTokenExpiration;
    }
}