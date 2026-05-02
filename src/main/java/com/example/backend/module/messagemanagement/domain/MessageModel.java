package com.example.backend.module.messagemanagement.domain;


import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tabla: messages
 *
 * Diseñada para consultas rápidas sin JOIN: almacena el conversation_id
 * como FK indexada y NO hace JOIN con users para mostrar mensajes.
 *
 * Soporta:
 *  - Texto plano (type = TEXT)
 *  - Payload estructurado JSON (type = PAYLOAD, payload_type = SALES | SYSTEM | ...)
 *  - Archivos adjuntos hasta 3 archivos de 5 MB c/u (urls en JSON)
 *  - Imágenes
 *
 * Índices:
 *   idx_msg_conversation  → listar mensajes de una conversación (ORDER BY sent_at DESC)
 *   idx_msg_sender        → historial de mensajes por emisor
 *   idx_msg_status        → filtrar por estado (para marcar como visto en lote)
 */
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_msg_conversation", columnList = "conversation_id, sent_at DESC"),
                @Index(name = "idx_msg_sender",       columnList = "sender_id"),
                @Index(name = "idx_msg_status",       columnList = "status")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Relación con conversación ────────────────────────────────────────────

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    // ─── Participantes (desnormalizado → sin JOIN) ────────────────────────────

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    // ─── Contenido ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type;

    /** Texto del mensaje (requerido si type = TEXT o IMAGE) */
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    /**
     * Payload estructurado en JSON.
     * Solo presente si type = PAYLOAD.
     * Ejemplo: {"title":"Oferta","price":99.9,"action":"BUY"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Object payload;

    /** Clasificación del payload (SALES, SYSTEM, SURVEY, CARD) */
    @Enumerated(EnumType.STRING)
    @Column(name = "payload_type", length = 20)
    private PayloadType payloadType;

    /**
     * URL de archivos adjuntos (máx. 3, 5 MB c/u).
     * Almacenado como JSON array: ["https://cdn.../file1.pdf", ...]
     * La validación de tamaño/cantidad se hace en el servicio.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_urls", columnDefinition = "jsonb")
    private List<String> fileUrls;

    // ─── Estado de lectura ────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ─── Auditoría ────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
