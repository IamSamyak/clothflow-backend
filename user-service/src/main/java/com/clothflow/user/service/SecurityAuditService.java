package com.clothflow.user.service;

import com.clothflow.user.entity.SecurityAuditEvent;
import com.clothflow.user.entity.SecurityAuditEventType;
import com.clothflow.user.entity.SecurityAuditOutcome;
import com.clothflow.user.repository.SecurityAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SecurityAuditService {

    private final SecurityAuditEventRepository repository;

    public SecurityAuditService(
            SecurityAuditEventRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void record(
            UUID userId,
            SecurityAuditEventType eventType,
            SecurityAuditOutcome outcome,
            String ipAddress,
            String userAgent,
            String correlationId,
            String details
    ) {

        SecurityAuditEvent event =
                new SecurityAuditEvent(
                        userId,
                        eventType,
                        outcome,
                        ipAddress,
                        userAgent,
                        correlationId,
                        details
                );

        repository.save(event);
    }
}