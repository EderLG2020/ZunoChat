package com.example.backend.module.messagemanagement.realtime.messaging.event;

import java.io.Serializable;

public record PresenceBroadcastEvent(
        Long userId,
        String username,
        boolean online,
        String lastSeen
) implements Serializable {}