package com.example.backend.module.messagemanagement.dto;

import java.util.List;

/**
 * Página de mensajes paginada por cursor.
 * nextCursor es el id a pasar como beforeId para pedir la siguiente página
 * (mensajes más antiguos); null cuando no hay más.
 */
public record MessageCursorPage(
        List<MessageResponse> content,
        boolean hasMore,
        Long nextCursor
) {}
