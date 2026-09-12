package com.clothflow.user.controller;

import com.clothflow.user.security.JwksResponse;
import com.clothflow.user.security.JwksService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class JwksController {

    private final JwksService jwksService;

    public JwksController(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksResponse> jwks() {

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl.maxAge(
                                5,
                                TimeUnit.MINUTES
                        ).cachePublic()
                )
                .body(jwksService.getJwks());
    }
}