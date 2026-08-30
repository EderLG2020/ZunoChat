package com.example.backend.module.streak.dto.ws;

import com.example.backend.module.streak.domain.StreakStatus;

/**
 * Evento de racha. Broadcast a /topic/streak.{conversationId}
 * eventType: REQUEST_SENT | REQUEST_ACCEPTED | REQUEST_DECLINED | INCREMENTED | RESET | AT_RISK | BROKEN | DISABLED
 */
public record StreakEvent(
        String eventType,
        Long conversationId,
        int currentCount,
        int longestCount,
        StreakStatus status,
        Long requestedByUserId
) {}
