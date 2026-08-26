package com.example.backend.module.messagemanagement.realtime.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementación de IPresenceService respaldada en Redis — misma semántica
 * que InMemoryPresenceService (TTL de 65s online, 5s typing) pero compartida
 * entre todas las instancias del backend, así "¿está online?" es correcto
 * sin importar a qué instancia esté conectado el usuario.
 *
 * Se activa con: app.presence.store=redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "redis")
public class RedisPresenceService implements IPresenceService {

    private static final Duration PRESENCE_TTL = Duration.ofSeconds(65);
    private static final Duration TYPING_TTL   = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;

    // ─── Online ──────────────────────────────────────────────────────────────

    @Override
    public void markOnline(Long userId) {
        redis.opsForValue().set(onlineKey(userId), "1", PRESENCE_TTL);
        log.debug("Presencia (redis): user {} → ONLINE", userId);
    }

    @Override
    public void heartbeat(Long userId) {
        // Solo renueva el TTL si la clave ya existe (usuario realmente conectado)
        Boolean exists = redis.hasKey(onlineKey(userId));
        if (Boolean.TRUE.equals(exists)) {
            redis.expire(onlineKey(userId), PRESENCE_TTL);
        }
    }

    @Override
    public void markOffline(Long userId) {
        redis.delete(onlineKey(userId));
        redis.opsForValue().set(lastSeenKey(userId), String.valueOf(Instant.now().toEpochMilli()));
        log.debug("Presencia (redis): user {} → OFFLINE", userId);
    }

    @Override
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redis.hasKey(onlineKey(userId)));
    }

    @Override
    public String getLastSeen(Long userId) {
        return redis.opsForValue().get(lastSeenKey(userId));
    }

    // ─── Typing ──────────────────────────────────────────────────────────────

    @Override
    public void setTyping(Long conversationId, Long userId) {
        redis.opsForValue().set(typingKey(conversationId, userId), "1", TYPING_TTL);
    }

    @Override
    public void clearTyping(Long conversationId, Long userId) {
        redis.delete(typingKey(conversationId, userId));
    }

    @Override
    public boolean isTyping(Long conversationId, Long userId) {
        return Boolean.TRUE.equals(redis.hasKey(typingKey(conversationId, userId)));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String onlineKey(Long userId) {
        return "presence:online:" + userId;
    }

    private String lastSeenKey(Long userId) {
        return "presence:lastSeen:" + userId;
    }

    private String typingKey(Long convId, Long userId) {
        return "presence:typing:" + convId + ":" + userId;
    }
}
