package com.clothflow.user.security;

import com.clothflow.user.config.JwtKeyProperties;
import com.clothflow.user.config.ServiceClientBootstrapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({
        JwtKeyProperties.class,
        CorsProperties.class,
        HstsProperties.class,
        ServiceClientBootstrapProperties.class
})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final RestAuthenticationEntryPoint
            authenticationEntryPoint;

    private final RestAccessDeniedHandler
            accessDeniedHandler;

    private final CorsProperties corsProperties;

    private final HstsProperties hstsProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsProperties corsProperties,
            HstsProperties hstsProperties
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.authenticationEntryPoint =
                authenticationEntryPoint;

        this.accessDeniedHandler =
                accessDeniedHandler;

        this.corsProperties =
                corsProperties;

        this.hstsProperties =
                hstsProperties;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * CORS is disabled by default.
         *
         * This is appropriate for backend-to-backend
         * communication and local development unless a
         * browser frontend explicitly needs cross-origin
         * access.
         */
        if (!corsProperties.isEnabled()) {
            return request -> null;
        }

        configuration.setAllowedOrigins(
                corsProperties.getAllowedOrigins()
        );

        configuration.setAllowedMethods(
                corsProperties.getAllowedMethods()
        );

        configuration.setAllowedHeaders(
                corsProperties.getAllowedHeaders()
        );

        configuration.setExposedHeaders(
                corsProperties.getExposedHeaders()
        );

        configuration.setAllowCredentials(
                corsProperties.isAllowCredentials()
        );

        configuration.setMaxAge(
                corsProperties.getMaxAgeSeconds()
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                /*
                 * CORS
                 *
                 * The actual policy comes from
                 * CorsConfigurationSource above.
                 */
                .cors(cors -> {})

                /*
                 * CSRF
                 *
                 * The service is a stateless Bearer-token API.
                 * Authentication credentials are not stored in
                 * browser cookies.
                 *
                 * If we introduce cookie-based authentication
                 * later, this decision must be revisited.
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * Stateless authentication.
                 *
                 * Spring Security must not create or maintain
                 * an HTTP session for authentication.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Authentication / authorization failures
                 * are returned through our REST handlers.
                 */
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                /*
                 * Security response headers.
                 */
                .headers(headers -> {

                    /*
                     * Prevent MIME-type sniffing.
                     */
                    headers.contentTypeOptions(
                            contentTypeOptions -> {}
                    );

                    /*
                     * Prevent the API from being embedded
                     * inside an iframe.
                     */
                    headers.frameOptions(
                            frameOptions ->
                                    frameOptions.deny()
                    );

                    /*
                     * Do not send the originating URL as
                     * the HTTP Referer header.
                     */
                    headers.referrerPolicy(
                            referrerPolicy ->
                                    referrerPolicy.policy(
                                            ReferrerPolicyHeaderWriter
                                                    .ReferrerPolicy
                                                    .NO_REFERRER
                                    )
                    );

                    /*
                     * HSTS is enabled only when explicitly
                     * configured.
                     *
                     * This prevents local HTTP/FLOCI
                     * development from receiving an HSTS
                     * policy accidentally.
                     */
                    if (hstsProperties.isEnabled()) {

                        headers.httpStrictTransportSecurity(
                                hsts ->
                                        hsts
                                                .includeSubDomains(
                                                        hstsProperties
                                                                .isIncludeSubdomains()
                                                )
                                                .preload(
                                                        hstsProperties
                                                                .isPreload()
                                                )
                                                .maxAgeInSeconds(
                                                        hstsProperties
                                                                .getMaxAgeSeconds()
                                                )
                        );
                    }
                })

                /*
                 * Public endpoints.
                 *
                 * Everything else requires a valid
                 * authenticated principal.
                 */
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/reset-password",

                                        /*
                                         * Public JWKS endpoint.
                                         *
                                         * Downstream services need
                                         * this to validate JWT signatures.
                                         */
                                        "/.well-known/jwks.json",

                                        /*
                                         * Only health endpoints are
                                         * intentionally public.
                                         */
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/prometheus",

                                        /*
                                         * Swagger remains enabled
                                         * for development.
                                         *
                                         * Production exposure will
                                         * be made environment-specific
                                         * during deployment hardening.
                                         */
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                /*
                 * Validate the Bearer JWT before Spring reaches
                 * UsernamePasswordAuthenticationFilter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}