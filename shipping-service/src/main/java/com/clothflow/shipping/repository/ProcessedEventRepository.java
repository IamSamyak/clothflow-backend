package com.clothflow.shipping.repository;

import com.clothflow.shipping.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventId(
            UUID eventId
    );

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_event (
                        event_id,
                        event_type,
                        aggregate_id,
                        processed_at
                    )
                    VALUES (
                        :eventId,
                        :eventType,
                        :aggregateId,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (event_id)
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfNotProcessed(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("aggregateId") UUID aggregateId
    );
}