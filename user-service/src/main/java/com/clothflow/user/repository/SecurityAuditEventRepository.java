package com.clothflow.user.repository;

import com.clothflow.user.entity.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityAuditEventRepository
        extends JpaRepository<SecurityAuditEvent, UUID> {
}