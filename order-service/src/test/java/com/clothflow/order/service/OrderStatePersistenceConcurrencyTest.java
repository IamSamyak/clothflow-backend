//package com.clothflow.order.service;
//
//import com.clothflow.order.entity.Order;
//import com.clothflow.order.entity.OrderStatus;
//import com.clothflow.order.repository.OrderRepository;
//import com.clothflow.order.service.OrderStatePersistenceService;
//import com.clothflow.order.service.PaymentTransitionResult;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//import org.testcontainers.postgresql.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@Testcontainers
//@DataJpaTest
//@ActiveProfiles("test")
//@Transactional(propagation = Propagation.NOT_SUPPORTED)
//class OrderStatePersistenceConcurrencyTest {
//
//    @Container
//    static PostgreSQLContainer POSTGRES =
//            new PostgreSQLContainer("postgres:16")
//                    .withDatabaseName("clothflow_order_test")
//                    .withUsername("test")
//                    .withPassword("test");
//
//    @DynamicPropertySource
//    static void registerProperties(
//            DynamicPropertyRegistry registry
//    ) {
//
//        registry.add(
//                "spring.datasource.url",
//                POSTGRES::getJdbcUrl
//        );
//
//        registry.add(
//                "spring.datasource.username",
//                POSTGRES::getUsername
//        );
//
//        registry.add(
//                "spring.datasource.password",
//                POSTGRES::getPassword
//        );
//
//        registry.add(
//                "spring.flyway.enabled",
//                () -> true
//        );
//    }
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private OrderStatePersistenceService orderStatePersistenceService;
//
//    private UUID orderId;
//
//    @BeforeEach
//    void setUp() {
//
//        Order order =
//                new Order(
//                        "TEST-" + UUID.randomUUID(),
//                        UUID.randomUUID(),
//                        OrderStatus.INVENTORY_RESERVED,
//                        new BigDecimal("888.88"),
//                        "INR"
//                );
//
//        Order saved =
//                orderRepository.saveAndFlush(order);
//
//        orderId = saved.getId();
//    }
//
//    @Test
//    void shouldAllowOnlyOneConcurrentPaymentPendingTransition()
//            throws Exception {
//
//        int requestCount = 10;
//
//        ExecutorService executor =
//                Executors.newFixedThreadPool(
//                        requestCount
//                );
//
//        CountDownLatch startLatch =
//                new CountDownLatch(1);
//
//        try {
//
//            List<Future<PaymentTransitionResult>> futures =
//                    new ArrayList<>();
//
//            for (int i = 0; i < requestCount; i++) {
//
//                futures.add(
//                        executor.submit(() -> {
//
//                            startLatch.await();
//
//                            return orderStatePersistenceService
//                                    .markPaymentPending(orderId);
//                        })
//                );
//            }
//
//            startLatch.countDown();
//
//            int successfulTransitions = 0;
//            int alreadyPending = 0;
//
//            for (Future<PaymentTransitionResult> future :
//                    futures) {
//
//                PaymentTransitionResult result =
//                        future.get();
//
//                if (result.transitioned()) {
//                    successfulTransitions++;
//                } else {
//                    alreadyPending++;
//                }
//            }
//
//            assertThat(successfulTransitions)
//                    .isEqualTo(1);
//
//            assertThat(alreadyPending)
//                    .isEqualTo(9);
//
//            Order finalOrder =
//                    orderRepository
//                            .findById(orderId)
//                            .orElseThrow();
//
//            assertThat(finalOrder.getStatus())
//                    .isEqualTo(
//                            OrderStatus.PAYMENT_PENDING
//                    );
//
//        } finally {
//
//            executor.shutdownNow();
//        }
//    }
//}