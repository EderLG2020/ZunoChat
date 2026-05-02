package com.example.backend.module.messagemanagement.dto;

import com.example.backend.common.enums.ConversationStatus;

import java.time.LocalDateTime;

/**
 * Respuesta de conversación para el listado.
 * El frontend recibe: con quién hablo, el último mensaje y el estado.
 * No hay JOIN: todos los campos vienen de la tabla conversations.
 */
public record ConversationResponse(
        Long   conversationId,

        /** ID y datos del "otro" participante (relativo al usuario autenticado) */
        Long   otherUserId,
        String otherUsername,
        String otherAvatar,

        /** Preview del último mensaje (máx. 50 chars almacenados, frontend recorta a 15) */
        String lastMessagePreview,

        /** true si el último mensaje lo envió el usuario autenticado → frontend muestra "Tú: ..." */
        boolean lastMessageIsMine,

        /** Fecha/hora del último mensaje (el frontend formatea como "hace X min" si es hoy) */
        LocalDateTime lastMessageAt,

        /** Estado del otro participante */
        ConversationStatus status,

        /** Cantidad de mensajes no leídos para el usuario autenticado */
        int unreadCount
) {}