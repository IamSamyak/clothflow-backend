package com.clothflow.product.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfig {

    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16")
                .withDatabaseName("clothflow_test")
                .withUsername("test")
                .withPassword("test");
    }
}
