package com.example.backend.module.messagemanagement.domain;

import com.example.backend.common.enums.ConversationStatus;
import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Tabla: conversations
 *
 * Guarda el estado actual de la conversación entre dos usuarios.
 * Desnormaliza username + avatar de ambos participantes para evitar
 * JOIN en el listado de conversaciones (critical path de rendimiento).
 *
 * Regla de negocio: siempre user1Id < user2Id → garantiza unicidad con 1 fila.
 *
 * Índices:
 *   idx_conv_user1   → listado de conversaciones del usuario 1
 *   idx_conv_user2   → listado de conversaciones del usuario 2
 *   idx_conv_updated → orden cronológico (último mensaje primero)
 */
@Entity
@Table(
        name = "conversations",
        indexes = {
                @Index(name = "idx_conv_user1",   columnList = "user1_id"),
                @Index(name = "idx_conv_user2",   columnList = "user2_id"),
                @Index(name = "idx_conv_updated", columnList = "last_message_at DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conv_participants", columnNames = {"user1_id", "user2_id"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Participantes ───────────────────────────────────────────────────────

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    // ─── Desnormalización: evita JOIN en el listado de conversaciones ─────────

    @Column(name = "user1_username", nullable = false, length = 50)
    private String user1Username;

    @Column(name = "user2_username", nullable = false, length = 50)
    private String user2Username;

    @Column(name = "user1_avatar", length = 500)
    private String user1Avatar;

    @Column(name = "user2_avatar", length = 500)
    private String user2Avatar;

    // ─── Último mensaje (desnormalizado → sin JOIN con messages) ─────────────

    /** Texto recortado a 50 chars. El frontend muestra solo 15 visualmente */
    @Column(name = "last_message_preview", length = 50)
    private String lastMessagePreview;

    /** de quien envió el último mensaje (para mostrar "Tú: ..." en frontend) */
    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // ─── Estado del otro participante ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.OFFLINE;

    // ─── Contadores de no leídos ──────────────────────────────────────────────

    @Column(name = "unread_count_user1", nullable = false)
    @Builder.Default
    private Integer unreadCountUser1 = 0;

    @Column(name = "unread_count_user2", nullable = false)
    @Builder.Default
    private Integer unreadCountUser2 = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}