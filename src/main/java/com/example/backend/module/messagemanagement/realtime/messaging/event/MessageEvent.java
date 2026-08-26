package com.example.backend.module.messagemanagement.realtime.messaging.event;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de mensaje para broadcast por WebSocket.
 * Transporta la información completa del mensaje.
 */
public record MessageEvent(
        Long messageId,
        Long conversationId,
        Long senderId,
        String senderUsername,
        Long receiverId,
        String receiverUsername,
        MessageType type,
        String textContent,
        Object payload,
        PayloadType payloadType,
        List<String> fileUrls,
        MessageStatus status,
        LocalDateTime sentAt,
        boolean deleted,
        LocalDateTime editedAt
) implements Serializable {}