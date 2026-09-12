package com.clothflow.user.repository;

import com.clothflow.user.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
                    FROM outbox_events
                    WHERE status = 'PENDING'
                      AND (
                            next_attempt_at IS NULL
                            OR next_attempt_at <= CURRENT_TIMESTAMP
                          )
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findPendingForUpdate(
            Pageable pageable
    );

    @Modifying
    @Query("""
        update OutboxEvent event
        set event.status = 'PENDING',
            event.processingStartedAt = null,
            event.nextAttemptAt = CURRENT_TIMESTAMP
        where event.status = 'PROCESSING'
          and event.processingStartedAt < :cutoff
        """)
    int releaseExpiredProcessingEvents(
            @Param("cutoff")
            OffsetDateTime cutoff
    );

    @Modifying
    @Query("""
        DELETE FROM OutboxEvent event
        WHERE event.eventType =
              'PASSWORD_RESET_REQUESTED'
          AND event.status = 'PUBLISHED'
          AND event.publishedAt < :cutoff
        """)
    int deletePublishedPasswordResetEventsBefore(
            @Param("cutoff") OffsetDateTime cutoff
    );
}