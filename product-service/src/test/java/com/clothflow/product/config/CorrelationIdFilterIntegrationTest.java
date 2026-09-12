package com.clothflow.product.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CorrelationIdFilterIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("clothflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateCorrelationIdWhenRequestDoesNotProvideOne()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/products")
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "X-Correlation-ID",
                                matchesPattern(
                                        "[0-9a-fA-F-]{36}"
                                )
                        )
                );
    }

    @Test
    void shouldPreserveExistingCorrelationId()
            throws Exception {

        String correlationId =
                "8f7c2a10-7c9d-4f1e-9b42-123456789abc";

        mockMvc.perform(
                        get("/api/v1/products")
                                .header(
                                        "X-Correlation-ID",
                                        correlationId
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "X-Correlation-ID",
                                correlationId
                        )
                );
    }

    @Test
    void shouldClearCorrelationIdFromMdcAfterRequest()
            throws Exception {

        assertThat(org.slf4j.MDC.get("correlationId"))
                .isNull();

        mockMvc.perform(
                get("/api/v1/products")
        );

        assertThat(org.slf4j.MDC.get("correlationId"))
                .isNull();
    }
}