package com.example.backend.module.usermanagement.domain;

import com.example.backend.common.enums.AdminAuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tabla: admin_audit_log
 *
 * Registro inmutable de acciones de moderación (ban/activar/eliminar/cambiar
 * rol). Antes solo quedaba `UserModel.updatedBy` con el id del último actor,
 * pisado en cada nueva acción — sin esto no había forma de responder
 * "quién baneó a X y cuándo" si hubo más de un cambio de estado después.
 *
 * Desnormaliza los usernames de actor y target (mismo criterio que
 * ConversationModel/GroupMemberModel) para poder listar el log sin JOIN,
 * incluso si el usuario target termina eliminado (soft-delete).
 */
@Entity
@Table(
        name = "admin_audit_log",
        indexes = {
                @Index(name = "idx_audit_log_target", columnList = "target_id"),
                @Index(name = "idx_audit_log_actor",  columnList = "actor_id"),
                @Index(name = "idx_audit_log_created", columnList = "created_at DESC")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAuditLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_username", nullable = false, length = 50)
    private String targetUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAuditAction action;

    /** Motivo (ban) o detalle del cambio (ej. "USER → ADMIN"). Puede ser null. */
    @Column(length = 500)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
