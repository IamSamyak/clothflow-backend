package com.clothflow.user.security;

import com.clothflow.user.config.JwtKeyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

@Component
@ConditionalOnProperty(
        name = "jwt.key-source",
        havingValue = "aws"
)
public class AwsSecretsManagerJwtKeySource
        implements JwtKeySource {

    private final SecretsManagerClient secretsManagerClient;

    private final ObjectMapper objectMapper;

    private final JwtKeyProperties applicationProperties;

    public AwsSecretsManagerJwtKeySource(
            SecretsManagerClient secretsManagerClient,
            ObjectMapper objectMapper,
            JwtKeyProperties applicationProperties
    ) {
        this.secretsManagerClient = secretsManagerClient;
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public JwtKeyProperties load() {

        String secretId =
                applicationProperties.getKeySecretId();

        if (secretId == null
                || secretId.isBlank()) {

            throw new IllegalStateException(
                    "JWT key secret ID must be configured " +
                            "when jwt.key-source=aws"
            );
        }

        GetSecretValueRequest request =
                GetSecretValueRequest.builder()
                        .secretId(secretId)
                        .build();

        try {

            GetSecretValueResponse response =
                    secretsManagerClient.getSecretValue(
                            request
                    );

            String secretString =
                    response.secretString();

            if (secretString == null
                    || secretString.isBlank()) {

                throw new IllegalStateException(
                        "JWT key secret contains no secret string"
                );
            }

            return objectMapper.readValue(
                    secretString,
                    JwtKeyProperties.class
            );

        } catch (SecretsManagerException ex) {

            throw new IllegalStateException(
                    "Failed to load JWT key set from " +
                            "AWS Secrets Manager",
                    ex
            );

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "JWT key secret contains invalid JSON",
                    ex
            );
        }
    }
}