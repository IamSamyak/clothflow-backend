package com.clothflow.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@ConditionalOnProperty(
        name = "jwt.key-source",
        havingValue = "aws"
)
public class AwsSecretsManagerConfig {

    @Bean(destroyMethod = "close")
    public SecretsManagerClient secretsManagerClient(
            @Value("${AWS_REGION:us-east-1}")
            String region
    ) {

        return SecretsManagerClient.builder()
                .region(
                        Region.of(region)
                )
                .build();
    }
}