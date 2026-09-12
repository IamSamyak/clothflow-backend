package com.clothflow.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class OutboxPayloadEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int GCM_TAG_LENGTH = 128;

    private static final int IV_LENGTH = 12;

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public OutboxPayloadEncryptionService(
            @Value("${security.encryption.outbox.key}")
            String base64Key
    ) {

        byte[] keyBytes;

        try {

            keyBytes =
                    Base64.getDecoder()
                            .decode(base64Key);

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Invalid OUTBOX_ENCRYPTION_KEY",
                    exception
            );
        }

        if (keyBytes.length != 32) {

            throw new IllegalStateException(
                    "OUTBOX_ENCRYPTION_KEY must decode to 32 bytes"
            );
        }

        this.secretKey =
                new SecretKeySpec(
                        keyBytes,
                        "AES"
                );
    }

    public String encrypt(
            String plaintext
    ) {

        if (plaintext == null) {
            throw new IllegalArgumentException(
                    "Payload must not be null"
            );
        }

        try {

            byte[] iv =
                    new byte[IV_LENGTH];

            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    )
            );

            byte[] ciphertext =
                    cipher.doFinal(
                            plaintext.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            /*
             * Store IV + ciphertext together.
             *
             * Format:
             *
             * Base64(
             *     IV || ciphertext
             * )
             */
            byte[] combined =
                    new byte[
                            iv.length
                                    + ciphertext.length
                            ];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );

            System.arraycopy(
                    ciphertext,
                    0,
                    combined,
                    iv.length,
                    ciphertext.length
            );

            return Base64.getEncoder()
                    .encodeToString(
                            combined
                    );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to encrypt outbox payload",
                    exception
            );
        }
    }

    public String decrypt(
            String encryptedPayload
    ) {

        if (encryptedPayload == null) {
            throw new IllegalArgumentException(
                    "Encrypted payload must not be null"
            );
        }

        try {

            byte[] combined =
                    Base64.getDecoder()
                            .decode(
                                    encryptedPayload
                            );

            if (combined.length <= IV_LENGTH) {

                throw new IllegalArgumentException(
                        "Invalid encrypted payload"
                );
            }

            byte[] iv =
                    new byte[IV_LENGTH];

            byte[] ciphertext =
                    new byte[
                            combined.length
                                    - IV_LENGTH
                            ];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    ciphertext,
                    0,
                    ciphertext.length
            );

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    )
            );

            byte[] plaintext =
                    cipher.doFinal(
                            ciphertext
                    );

            return new String(
                    plaintext,
                    StandardCharsets.UTF_8
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to decrypt outbox payload",
                    exception
            );
        }
    }
}