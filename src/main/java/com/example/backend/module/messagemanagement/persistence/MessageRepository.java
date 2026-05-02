package com.example.backend.module.messagemanagement.persistence;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.module.messagemanagement.domain.MessageModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<MessageModel, Long> {

    /**
     * Listo mensaje de una conversación en orden inverso (más reciente primero).
     * Solo usa conversation_id + índice → sin JOIN.
     */
    @Query("""
        SELECT m FROM MessageModel m
        WHERE m.conversationId = :conversationId
        ORDER BY m.sentAt DESC
        """)
    Page<MessageModel> findByConversationId(
            @Param("conversationId") Long conversationId,
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
          AND m.status <> :excludeStatus
        """)
    int markAsRead(
            @Param("conversationId") Long conversationId,
            @Param("receiverId")     Long receiverId,
            @Param("status") MessageStatus status
    );
}