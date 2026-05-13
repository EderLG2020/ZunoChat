package com.example.backend.module.messagemanagement.dto.ws;

/**
 * Evento de presencia online/offline.
 * Broadcast a /topic/presence.{userId}
 */
public record PresenceEvent(
        Long userId,
        String username,
        boolean online,
        String lastSeen           // ISO-8601 cuando offline
) {}