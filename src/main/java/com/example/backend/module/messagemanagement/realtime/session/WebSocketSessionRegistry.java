package com.example.backend.module.messagemanagement.realtime.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Registro de sesiones WebSocket activas en Redis.
 *
 * Permite conocer cuántas sesiones tiene un usuario
 * (mismo usuario, múltiples tabs/dispositivos).
 *
 * Key:  ws:sessions:{userId}   → Set de sessionIds   TTL: 24h
 * Key:  ws:user:{sessionId}    → userId (reverse lookup)
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    private static final String PREFIX_SESSIONS = "ws:sessions:";
    private static final String PREFIX_USER_BY_SESSION = "ws:user:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void registerSession(Long userId, String sessionId) {
        String sessionsKey = PREFIX_SESSIONS + userId;
        String reverseKey  = PREFIX_USER_BY_SESSION + sessionId;

        redisTemplate.opsForSet().add(sessionsKey, sessionId);
        redisTemplate.expire(sessionsKey, SESSION_TTL);
        redisTemplate.opsForValue().set(reverseKey, userId.toString(), SESSION_TTL);

        log.debug("WS Session registrada: userId={} sessionId={}", userId, sessionId);
    }

    public void removeSession(Long userId, String sessionId) {
        String sessionsKey = PREFIX_SESSIONS + userId;
        String reverseKey  = PREFIX_USER_BY_SESSION + sessionId;

        redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        redisTemplate.delete(reverseKey);

        log.debug("WS Session eliminada: userId={} sessionId={}", userId, sessionId);
    }

    /** Retorna true si el usuario tiene al menos una sesión activa. */
    public boolean hasActiveSessions(Long userId) {
        Set<Object> sessions = redisTemplate.opsForSet().members(PREFIX_SESSIONS + userId);
        return sessions != null && !sessions.isEmpty();
    }

    /** Cantidad de sesiones activas del usuario (múltiples dispositivos). */
    public long countSessions(Long userId) {
        Long count = redisTemplate.opsForSet().size(PREFIX_SESSIONS + userId);
        return count != null ? count : 0;
    }

    public Long getUserIdBySession(String sessionId) {
        Object val = redisTemplate.opsForValue().get(PREFIX_USER_BY_SESSION + sessionId);
        if (val == null) return null;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}