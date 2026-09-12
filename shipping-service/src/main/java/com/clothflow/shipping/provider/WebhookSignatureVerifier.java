package com.clothflow.shipping.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM =
            "HmacSHA256";

    private final String webhookSecret;

    private final long toleranceSeconds;

    public WebhookSignatureVerifier(
            @Value("${shipping.webhook.secret}")
            String webhookSecret,

            @Value("${shipping.webhook.tolerance-seconds:300}")
            long toleranceSeconds
    ) {

        if (webhookSecret == null ||
                webhookSecret.isBlank()) {

            throw new IllegalArgumentException(
                    "Shipping webhook secret must be configured"
            );
        }

        if (toleranceSeconds <= 0) {

            throw new IllegalArgumentException(
                    "Webhook tolerance must be greater than zero"
            );
        }

        this.webhookSecret =
                webhookSecret;

        this.toleranceSeconds =
                toleranceSeconds;
    }

    public boolean verify(
            String eventId,
            String timestamp,
            String trackingNumber,
            String status,
            String providedSignature
    ) {

        if (eventId == null ||
                timestamp == null ||
                trackingNumber == null ||
                status == null ||
                providedSignature == null) {

            return false;
        }

        if (!isTimestampValid(timestamp)) {
            return false;
        }

        String payload =
                eventId
                        + "."
                        + timestamp
                        + "."
                        + trackingNumber
                        + "."
                        + status;

        String expectedSignature =
                generateSignature(
                        payload
                );

        return MessageDigest.isEqual(
                expectedSignature.getBytes(
                        StandardCharsets.UTF_8
                ),
                providedSignature.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private boolean isTimestampValid(
            String timestamp
    ) {

        try {

            long providerTimestamp =
                    Long.parseLong(
                            timestamp
                    );

            long currentTimestamp =
                    Instant.now()
                            .getEpochSecond();

            long difference =
                    Math.abs(
                            currentTimestamp
                                    - providerTimestamp
                    );

            return difference <=
                    toleranceSeconds;

        } catch (NumberFormatException ex) {

            return false;
        }
    }

    private String generateSignature(
            String payload
    ) {

        try {

            Mac mac =
                    Mac.getInstance(
                            HMAC_ALGORITHM
                    );

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            webhookSecret.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            HMAC_ALGORITHM
                    );

            mac.init(secretKey);

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

            throw new IllegalStateException(
                    "Failed to generate webhook signature",
                    ex
            );
        }
    }
}