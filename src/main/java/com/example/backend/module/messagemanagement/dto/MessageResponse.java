package com.example.backend.module.messagemanagement.dto;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        Long messageId,
        Long conversationId,
        Long senderId,
        Long receiverId,
        MessageType type,
        String textContent,
        Object payload,
        PayloadType payloadType,
        List<String> fileUrls,
        MessageStatus status,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        boolean deleted,
        LocalDateTime editedAt
) {}
