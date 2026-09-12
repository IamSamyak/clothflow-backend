package com.clothflow.user.controller;

import com.clothflow.user.dto.request.ChangePasswordRequest;
import com.clothflow.user.dto.request.ForgotPasswordRequest;
import com.clothflow.user.dto.request.LoginRequest;
import com.clothflow.user.dto.request.RefreshTokenRequest;
import com.clothflow.user.dto.request.RegisterRequest;
import com.clothflow.user.dto.request.ResetPasswordRequest;
import com.clothflow.user.dto.response.AuthenticatedUserResponse;
import com.clothflow.user.dto.response.ForgotPasswordResponse;
import com.clothflow.user.dto.response.LoginResponse;
import com.clothflow.user.dto.response.UserResponse;
import com.clothflow.user.security.ClientIpResolver;
import com.clothflow.user.security.LoginContext;
import com.clothflow.user.service.AuthenticationService;
import com.clothflow.user.service.PasswordResetService;
import com.clothflow.user.service.PasswordService;
import com.clothflow.user.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import com.clothflow.user.dto.request.ServiceTokenRequest;
import com.clothflow.user.dto.response.ServiceTokenResponse;
import com.clothflow.user.service.ServiceTokenService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRegistrationService registrationService;

    private final AuthenticationService authenticationService;

    private final PasswordService passwordService;

    private final ClientIpResolver clientIpResolver;

    private final PasswordResetService passwordResetService;

    private final ServiceTokenService serviceTokenService;

    public AuthController(
            UserRegistrationService registrationService,
            AuthenticationService authenticationService,
            PasswordService passwordService,
            ClientIpResolver clientIpResolver,
            PasswordResetService passwordResetService, ServiceTokenService serviceTokenService
    ) {
        this.registrationService =
                registrationService;

        this.authenticationService =
                authenticationService;

        this.passwordService =
                passwordService;

        this.clientIpResolver =
                clientIpResolver;

        this.passwordResetService =
                passwordResetService;
        this.serviceTokenService = serviceTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse response =
                registrationService.register(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String clientIp =
                clientIpResolver.resolve(
                        httpRequest
                );

        String userAgent =
                httpRequest.getHeader(
                        "User-Agent"
                );

        LoginContext loginContext =
                new LoginContext(
                        clientIp,
                        userAgent,
                        null
                );

        return ResponseEntity.ok(
                authenticationService.login(
                        request,
                        loginContext
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(
            Authentication authentication
    ) {

        UUID userId =
                (UUID) authentication.getPrincipal();

        List<String> roles =
                authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        return ResponseEntity.ok(
                new AuthenticatedUserResponse(
                        userId,
                        roles
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp =
                clientIpResolver.resolve(
                        httpRequest
                );

        String userAgent =
                httpRequest.getHeader(
                        "User-Agent"
                );

        LoginContext loginContext =
                new LoginContext(
                        clientIp,
                        userAgent,
                        null
                );

        LoginResponse response =
                authenticationService.refresh(
                        request,
                        loginContext
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp =
                clientIpResolver.resolve(
                        httpRequest
                );

        String userAgent =
                httpRequest.getHeader(
                        "User-Agent"
                );

        LoginContext loginContext =
                new LoginContext(
                        clientIp,
                        userAgent,
                        null
                );

        authenticationService.logout(
                request.refreshToken(),
                loginContext
        );
    }

    @PatchMapping("/users/{userId}/password")
    @PreAuthorize(
            "@userAuthorizationService.isSelf(authentication, #userId)"
    )
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {

        String clientIp =
                clientIpResolver.resolve(
                        httpRequest
                );

        String userAgent =
                httpRequest.getHeader(
                        "User-Agent"
                );

        LoginContext loginContext =
                new LoginContext(
                        clientIp,
                        userAgent,
                        null
                );

        passwordService.changePassword(
                userId,
                request,
                loginContext
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse>
    forgotPassword(
            @Valid
            @RequestBody
            ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {

        ForgotPasswordResponse response =
                passwordResetService.requestPasswordReset(
                        request,
                        clientIpResolver.resolve(
                                httpRequest
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp =
                clientIpResolver.resolve(httpRequest);

        String userAgent =
                httpRequest.getHeader("User-Agent");

        LoginContext loginContext =
                new LoginContext(
                        clientIp,
                        userAgent,
                        null
                );

        passwordResetService.resetPassword(
                request,
                loginContext
        );
    }

    @PostMapping("/service-token")
    public ResponseEntity<ServiceTokenResponse> serviceToken(
            @Valid @RequestBody ServiceTokenRequest request
    ) {

        return ResponseEntity.ok(
                serviceTokenService.issueToken(
                        request
                )
        );
    }
}