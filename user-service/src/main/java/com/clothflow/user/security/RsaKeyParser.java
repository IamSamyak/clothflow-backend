package com.clothflow.user.security;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyParser {

    private RsaKeyParser() {
    }

    public static PrivateKey parsePrivateKey(
            String pem
    ) {

        try {

            String keyMaterial =
                    resolveKeyMaterial(pem);

            String normalized =
                    normalizePem(
                            keyMaterial,
                            "PRIVATE KEY"
                    );

            byte[] keyBytes =
                    Base64.getDecoder()
                            .decode(normalized);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(
                            keyBytes
                    );

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(
                    keySpec
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to parse RSA private key",
                    exception
            );
        }
    }

    public static PublicKey parsePublicKey(
            String pem
    ) {

        try {

            String keyMaterial =
                    resolveKeyMaterial(pem);

            String normalized =
                    normalizePem(
                            keyMaterial,
                            "PUBLIC KEY"
                    );

            byte[] keyBytes =
                    Base64.getDecoder()
                            .decode(normalized);

            X509EncodedKeySpec keySpec =
                    new X509EncodedKeySpec(
                            keyBytes
                    );

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(
                    keySpec
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to parse RSA public key",
                    exception
            );
        }
    }

    private static String resolveKeyMaterial(
            String value
    ) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(
                    "RSA key material must not be blank"
            );
        }

        if (!value.startsWith("classpath:")) {
            return value;
        }

        String resourcePath =
                value.substring(
                        "classpath:".length()
                );

        if (resourcePath.isBlank()) {

            throw new IllegalArgumentException(
                    "Classpath RSA key resource must not be blank"
            );
        }

        try {

            ClassPathResource resource =
                    new ClassPathResource(
                            resourcePath
                    );

            if (!resource.exists()) {

                throw new IllegalStateException(
                        "RSA key resource does not exist: "
                                + value
                );
            }

            try (InputStream inputStream =
                         resource.getInputStream()) {

                return new String(
                        inputStream.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to load RSA key resource: "
                            + value,
                    exception
            );
        }
    }

    private static String normalizePem(
            String pem,
            String type
    ) {

        if (pem == null || pem.isBlank()) {

            throw new IllegalArgumentException(
                    "RSA " + type + " must not be blank"
            );
        }

        return pem
                .replace(
                        "-----BEGIN " + type + "-----",
                        ""
                )
                .replace(
                        "-----END " + type + "-----",
                        ""
                )
                .replaceAll(
                        "\\s",
                        ""
                );
    }
}