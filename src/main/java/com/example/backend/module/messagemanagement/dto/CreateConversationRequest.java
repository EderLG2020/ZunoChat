package com.example.backend.module.messagemanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateConversationRequest(
        @NotNull(message = "El ID del otro usuario es obligatorio")
        @Positive(message = "El ID debe ser positivo")
        Long targetUserId
) {}