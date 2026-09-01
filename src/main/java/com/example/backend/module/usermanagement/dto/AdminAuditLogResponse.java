package com.example.backend.module.usermanagement.dto;

import com.example.backend.common.enums.AdminAuditAction;
import com.example.backend.module.usermanagement.domain.AdminAuditLogModel;

import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long actorId,
        String actorUsername,
        Long targetId,
        String targetUsername,
        AdminAuditAction action,
        String details,
        LocalDateTime createdAt
) {
    public static AdminAuditLogResponse from(AdminAuditLogModel m) {
        return new AdminAuditLogResponse(
                m.getId(), m.getActorId(), m.getActorUsername(),
                m.getTargetId(), m.getTargetUsername(),
                m.getAction(), m.getDetails(), m.getCreatedAt());
    }
}
