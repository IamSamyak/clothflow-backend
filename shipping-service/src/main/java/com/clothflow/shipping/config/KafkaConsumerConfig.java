package com.clothflow.shipping.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {

        Map<String, Object> properties =
                new HashMap<>();

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

        /*
         * Kafka offset is committed only after our
         * business transaction succeeds and the listener
         * acknowledges the message.
         */
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        /*
         * Useful for a new consumer group.
         *
         * Existing committed offsets are still respected.
         */
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                properties
        );
    }

    @Bean
    public ProducerFactory<String, String>
    kafkaProducerFactory() {

        Map<String, Object> properties =
                new HashMap<>();

        properties.put(
                org.apache.kafka.clients.producer.ProducerConfig
                        .BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                org.apache.kafka.clients.producer.ProducerConfig
                        .KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                org.apache.kafka.clients.producer.ProducerConfig
                        .VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        /*
         * Wait for all in-sync replicas.
         */
        properties.put(
                org.apache.kafka.clients.producer.ProducerConfig
                        .ACKS_CONFIG,
                "all"
        );

        /*
         * Producer idempotence prevents duplicate writes
         * caused by producer retries.
         */
        properties.put(
                org.apache.kafka.clients.producer.ProducerConfig
                        .ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        properties.put(
                org.apache.kafka.clients.producer.ProducerConfig
                        .RETRIES_CONFIG,
                10
        );

        return new DefaultKafkaProducerFactory<>(
                properties
        );
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> producerFactory
    ) {
        return new KafkaTemplate<>(
                producerFactory
        );
    }

    @Bean
    public DeadLetterPublishingRecoverer
    deadLetterPublishingRecoverer(
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
            DeadLetterPublishingRecoverer
                    deadLetterPublishingRecoverer
    ) {

        /*
         * Retry twice with a 1-second delay.
         *
         * If the message still fails, it is sent to
         * the Dead Letter Topic.
         */
        FixedBackOff backOff =
                new FixedBackOff(
                        1000L,
                        2L
                );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        deadLetterPublishingRecoverer,
                        backOff
                );

        /*
         * Invalid event types are not transient failures.
         *
         * Retrying them would not fix the message.
         */
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String,
            String
            > kafkaListenerContainerFactory(
            ConsumerFactory<String, String>
                    consumerFactory,
            DefaultErrorHandler
                    kafkaErrorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<
                String,
                String
                > factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                consumerFactory
        );

        factory.setCommonErrorHandler(
                kafkaErrorHandler
        );

        /*
         * Business code explicitly acknowledges the event
         * only after the database transaction succeeds.
         */
        factory.getContainerProperties()
                .setAckMode(
                        ContainerProperties.AckMode.MANUAL
                );

        return factory;
    }
}