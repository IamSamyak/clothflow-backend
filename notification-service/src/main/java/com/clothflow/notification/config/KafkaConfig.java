package com.clothflow.notification.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;

import org.springframework.util.backoff.FixedBackOff;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        properties.put(
                ProducerConfig.RETRIES_CONFIG,
                10
        );

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> producerFactory
    ) {

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate
    ) {

        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(
                                record.topic() + ".DLT",
                                record.partition()
                        )
        );
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer deadLetterPublishingRecoverer,
            NotificationMetrics notificationMetrics
    ) {

        FixedBackOff backOff = new FixedBackOff(
                1000L,
                2L
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                deadLetterPublishingRecoverer,
                backOff
        );

        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class
        );

        errorHandler.setRetryListeners(
                new RetryListener() {

                    @Override
                    public void failedDelivery(
                            org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record,
                            Exception exception,
                            int deliveryAttempt
                    ) {

                        /*
                         * failedDelivery() is invoked by the
                         * DefaultErrorHandler when a delivery
                         * fails.
                         *
                         * deliveryAttempt starts at 1.
                         *
                         * Attempt 1 = original delivery.
                         * Attempt 2+ = retry delivery.
                         */
                        if (deliveryAttempt > 1) {
                            notificationMetrics.kafkaRetry();
                        }
                    }

                    @Override
                    public void recovered(
                            org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record,
                            Exception exception
                    ) {

                        /*
                         * The DeadLetterPublishingRecoverer
                         * successfully recovered the failed
                         * record.
                         */
                        notificationMetrics.kafkaDlt();
                    }

                    @Override
                    public void recoveryFailed(
                            org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record,
                            Exception original,
                            Exception failure
                    ) {

                        /*
                         * DLT publishing itself failed.
                         *
                         * We deliberately do NOT increment
                         * kafkaDlt here because the message was
                         * NOT successfully recovered.
                         */
                    }
                }
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.setCommonErrorHandler(kafkaErrorHandler);

        factory.getContainerProperties()
                .setAckMode(
                        ContainerProperties.AckMode.MANUAL
                );

        return factory;
    }
}
