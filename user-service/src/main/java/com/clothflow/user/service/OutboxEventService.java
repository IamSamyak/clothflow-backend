package com.clothflow.user.service;

import com.clothflow.user.entity.OutboxEvent;
import com.clothflow.user.event.PasswordResetRequestedEvent;
import com.clothflow.user.repository.OutboxEventRepository;
import com.clothflow.user.security.OutboxPayloadEncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxEventService {

    private static final String AGGREGATE_TYPE_USER = "USER";

    private static final String EVENT_TYPE_PASSWORD_RESET_REQUESTED =
            "PASSWORD_RESET_REQUESTED";

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    private final OutboxPayloadEncryptionService outboxPayloadEncryptionService;

    public OutboxEventService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper, OutboxPayloadEncryptionService outboxPayloadEncryptionService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.outboxPayloadEncryptionService = outboxPayloadEncryptionService;
    }

    @Transactional
    public void createPasswordResetRequestedEvent(
            UUID userId,
            String email,
            String rawResetToken
    ) {

        UUID eventId = UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        email,
                        rawResetToken
                );

        String payload;

        try {

            String plaintextPayload =
                    objectMapper.writeValueAsString(
                            event
                    );

            payload =
                    outboxPayloadEncryptionService.encrypt(
                            plaintextPayload
                    );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to serialize password reset event",
                    exception
            );
        }

        OutboxEvent outboxEvent =
                new OutboxEvent(
                        AGGREGATE_TYPE_USER,
                        userId,
                        EVENT_TYPE_PASSWORD_RESET_REQUESTED,
                        payload
                );

        outboxEventRepository.save(
                outboxEvent
        );
    }
}