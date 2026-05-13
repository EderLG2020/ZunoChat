package com.example.backend.module.messagemanagement.dto.ws;

import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mensaje entrante desde el cliente WebSocket (STOMP payload).
 * Se publica en /app/chat.send
 */
public record WsInboundMessage(

        @NotNull
        @Positive
        Long conversationId,

        @NotNull
        MessageType type,

        String textContent,

        Object payload,

        PayloadType payloadType,

        @Size(max = 3)
        List<String> fileUrls
) {}