package com.clothflow.product.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * Product Service is a stateless REST API.
                 *
                 * Authentication is supplied through the
                 * Authorization: Bearer <JWT> header.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * JWT Bearer token authentication.
                 *
                 * Spring Security will:
                 * - extract the Bearer token
                 * - resolve the signing key using JWKS
                 * - validate the JWT signature
                 * - validate issuer
                 * - validate audience
                 * - validate expiration
                 * - create Authentication
                 */
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        new JwtAuthenticationConverter()
                                )
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                /*
                                 * Operational endpoints.
                                 */
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/prometheus",
                                        "/actuator/info",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html"
                                ).permitAll()

                                /*
                                 * API documentation.
                                 *
                                 * We will make this environment-specific
                                 * during final deployment hardening.
                                 */
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                /*
                                 * Product APIs require authentication.
                                 */
                                .anyRequest()
                                .authenticated()
                );

        return http.build();
    }
}