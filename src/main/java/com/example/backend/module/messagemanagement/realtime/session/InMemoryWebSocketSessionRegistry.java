package com.example.backend.module.messagemanagement.realtime.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de sesiones WebSocket en memoria.
 * Estado local a la instancia — pensado para un nodo único.
 * Activo por defecto (app.presence.store=memory o sin definir).
 * Para varias instancias del backend, ver RedisWebSocketSessionRegistry.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "memory", matchIfMissing = true)
public class InMemoryWebSocketSessionRegistry implements IWebSocketSessionRegistry {

    // userId → Set<sessionId>
    private final Map<Long, Set<String>> userSessions    = new ConcurrentHashMap<>();
    // sessionId → userId
    private final Map<String, Long>      sessionToUser   = new ConcurrentHashMap<>();

    public void registerSession(Long userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToUser.put(sessionId, userId);
        log.debug("WS Session registrada (memory): userId={} sessionId={}", userId, sessionId);
    }

    public void removeSession(Long userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) userSessions.remove(userId);
        }
        sessionToUser.remove(sessionId);
        log.debug("WS Session eliminada (memory): userId={} sessionId={}", userId, sessionId);
    }

    public boolean hasActiveSessions(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public long countSessions(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    public Long getUserIdBySession(String sessionId) {
        return sessionToUser.get(sessionId);
    }
}
