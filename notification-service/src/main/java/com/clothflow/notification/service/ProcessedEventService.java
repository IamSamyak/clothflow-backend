package com.clothflow.notification.service;

import com.clothflow.notification.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessedEventService {

    private final ProcessedEventRepository
            processedEventRepository;

    public ProcessedEventService(
            ProcessedEventRepository processedEventRepository
    ) {
        this.processedEventRepository =
                processedEventRepository;
    }

    @Transactional
    public boolean tryMarkProcessed(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId
    ) {

        int inserted =
                processedEventRepository
                        .insertIfNotProcessed(
                                eventId,
                                eventType,
                                aggregateType,
                                aggregateId
                        );

        return inserted == 1;
    }
}