package com.clothflow.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

//@Component
public class LocalRsaKeyProvider
        implements JwtKeyProvider {

    private final String currentKeyId;

    private final PrivateKey currentPrivateKey;

    private final Map<String, PublicKey> publicKeys;

    public LocalRsaKeyProvider(
            @Value("${jwt.key-id}")
            String currentKeyId,

            @Value("${jwt.private-key}")
            String privateKeyValue,

            @Value("${jwt.public-key}")
            String publicKeyValue
    ) {
        this.currentKeyId = currentKeyId;

        this.currentPrivateKey =
                loadPrivateKey(privateKeyValue);

        PublicKey currentPublicKey =
                loadPublicKey(publicKeyValue);

        this.publicKeys =
                Map.of(
                        currentKeyId,
                        currentPublicKey
                );
    }

    @Override
    public String getCurrentKeyId() {
        return currentKeyId;
    }

    @Override
    public PrivateKey getCurrentPrivateKey() {
        return currentPrivateKey;
    }

    @Override
    public PublicKey getPublicKey(
            String keyId
    ) {
        return publicKeys.get(keyId);
    }

    @Override
    public Map<String, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    private PrivateKey loadPrivateKey(
            String value
    ) {

        try {

            String normalized =
                    value
                            .replace(
                                    "-----BEGIN PRIVATE KEY-----",
                                    ""
                            )
                            .replace(
                                    "-----END PRIVATE KEY-----",
                                    ""
                            )
                            .replaceAll("\\s+", "");

            byte[] decoded =
                    Base64.getDecoder()
                            .decode(normalized);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(decoded);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(
                    keySpec
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to load RSA private key",
                    ex
            );
        }
    }

    private PublicKey loadPublicKey(
            String value
    ) {

        try {

            String normalized =
                    value
                            .replace(
                                    "-----BEGIN PUBLIC KEY-----",
                                    ""
                            )
                            .replace(
                                    "-----END PUBLIC KEY-----",
                                    ""
                            )
                            .replaceAll("\\s+", "");

            byte[] decoded =
                    Base64.getDecoder()
                            .decode(normalized);

            X509EncodedKeySpec keySpec =
                    new X509EncodedKeySpec(decoded);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(
                    keySpec
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to load RSA public key",
                    ex
            );
        }
    }
}