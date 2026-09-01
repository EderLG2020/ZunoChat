package com.example.backend.module.usermanagement.application;

import com.example.backend.common.enums.AdminAuditAction;
import com.example.backend.module.usermanagement.domain.AdminAuditLogModel;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.AdminAuditLogResponse;
import com.example.backend.module.usermanagement.persistence.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de auditoría de acciones de moderación — separado de AdminService
 * para que ban/activate/delete/assignRole no carguen con el detalle de cómo
 * se persiste el log, solo con el hecho de que deben dejarlo.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository auditLogRepository;

    /**
     * Se llama DESPUÉS de que la acción de moderación ya se aplicó y
     * persistió — misma transacción que AdminService (mismo @Transactional
     * del método invocador), así que si el UPDATE de UserModel falla, el
     * log tampoco se escribe (todo o nada).
     */
    @Transactional
    public void log(UserModel actor, UserModel target, AdminAuditAction action, String details) {
        auditLogRepository.save(AdminAuditLogModel.builder()
                .actorId(actor.getId())
                .actorUsername(actor.getUsername())
                .targetId(target.getId())
                .targetUsername(target.getUsername())
                .action(action)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> search(Long targetId, Long actorId, Pageable pageable) {
        return auditLogRepository.search(targetId, actorId, pageable).map(AdminAuditLogResponse::from);
    }
}
