package com.clothflow.user.security;

import java.security.PrivateKey;
import java.security.PublicKey;

public record JwtKey(
        String keyId,
        PrivateKey privateKey,
        PublicKey publicKey
) {

    public JwtKey {

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Key ID must not be blank"
            );
        }

        if (publicKey == null) {
            throw new IllegalArgumentException(
                    "Public key must not be null"
            );
        }
    }

    public boolean canSign() {
        return privateKey != null;
    }
}