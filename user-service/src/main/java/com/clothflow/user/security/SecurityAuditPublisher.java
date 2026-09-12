package com.clothflow.user.security;

import com.clothflow.user.service.SecurityAuditService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SecurityAuditPublisher {

    private final SecurityAuditService securityAuditService;

    public SecurityAuditPublisher(
            SecurityAuditService securityAuditService
    ) {
        this.securityAuditService = securityAuditService;
    }

    public void publishAfterCommit(
            SecurityAuditCommand command
    ) {

        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {

            /*
             * No surrounding transaction exists.
             *
             * Persist immediately.
             */
            securityAuditService.record(
                    command.userId(),
                    command.eventType(),
                    command.outcome(),
                    command.ipAddress(),
                    command.userAgent(),
                    command.correlationId(),
                    command.details()
            );

            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                securityAuditService.record(
                                        command.userId(),
                                        command.eventType(),
                                        command.outcome(),
                                        command.ipAddress(),
                                        command.userAgent(),
                                        command.correlationId(),
                                        command.details()
                                );
                            }
                        }
                );
    }
}