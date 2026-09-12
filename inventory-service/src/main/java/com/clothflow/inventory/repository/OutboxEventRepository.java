package com.clothflow.inventory.repository;

import com.clothflow.inventory.entity.OutboxEvent;
import com.clothflow.inventory.entity.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxEventStatus status
    );

    @Query(
            value = """
                SELECT *
                FROM outbox_event
                WHERE status = 'PENDING'
                  AND (
                        next_attempt_at IS NULL
                        OR next_attempt_at <= CURRENT_TIMESTAMP
                  )
                ORDER BY created_at
                LIMIT 100
                FOR UPDATE SKIP LOCKED
                """,
            nativeQuery = true
    )
    List<OutboxEvent> claimPendingEvents();

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = com.clothflow.inventory.entity.OutboxEventStatus.PROCESSING
              AND e.processingStartedAt < :cutoff
            """)
    List<OutboxEvent> findStaleProcessingEvents(
            @Param("cutoff") LocalDateTime cutoff
    );
}