package com.clothflow.order.service;

import com.clothflow.order.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

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
            UUID aggregateId
    ) {

        int inserted =
                processedEventRepository.insertIfNotProcessed(
                        eventId,
                        eventType,
                        aggregateId
                );

        return inserted == 1;
    }
}