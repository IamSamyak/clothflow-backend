package com.clothflow.product.controller;

import com.clothflow.product.entity.Product;
import com.clothflow.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductControllerIntegrationTest {

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

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void shouldCreateProduct() throws Exception {

        String request = """
                {
                    "name": "Silk Saree",
                    "description": "Traditional silk saree",
                    "price": 4999.00,
                    "sku": "SAR-API-001"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Silk Saree")))
                .andExpect(jsonPath("$.sku", is("SAR-API-001")))
                .andExpect(jsonPath("$.price", is(4999.00)));
    }

    @Test
    void shouldRejectInvalidProduct() throws Exception {

        String request = """
                {
                    "name": "",
                    "description": "Invalid product",
                    "price": -100,
                    "sku": ""
                }
                """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void shouldRejectDuplicateSku() throws Exception {

        createProduct();

        String request = """
                {
                    "name": "Another Saree",
                    "description": "Another product",
                    "price": 2999.00,
                    "sku": "SAR-API-001"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Test
    void shouldGetProductById() throws Exception {

        Product product = createProduct();

        mockMvc.perform(
                        get(
                                "/api/v1/products/{id}",
                                product.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.id",
                        is(product.getId().toString())
                ))
                .andExpect(jsonPath(
                        "$.name",
                        is("Silk Saree")
                ))
                .andExpect(jsonPath(
                        "$.sku",
                        is("SAR-API-001")
                ));
    }

    @Test
    void shouldReturn404WhenProductDoesNotExist()
            throws Exception {

        UUID unknownProductId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/products/{id}",
                                unknownProductId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath(
                        "$.error",
                        is("Not Found")
                ));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void shouldUpdateProduct() throws Exception {

        Product product = createProduct();

        String request = """
                {
                    "name": "Premium Silk Saree",
                    "description": "Premium silk saree",
                    "price": 5999.00,
                    "sku": "SAR-API-001",
                    "version": 0
                }
                """;

        mockMvc.perform(
                        put(
                                "/api/v1/products/{id}",
                                product.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.name",
                        is("Premium Silk Saree")
                ))
                .andExpect(jsonPath(
                        "$.price",
                        is(5999.00)
                ))
                .andExpect(jsonPath(
                        "$.version",
                        is(1)
                ));
    }

    @Test
    void shouldRejectStaleVersion() throws Exception {

        Product product = createProduct();

        String request = """
                {
                    "name": "Updated Saree",
                    "description": "Updated",
                    "price": 5999.00,
                    "sku": "SAR-API-001",
                    "version": 99
                }
                """;

        mockMvc.perform(
                        put(
                                "/api/v1/products/{id}",
                                product.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void shouldSoftDeleteProduct() throws Exception {

        Product product = createProduct();

        mockMvc.perform(
                        delete(
                                "/api/v1/products/{id}",
                                product.getId()
                        )
                                .param("version", "0")
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get(
                                "/api/v1/products/{id}",
                                product.getId()
                        )
                )
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // PAGINATION
    // =========================================================

    @Test
    void shouldReturnPaginatedProducts() throws Exception {

        createProduct(
                "SAR-API-001",
                "Silk Saree",
                "4999.00"
        );

        createProduct(
                "SAR-API-002",
                "Cotton Saree",
                "2999.00"
        );

        createProduct(
                "SAR-API-003",
                "Designer Saree",
                "7999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content.length()",
                        is(2)
                ))
                .andExpect(jsonPath(
                        "$.page",
                        is(0)
                ))
                .andExpect(jsonPath(
                        "$.size",
                        is(2)
                ))
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(3)
                ))
                .andExpect(jsonPath(
                        "$.totalPages",
                        is(2)
                ))
                .andExpect(jsonPath(
                        "$.first",
                        is(true)
                ))
                .andExpect(jsonPath(
                        "$.last",
                        is(false)
                ));
    }

    // =========================================================
    // NAME FILTER
    // =========================================================

    @Test
    void shouldFilterProductsByName() throws Exception {

        createProduct(
                "SAR-API-001",
                "Silk Saree",
                "4999.00"
        );

        createProduct(
                "SAR-API-002",
                "Cotton Saree",
                "2999.00"
        );

        createProduct(
                "DRE-API-001",
                "Designer Dress",
                "3999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("name", "silk")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(1)
                ))
                .andExpect(jsonPath(
                        "$.content[0].name",
                        is("Silk Saree")
                ));
    }

    @Test
    void shouldSearchProductNameCaseInsensitively()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Silk Saree",
                "4999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("name", "SILK")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(1)
                ))
                .andExpect(jsonPath(
                        "$.content[0].name",
                        is("Silk Saree")
                ));
    }

    // =========================================================
    // PRICE FILTER
    // =========================================================

    @Test
    void shouldFilterProductsByMinimumPrice()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Budget Saree",
                "999.00"
        );

        createProduct(
                "SAR-API-002",
                "Silk Saree",
                "4999.00"
        );

        createProduct(
                "SAR-API-003",
                "Premium Saree",
                "9999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("minPrice", "4000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(2)
                ));
    }

    @Test
    void shouldFilterProductsByMaximumPrice()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Budget Saree",
                "999.00"
        );

        createProduct(
                "SAR-API-002",
                "Silk Saree",
                "4999.00"
        );

        createProduct(
                "SAR-API-003",
                "Premium Saree",
                "9999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("maxPrice", "5000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(2)
                ));
    }

    @Test
    void shouldFilterProductsByPriceRange()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Budget Saree",
                "999.00"
        );

        createProduct(
                "SAR-API-002",
                "Silk Saree",
                "4999.00"
        );

        createProduct(
                "SAR-API-003",
                "Premium Saree",
                "9999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("minPrice", "2000")
                                .param("maxPrice", "6000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(1)
                ))
                .andExpect(jsonPath(
                        "$.content[0].name",
                        is("Silk Saree")
                ));
    }

    // =========================================================
    // SORTING
    // =========================================================

    @Test
    void shouldSortProductsByPriceAscending()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Expensive Saree",
                "9999.00"
        );

        createProduct(
                "SAR-API-002",
                "Cheap Saree",
                "999.00"
        );

        createProduct(
                "SAR-API-003",
                "Medium Saree",
                "4999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("sort", "price,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[0].name",
                        is("Cheap Saree")
                ))
                .andExpect(jsonPath(
                        "$.content[1].name",
                        is("Medium Saree")
                ))
                .andExpect(jsonPath(
                        "$.content[2].name",
                        is("Expensive Saree")
                ));
    }

    @Test
    void shouldSortProductsByPriceDescending()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Expensive Saree",
                "9999.00"
        );

        createProduct(
                "SAR-API-002",
                "Cheap Saree",
                "999.00"
        );

        createProduct(
                "SAR-API-003",
                "Medium Saree",
                "4999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("sort", "price,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[0].name",
                        is("Expensive Saree")
                ))
                .andExpect(jsonPath(
                        "$.content[1].name",
                        is("Medium Saree")
                ))
                .andExpect(jsonPath(
                        "$.content[2].name",
                        is("Cheap Saree")
                ));
    }

    // =========================================================
    // VALIDATION / HARDENING
    // =========================================================

    @Test
    void shouldRejectInvalidPriceRange()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("minPrice", "10000")
                                .param("maxPrice", "5000")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.status",
                        is(400)
                ));
    }

    @Test
    void shouldRejectUnsupportedSortField()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("sort", "deleted,asc")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.status",
                        is(400)
                ));
    }

    @Test
    void shouldLimitMaximumPageSize()
            throws Exception {

        createProduct(
                "SAR-API-001",
                "Silk Saree",
                "4999.00"
        );

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "1000")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.size",
                        is(100)
                ));
    }

    // =========================================================
    // CONCURRENCY
    // =========================================================

    @Test
    void shouldAllowOnlyOneConcurrentUpdateForSameVersion()
            throws Exception {

        Product product = createProduct(
                "CONC-002",
                "Concurrent Saree",
                "3999.00"
        );

        Long version = product.getVersion();

        String request1 = """
            {
                "name": "Concurrent Update A",
                "description": "Updated by thread A",
                "price": 4499.00,
                "sku": "CONC-002",
                "version": %d
            }
            """.formatted(version);

        String request2 = """
            {
                "name": "Concurrent Update B",
                "description": "Updated by thread B",
                "price": 4999.00,
                "sku": "CONC-002",
                "version": %d
            }
            """.formatted(version);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            Future<Integer> result1 =
                    executor.submit(() -> {

                        startLatch.await();

                        return mockMvc.perform(
                                        put(
                                                "/api/v1/products/{id}",
                                                product.getId()
                                        )
                                                .contentType(
                                                        MediaType.APPLICATION_JSON
                                                )
                                                .content(request1)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    });

            Future<Integer> result2 =
                    executor.submit(() -> {

                        startLatch.await();

                        return mockMvc.perform(
                                        put(
                                                "/api/v1/products/{id}",
                                                product.getId()
                                        )
                                                .contentType(
                                                        MediaType.APPLICATION_JSON
                                                )
                                                .content(request2)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    });

            startLatch.countDown();

            int status1 = result1.get();
            int status2 = result2.get();

            assertThat(List.of(status1, status2))
                    .containsExactlyInAnyOrder(200, 409);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldRejectDuplicateSkuAtDatabaseBoundary()
            throws Exception {

        createProduct(
                "DB-SKU-001",
                "Existing Product",
                "1999.00"
        );

        String request = """
            {
                "name": "Another Product",
                "description": "Duplicate SKU test",
                "price": 2999.00,
                "sku": "DB-SKU-001"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAllowOnlyOneConcurrentProductCreationForSameSku()
            throws Exception {

        String request1 = """
            {
                "name": "Concurrent Product A",
                "description": "Created concurrently",
                "price": 1999.00,
                "sku": "RACE-SKU-001"
            }
            """;

        String request2 = """
            {
                "name": "Concurrent Product B",
                "description": "Created concurrently",
                "price": 2999.00,
                "sku": "RACE-SKU-001"
            }
            """;

        CountDownLatch startLatch =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            Future<Integer> result1 =
                    executor.submit(() -> {

                        startLatch.await();

                        return mockMvc.perform(
                                        post("/api/v1/products")
                                                .contentType(
                                                        MediaType.APPLICATION_JSON
                                                )
                                                .content(request1)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    });

            Future<Integer> result2 =
                    executor.submit(() -> {

                        startLatch.await();

                        return mockMvc.perform(
                                        post("/api/v1/products")
                                                .contentType(
                                                        MediaType.APPLICATION_JSON
                                                )
                                                .content(request2)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    });

            startLatch.countDown();

            int status1 = result1.get();
            int status2 = result2.get();

            assertThat(List.of(status1, status2))
                    .containsExactlyInAnyOrder(201, 409);

            long count =
                    productRepository.findAll()
                            .stream()
                            .filter(existingProduct ->
                                    "RACE-SKU-001"
                                            .equals(existingProduct.getSku()))
                            .count();

            assertEquals(1, count);

        } finally {
            executor.shutdown();
        }
    }

    // =========================================================
    // TEST HELPERS
    // =========================================================

    private Product createProduct() {

        Product product = new Product();

        product.setName("Silk Saree");
        product.setDescription(
                "Traditional silk saree"
        );
        product.setPrice(
                new BigDecimal("4999.00")
        );
        product.setSku("SAR-API-001");

        return productRepository.save(product);
    }

    private Product createProduct(
            String sku,
            String name,
            String price) {

        Product product = new Product();

        product.setName(name);
        product.setDescription("Test product");
        product.setPrice(
                new BigDecimal(price)
        );
        product.setSku(sku);

        return productRepository.save(product);
    }
}