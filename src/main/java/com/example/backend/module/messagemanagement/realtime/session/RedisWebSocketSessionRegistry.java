package com.example.backend.module.messagemanagement.realtime.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementación de IWebSocketSessionRegistry respaldada en Redis — permite
 * que hasActiveSessions(userId) sea correcto entre instancias: si el usuario
 * tiene una pestaña/dispositivo conectado a la instancia A y otro a la
 * instancia B, ambas ven que sigue "con sesión activa" al desconectar una.
 *
 * Se activa con: app.presence.store=redis
 *
 * Límite conocido y aceptado: si una instancia muere sin disparar el evento
 * de desconexión (crash, kill -9), sus sesiones quedan huérfanas en Redis
 * hasta que markOffline se llame por otra vía. Mismo trade-off que ya asume
 * el resto del sistema de presencia (ver TTLs en RedisPresenceService).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "redis")
public class RedisWebSocketSessionRegistry implements IWebSocketSessionRegistry {

    private final StringRedisTemplate redis;

    @Override
    public void registerSession(Long userId, String sessionId) {
        redis.opsForSet().add(sessionsKey(userId), sessionId);
        redis.opsForValue().set(ownerKey(sessionId), String.valueOf(userId));
        log.debug("WS Session registrada (redis): userId={} sessionId={}", userId, sessionId);
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        redis.opsForSet().remove(sessionsKey(userId), sessionId);
        redis.delete(ownerKey(sessionId));
        log.debug("WS Session eliminada (redis): userId={} sessionId={}", userId, sessionId);
    }

    @Override
    public boolean hasActiveSessions(Long userId) {
        Long size = redis.opsForSet().size(sessionsKey(userId));
        return size != null && size > 0;
    }

    @Override
    public long countSessions(Long userId) {
        Long size = redis.opsForSet().size(sessionsKey(userId));
        return size != null ? size : 0;
    }

    @Override
    public Long getUserIdBySession(String sessionId) {
        String userId = redis.opsForValue().get(ownerKey(sessionId));
        return userId != null ? Long.valueOf(userId) : null;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String sessionsKey(Long userId) {
        return "presence:sessions:" + userId;
    }

    private String ownerKey(String sessionId) {
        return "presence:session-owner:" + sessionId;
    }
}
