package com.clothflow.product;

import com.clothflow.product.dto.CreateProductRequest;
import com.clothflow.product.dto.ProductResponse;
import com.clothflow.product.repository.ProductRepository;
import com.clothflow.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("clothflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

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
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldCreateAndRetrieveProduct() {

        CreateProductRequest request =
                new CreateProductRequest(
                        "Integration Saree",
                        "Created by integration test",
                        new BigDecimal("2999.00"),
                        "INT-001"
                );

        ProductResponse created =
                productService.createProduct(request);

        assertNotNull(created.id());
        assertEquals("Integration Saree", created.name());
        assertEquals("INT-001", created.sku());

        ProductResponse retrieved =
                productService.getProductById(created.id());

        assertEquals(created.id(), retrieved.id());
        assertEquals("Integration Saree", retrieved.name());
        assertEquals("INT-001", retrieved.sku());
    }

    @Test
    void shouldPersistProductInDatabase() {

        CreateProductRequest request =
                new CreateProductRequest(
                        "Database Saree",
                        "Database persistence test",
                        new BigDecimal("3999.00"),
                        "DB-001"
                );

        ProductResponse created =
                productService.createProduct(request);

        assertTrue(
                productRepository.existsById(created.id())
        );
    }
}