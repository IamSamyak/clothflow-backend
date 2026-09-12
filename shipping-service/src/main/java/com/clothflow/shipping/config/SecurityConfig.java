package com.clothflow.shipping.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final String issuer;
    private final String jwkSetUri;
    private final String apiAudience;
    private final String internalAudience;

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuer,

            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
            String jwkSetUri,

            @Value("${jwt.audience:clothflow-api}")
            String apiAudience,

            @Value("${jwt.internal-audience:clothflow-internal}")
            String internalAudience
    ) {
        this.issuer = issuer;
        this.jwkSetUri = jwkSetUri;
        this.apiAudience = apiAudience;
        this.internalAudience = internalAudience;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * -------------------------------------------------
                         * Public infrastructure endpoints
                         * -------------------------------------------------
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        /*
                         * -------------------------------------------------
                         * Carrier webhook
                         *
                         * Spring Security does not authenticate this using
                         * a user/service JWT.
                         *
                         * The webhook signature must be validated by the
                         * application-level webhook verifier.
                         * -------------------------------------------------
                         */
                        .requestMatchers(
                                "/api/v1/shipments/webhooks/tracking"
                        ).permitAll()

                        /*
                         * Everything else requires authentication.
                         *
                         * Fine-grained authorization is handled by
                         * @PreAuthorize on controller methods.
                         */
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .jwt(jwt ->
                                        jwt.jwtAuthenticationConverter(
                                                jwtAuthenticationConverter()
                                        )
                                )
                );

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withJwkSetUri(jwkSetUri)
                        .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                new JwtIssuerValidator(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator =
                token -> {

                    List<String> audiences =
                            token.getAudience();

                    if (audiences.contains(apiAudience)
                            || audiences.contains(internalAudience)) {

                        return OAuth2TokenValidatorResult.success();
                    }

                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(
                                    OAuth2ErrorCodes.INVALID_TOKEN,
                                    "Invalid JWT audience",
                                    null
                            )
                    );
                };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {

                    Collection<GrantedAuthority> authorities =
                            new ArrayList<>();

                    /*
                     * -------------------------------------------------
                     * User JWT roles
                     * -------------------------------------------------
                     *
                     * roles: ["CUSTOMER"]
                     *
                     * becomes:
                     *
                     * ROLE_CUSTOMER
                     */
                    List<String> roles =
                            jwt.getClaimAsStringList("roles");

                    if (roles != null) {

                        roles.forEach(role ->
                                authorities.add(
                                        new org.springframework.security.core
                                                .authority.SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                        );
                    }

                    /*
                     * -------------------------------------------------
                     * Service JWT
                     * -------------------------------------------------
                     *
                     * token_type: service
                     *
                     * becomes:
                     *
                     * TOKEN_SERVICE
                     */
                    String tokenType =
                            jwt.getClaimAsString("token_type");

                    if ("service".equals(tokenType)) {

                        authorities.add(
                                new org.springframework.security.core
                                        .authority.SimpleGrantedAuthority(
                                        "TOKEN_SERVICE"
                                )
                        );
                    }

                    /*
                     * -------------------------------------------------
                     * Service scopes
                     * -------------------------------------------------
                     *
                     * scope:
                     * [
                     *   "order.read",
                     *   "order.shipping.write"
                     * ]
                     *
                     * becomes:
                     *
                     * SCOPE_order.read
                     * SCOPE_order.shipping.write
                     */
                    List<String> scopes =
                            jwt.getClaimAsStringList("scope");

                    if (scopes != null) {

                        scopes.forEach(scope ->
                                authorities.add(
                                        new org.springframework.security.core
                                                .authority.SimpleGrantedAuthority(
                                                "SCOPE_" + scope
                                        )
                                )
                        );
                    }

                    return authorities;
                }
        );

        return converter;
    }
}