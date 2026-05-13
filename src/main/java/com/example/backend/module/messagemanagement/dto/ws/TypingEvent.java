package com.example.backend.module.messagemanagement.dto.ws;

/**
 * Evento de "usuario escribiendo".
 * Publicado en /app/chat.typing  →  broadcast a /topic/typing.{conversationId}
 */
public record TypingEvent(
        Long conversationId,
        Long userId,
        String username,
        boolean typing            // true = escribiendo, false = dejó de escribir
) {}