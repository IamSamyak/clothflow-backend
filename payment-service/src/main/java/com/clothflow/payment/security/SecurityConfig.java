package com.clothflow.payment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final String issuer;
    private final String jwkSetUri;

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
            String issuer,

            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
            String jwkSetUri
    ) {
        this.issuer = issuer;
        this.jwkSetUri = jwkSetUri;
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withJwkSetUri(jwkSetUri)
                        .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(
                        issuer
                );

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtAudienceValidator();

        OAuth2TokenValidator<Jwt> tokenTypeValidator =
                new JwtTokenTypeValidator();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator,
                        tokenTypeValidator
                )
        );

        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.disable()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/prometheus",
                                        "/actuator/info",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html"
                                ).permitAll()

                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                new ClothFlowJwtGrantedAuthoritiesConverter()
        );

        return converter;
    }
}