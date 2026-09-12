package com.clothflow.shipping.provider;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureVerifierTest {

    private static final String SECRET =
            "test-secret";

    private final WebhookSignatureVerifier verifier =
            new WebhookSignatureVerifier(
                    SECRET,
                    300
            );

    @Test
    void shouldAcceptValidSignatureAndTimestamp() {

        String eventId =
                "event-001";

        String timestamp =
                String.valueOf(
                        System.currentTimeMillis() / 1000
                );

        String trackingNumber =
                "CF-ABC12345";

        String status =
                "DELIVERED";

        String signature =
                generateSignature(
                        eventId,
                        timestamp,
                        trackingNumber,
                        status
                );

        assertTrue(
                verifier.verify(
                        eventId,
                        timestamp,
                        trackingNumber,
                        status,
                        signature
                )
        );
    }

    @Test
    void shouldRejectInvalidSignature() {

        String timestamp =
                String.valueOf(
                        System.currentTimeMillis() / 1000
                );

        assertFalse(
                verifier.verify(
                        "event-001",
                        timestamp,
                        "CF-ABC12345",
                        "DELIVERED",
                        "invalid-signature"
                )
        );
    }

    @Test
    void shouldRejectExpiredTimestamp() {

        long oldTimestamp =
                (System.currentTimeMillis() / 1000)
                        - 1000;

        String timestamp =
                String.valueOf(
                        oldTimestamp
                );

        String signature =
                generateSignature(
                        "event-001",
                        timestamp,
                        "CF-ABC12345",
                        "DELIVERED"
                );

        assertFalse(
                verifier.verify(
                        "event-001",
                        timestamp,
                        "CF-ABC12345",
                        "DELIVERED",
                        signature
                )
        );
    }

    @Test
    void shouldRejectFutureTimestampOutsideTolerance() {

        long futureTimestamp =
                (System.currentTimeMillis() / 1000)
                        + 1000;

        String timestamp =
                String.valueOf(
                        futureTimestamp
                );

        String signature =
                generateSignature(
                        "event-001",
                        timestamp,
                        "CF-ABC12345",
                        "DELIVERED"
                );

        assertFalse(
                verifier.verify(
                        "event-001",
                        timestamp,
                        "CF-ABC12345",
                        "DELIVERED",
                        signature
                )
        );
    }

    private String generateSignature(
            String eventId,
            String timestamp,
            String trackingNumber,
            String status
    ) {

        try {

            String payload =
                    eventId
                            + "."
                            + timestamp
                            + "."
                            + trackingNumber
                            + "."
                            + status;

            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );

            SecretKeySpec key =
                    new SecretKeySpec(
                            SECRET.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            "HmacSHA256"
                    );

            mac.init(key);

            byte[] digest =
                    mac.doFinal(
                            payload.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : digest) {

                result.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return result.toString();

        } catch (Exception ex) {

            throw new IllegalStateException(ex);
        }
    }
}