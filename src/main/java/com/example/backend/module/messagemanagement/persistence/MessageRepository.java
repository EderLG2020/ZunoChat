package com.example.backend.module.messagemanagement.persistence;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.module.messagemanagement.domain.MessageModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageModel, Long> {

    /** Para el replay idempotente de POST /api/messages — ver MessageService#sendMessage. */
    Optional<MessageModel> findByClientMessageId(String clientMessageId);

    /**
     * Lista mensajes de una conversación en orden inverso (más reciente primero),
     * paginado por cursor (keyset) en vez de OFFSET.
     *
     * beforeId = null  → los `size` mensajes más recientes.
     * beforeId != null → los `size` mensajes anteriores a ese id (scroll hacia el pasado).
     *
     * Usa el índice idx_msg_conversation (conversation_id, id) → sin OFFSET,
     * sin COUNT(*) y con costo constante por página sin importar el historial acumulado.
     */
    @Query("""
        SELECT m FROM MessageModel m
        WHERE m.conversationId = :conversationId
          AND (:beforeId IS NULL OR m.id < :beforeId)
        ORDER BY m.id DESC
        """)
    List<MessageModel> findPageByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );

    /**
     * Marca como READ todos los mensajes enviados al receiver en esta conversación.
     * Se usa al abrir el chat → bulk update sin cargar entidades en memoria.
     */
    @Modifying
    @Query("""
        UPDATE MessageModel m
        SET m.status = :status, m.readAt = CURRENT_TIMESTAMP
        WHERE m.conversationId = :conversationId
          AND m.receiverId     = :receiverId
          AND m.status <> :status
        """)
    int markAsRead(
            @Param("conversationId") Long conversationId,
            @Param("receiverId")     Long receiverId,
            @Param("status") MessageStatus status
    );

    /**
     * Mensajes de "chat temporal" ya vencidos y todavía no borrados —
     * ver EphemeralMessageSweeper. Se cargan como entidades (no bulk UPDATE)
     * porque cada uno necesita disparar su propio evento MESSAGE_UPDATED por WS.
     */
    @Query("""
        SELECT m FROM MessageModel m
        WHERE m.expiresAt IS NOT NULL
          AND m.expiresAt <= :now
          AND m.deleted = false
        """)
    List<MessageModel> findExpiredNotDeleted(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Solo para DataSeeder: sentAt es @CreationTimestamp/updatable=false, así
     * que Hibernate lo ignora en cualquier UPDATE generado desde la entidad.
     * Este UPDATE nativo lo esquiva para poder simular un historial con
     * fechas repartidas en el pasado en vez de que todos los mensajes fake
     * queden con el mismo instante de inserción.
     */
    @Modifying
    @Query(value = "UPDATE messages SET sent_at = :sentAt WHERE id = :id", nativeQuery = true)
    void backdateSentAt(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);
}