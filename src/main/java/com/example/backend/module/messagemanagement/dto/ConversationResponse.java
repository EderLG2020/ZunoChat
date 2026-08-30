package com.example.backend.module.messagemanagement.dto;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.enums.ConversationType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de conversación para el listado.
 * DIRECT: el frontend recibe con quién hablo, el último mensaje y el estado.
 * GROUP: en vez de otherUser*, trae groupName/groupAvatar y la lista de miembros.
 * No hay JOIN: todos los campos vienen de conversations (+ group_members si es GROUP).
 */
public record ConversationResponse(
        Long   conversationId,

        ConversationType type,

        /** Solo DIRECT — null en conversaciones GROUP */
        Long   otherUserId,
        String otherUsername,
        String otherAvatar,

        /** Solo GROUP — null en conversaciones DIRECT */
        String groupName,
        String groupAvatar,
        List<GroupMemberResponse> members,

        /** Preview del último mensaje (máx. 50 chars almacenados, frontend recorta a 15) */
        String lastMessagePreview,

        /** true si el último mensaje lo envió el usuario autenticado → frontend muestra "Tú: ..." */
        boolean lastMessageIsMine,

        /** Fecha/hora del último mensaje (el frontend formatea como "hace X min" si es hoy) */
        LocalDateTime lastMessageAt,

        /** Estado del otro participante — sin uso real en GROUP (queda OFFLINE) */
        ConversationStatus status,

        /** Cantidad de mensajes no leídos para el usuario autenticado */
        int unreadCount,

        /** true si el usuario autenticado silenció esta conversación (no la del otro lado / los otros miembros) */
        boolean muted,

        /** Chat temporal — compartido, no por lado (ver ConversationModel#ephemeralEnabled) */
        boolean ephemeralEnabled
) {}