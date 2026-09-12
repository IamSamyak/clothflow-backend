package com.clothflow.order.repository;

import com.clothflow.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    @Query(
            value = """
                    SELECT *
                    FROM outbox_event
                    WHERE status = 'PENDING'
                      AND next_attempt_at <= :now
                    ORDER BY created_at
                    LIMIT 100
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findPendingEventsForUpdate(
            @Param("now") OffsetDateTime now
    );

    @Query(
            value = """
                    SELECT *
                    FROM outbox_event
                    WHERE status = 'PROCESSING'
                      AND next_attempt_at <= :now
                    ORDER BY created_at
                    LIMIT 100
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findStaleProcessingEventsForUpdate(
            @Param("now") OffsetDateTime now
    );
}