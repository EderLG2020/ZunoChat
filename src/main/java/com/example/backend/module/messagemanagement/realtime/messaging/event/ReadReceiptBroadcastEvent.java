package com.example.backend.module.messagemanagement.realtime.messaging.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ReadReceiptBroadcastEvent(
        Long conversationId,
        Long readByUserId,
        String readByUsername,
        int messagesMarked,
        LocalDateTime readAt
) implements Serializable {}