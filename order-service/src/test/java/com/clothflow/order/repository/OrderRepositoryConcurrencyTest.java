package com.clothflow.order.repository;

import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ActiveProfiles("test")
class OrderRepositoryConcurrencyTest {

    @Container
    static PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("clothflow_order_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID orderId;

    @BeforeEach
    void setUp() {

        Order order =
                new Order(
                        "TEST-" + UUID.randomUUID(),
                        UUID.randomUUID(),
                        OrderStatus.INVENTORY_RESERVED,
                        new BigDecimal("888.88"),
                        "INR",

                        // Shipping snapshot
                        "Test Customer",
                        "123 Test Street",
                        "Apartment 101",
                        "Pune",
                        "Maharashtra",
                        "411001",
                        "India"
                );

        Order saved =
                orderRepository.saveAndFlush(order);

        orderId = saved.getId();
    }

    @Test
    void shouldAllowOnlyOneConcurrentPaymentTransition()
            throws Exception {

        int requestCount = 10;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        requestCount
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        try {

            List<Future<Integer>> futures =
                    new ArrayList<>();

            for (int i = 0; i < requestCount; i++) {

                futures.add(
                        executor.submit(() -> {

                            startLatch.await();

                            return transactionTemplate.execute(
                                    status ->
                                            orderRepository
                                                    .transitionStatusAtomically(
                                                            orderId,
                                                            OrderStatus.INVENTORY_RESERVED,
                                                            OrderStatus.PAYMENT_PENDING
                                                    )
                            );
                        })
                );
            }

            // Release all threads at approximately the same time.
            startLatch.countDown();

            int successfulTransitions = 0;

            for (Future<Integer> future : futures) {

                Integer updatedRows =
                        future.get();

                successfulTransitions +=
                        updatedRows;
            }

            /*
             * Only one transaction should be able to change:
             *
             * INVENTORY_RESERVED
             *          ↓
             * PAYMENT_PENDING
             *
             * The WHERE clause in transitionStatusAtomically()
             * guarantees that once the first transaction changes
             * the status, the remaining transactions update 0 rows.
             */
            assertThat(successfulTransitions)
                    .isEqualTo(1);

            Order finalOrder =
                    orderRepository
                            .findById(orderId)
                            .orElseThrow();

            assertThat(finalOrder.getStatus())
                    .isEqualTo(
                            OrderStatus.PAYMENT_PENDING
                    );

        } finally {

            executor.shutdownNow();
        }
    }
}