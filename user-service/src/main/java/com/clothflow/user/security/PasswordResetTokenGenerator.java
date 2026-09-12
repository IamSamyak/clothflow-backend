package com.clothflow.user.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordResetTokenGenerator {

    private final SecureRandom secureRandom =
            new SecureRandom();

    public String generate() {

        byte[] bytes =
                new byte[64];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}