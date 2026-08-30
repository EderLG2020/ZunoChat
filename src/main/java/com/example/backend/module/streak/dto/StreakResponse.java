package com.example.backend.module.streak.dto;

import com.example.backend.module.streak.domain.StreakRequestStatus;
import com.example.backend.module.streak.domain.StreakStatus;

import java.time.LocalDate;

/**
 * Si la conversación nunca activó racha, se devuelve con enabled=false,
 * currentCount=0 y status=INACTIVE sin que exista fila en BD todavía.
 */
public record StreakResponse(
        Long conversationId,
        boolean enabled,
        int currentCount,
        int longestCount,
        LocalDate lastInteractionDate,
        StreakStatus status,
        StreakRequestStatus requestStatus,
        /** Quién envió la solicitud pendiente — null si no hay ninguna en curso. */
        Long requestedByUserId
) {}
