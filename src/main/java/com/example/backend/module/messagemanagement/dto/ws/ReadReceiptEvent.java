package com.example.backend.module.messagemanagement.dto.ws;

import java.time.LocalDateTime;

/**
 * Evento de lectura de mensajes.
 * Publicado en /app/chat.read  →  broadcast a /topic/read.{conversationId}
 */
public record ReadReceiptEvent(
        Long conversationId,
        Long readByUserId,
        String readByUsername,
        LocalDateTime readAt
) {}