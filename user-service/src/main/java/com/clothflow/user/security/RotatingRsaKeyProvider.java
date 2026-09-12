package com.clothflow.user.security;

import com.clothflow.user.config.JwtKeyProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Primary
public class RotatingRsaKeyProvider
        implements JwtKeyProvider {

    private final Map<String, JwtKey> keys =
            new ConcurrentHashMap<>();

    private final AtomicReference<String>
            currentSigningKeyId =
            new AtomicReference<>();

    public RotatingRsaKeyProvider(
            JwtKeySource keySource
    ) {

        if (keySource == null) {
            throw new IllegalArgumentException(
                    "JWT key source must not be null"
            );
        }

        JwtKeyProperties properties =
                keySource.load();

        if (properties == null) {
            throw new IllegalStateException(
                    "JWT key source returned null properties"
            );
        }

        if (properties.getKeys() == null
                || properties.getKeys().isEmpty()) {

            throw new IllegalStateException(
                    "At least one JWT key must be configured"
            );
        }

        for (
                JwtKeyProperties.KeyProperties
                        keyProperties :
                properties.getKeys()
        ) {

            if (keyProperties.getKeyId() == null
                    || keyProperties.getKeyId().isBlank()) {

                throw new IllegalStateException(
                        "JWT key ID must not be blank"
                );
            }

            if (keyProperties.getPublicKey() == null
                    || keyProperties.getPublicKey().isBlank()) {

                throw new IllegalStateException(
                        "JWT public key must not be blank: "
                                + keyProperties.getKeyId()
                );
            }

            PublicKey publicKey =
                    RsaKeyParser.parsePublicKey(
                            keyProperties.getPublicKey()
                    );

            PrivateKey privateKey = null;

            if (keyProperties.getPrivateKey() != null
                    && !keyProperties.getPrivateKey().isBlank()) {

                privateKey =
                        RsaKeyParser.parsePrivateKey(
                                keyProperties.getPrivateKey()
                        );
            }

            addKey(
                    new JwtKey(
                            keyProperties.getKeyId(),
                            privateKey,
                            publicKey
                    )
            );
        }

        setCurrentSigningKey(
                properties.getCurrentKeyId()
        );
    }

    public void addKey(
            JwtKey key
    ) {

        if (key == null) {
            throw new IllegalArgumentException(
                    "JWT key must not be null"
            );
        }

        JwtKey existing =
                keys.putIfAbsent(
                        key.keyId(),
                        key
                );

        if (existing != null) {
            throw new IllegalStateException(
                    "JWT key already exists: "
                            + key.keyId()
            );
        }
    }

    public void setCurrentSigningKey(
            String keyId
    ) {

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Signing key ID must not be blank"
            );
        }

        JwtKey key =
                keys.get(keyId);

        if (key == null) {
            throw new IllegalArgumentException(
                    "Unknown signing key: " + keyId
            );
        }

        if (!key.canSign()) {
            throw new IllegalArgumentException(
                    "Key cannot sign: " + keyId
            );
        }

        currentSigningKeyId.set(
                keyId
        );
    }

    public void removeKey(
            String keyId
    ) {

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Key ID must not be blank"
            );
        }

        String current =
                currentSigningKeyId.get();

        if (keyId.equals(current)) {
            throw new IllegalStateException(
                    "Cannot remove current signing key"
            );
        }

        JwtKey removed =
                keys.remove(keyId);

        if (removed == null) {
            throw new IllegalArgumentException(
                    "Unknown JWT key ID: " + keyId
            );
        }
    }

    @Override
    public String getCurrentKeyId() {

        String keyId =
                currentSigningKeyId.get();

        if (keyId == null) {
            throw new IllegalStateException(
                    "No current signing key configured"
            );
        }

        return keyId;
    }

    @Override
    public PrivateKey getCurrentPrivateKey() {

        JwtKey key =
                getCurrentKey();

        if (!key.canSign()) {
            throw new IllegalStateException(
                    "Current key cannot sign"
            );
        }

        return key.privateKey();
    }

    @Override
    public PublicKey getPublicKey(
            String keyId
    ) {

        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT key ID must not be blank"
            );
        }

        JwtKey key =
                keys.get(keyId);

        if (key == null) {
            throw new IllegalArgumentException(
                    "Unknown JWT key ID: " + keyId
            );
        }

        return key.publicKey();
    }

    @Override
    public Map<String, PublicKey> getPublicKeys() {

        Map<String, PublicKey> publicKeys =
                new LinkedHashMap<>();

        keys.values()
                .forEach(
                        key ->
                                publicKeys.put(
                                        key.keyId(),
                                        key.publicKey()
                                )
                );

        return Collections.unmodifiableMap(
                publicKeys
        );
    }

    private JwtKey getCurrentKey() {

        String keyId =
                getCurrentKeyId();

        JwtKey key =
                keys.get(keyId);

        if (key == null) {
            throw new IllegalStateException(
                    "Current signing key no longer exists"
            );
        }

        return key;
    }
}