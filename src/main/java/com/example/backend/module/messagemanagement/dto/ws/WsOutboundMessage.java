package com.example.backend.module.messagemanagement.dto.ws;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mensaje saliente hacia el cliente WebSocket.
 * Broadcast a /topic/conversation.{conversationId}
 */
public record WsOutboundMessage(

        String eventType,          // MESSAGE_RECEIVED
        Long messageId,
        Long conversationId,
        Long senderId,
        String senderUsername,
        Long receiverId,
        MessageType type,
        String textContent,
        Object payload,
        PayloadType payloadType,
        List<String> fileUrls,
        MessageStatus status,
        LocalDateTime sentAt
) {}