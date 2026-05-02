package com.example.backend.module.messagemanagement.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarkReadRequest(
        @NotNull @Positive Long conversationId
) {}
