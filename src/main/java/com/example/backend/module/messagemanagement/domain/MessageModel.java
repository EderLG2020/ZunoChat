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
 *   idx_msg_conversation    → listar mensajes de una conversación por cursor (ORDER BY id DESC)
 *   idx_msg_sender          → historial de mensajes por emisor
 *   idx_msg_status          → filtrar por estado (para marcar como visto en lote)
 *   idx_msg_conv_recv_status → markAsRead filtra por los tres campos a la vez
 *     (conversation_id + receiver_id + status <> READ); antes solo
 *     conversation_id estaba cubierto por un índice compuesto. Impacto bajo
 *     hoy (conversaciones 1:1 acotan mucho el scan), pero crece con el
 *     historial de cada conversación.
 */
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_msg_conversation",     columnList = "conversation_id, id DESC"),
                @Index(name = "idx_msg_sender",           columnList = "sender_id"),
                @Index(name = "idx_msg_status",           columnList = "status"),
                @Index(name = "idx_msg_conv_recv_status", columnList = "conversation_id, receiver_id, status")
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

    /** Null en mensajes de conversaciones GROUP — no hay un único receptor. */
    @Column(name = "receiver_id")
    private Long receiverId;

    /**
     * Id generado por el cliente (UUID/random string) para reintentos
     * idempotentes de POST /api/messages — si el cliente reintenta el mismo
     * envío (timeout de red, doble tap) con el mismo clientMessageId,
     * MessageService devuelve el mensaje ya creado en vez de duplicarlo.
     * Nullable: mensajes enviados antes de este campo, o sin id de cliente,
     * simplemente no participan en la deduplicación.
     */
    @Column(name = "client_message_id", unique = true, length = 64)
    private String clientMessageId;

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

    // ─── Edición / borrado ────────────────────────────────────────────────────

    /** Soft delete: el contenido se limpia y el cliente muestra un tombstone. */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /** No-null solo si el mensaje fue editado (distingue "editado" de "nunca tocado"). */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /**
     * No-null solo si se envió con la conversación en modo "chat temporal" —
     * EphemeralMessageSweeper lo barre y hace soft-delete pasada esa fecha.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ─── Auditoría ────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
