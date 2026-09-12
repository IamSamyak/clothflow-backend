package com.clothflow.user.security;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class JwtKeyRotationService {

    private final RotatingRsaKeyProvider keyProvider;

    public JwtKeyRotationService(
            RotatingRsaKeyProvider keyProvider
    ) {
        this.keyProvider = keyProvider;
    }

    public String getCurrentSigningKeyId() {

        return keyProvider.getCurrentKeyId();
    }

    public void rotateTo(
            String keyId
    ) {

        keyProvider.setCurrentSigningKey(
                keyId
        );
    }

    public void retire(
            String keyId
    ) {

        keyProvider.removeKey(
                keyId
        );
    }

    public Map<String, java.security.PublicKey>
    getPublishedVerificationKeys() {

        return keyProvider.getPublicKeys();
    }
}