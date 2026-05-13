package com.example.backend.module.messagemanagement.realtime.presence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Servicio de presencia online/offline usando Redis.
 *
 * Keys utilizadas:
 *   presence:{userId}          → "online"    TTL: 65s (se renueva con heartbeat c/60s)
 *   presence:lastseen:{userId} → epoch ms    Sin TTL
 *   typing:{convId}:{userId}   → "1"         TTL: 5s (auto-expira si el cliente deja de enviar)
 */
@Slf4j
@Service
public class PresenceService {

    private static final String PREFIX_PRESENCE  = "presence:";
    private static final String PREFIX_LASTSEEN  = "presence:lastseen:";
    private static final String PREFIX_TYPING    = "typing:";
    private static final Duration PRESENCE_TTL   = Duration.ofSeconds(65);
    private static final Duration TYPING_TTL     = Duration.ofSeconds(5);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ─── Online ──────────────────────────────────────────────────────────────

    public void markOnline(Long userId) {
        String key = PREFIX_PRESENCE + userId;
        redisTemplate.opsForValue().set(key, "online", PRESENCE_TTL);
        log.debug("Presencia: user {} → ONLINE", userId);
    }

    /**
     * Llamado en heartbeat desde el cliente (cada 60s).
     * Renueva TTL para evitar expiración falsa.
     */
    public void heartbeat(Long userId) {
        String key = PREFIX_PRESENCE + userId;
        redisTemplate.expire(key, PRESENCE_TTL);
    }

    public void markOffline(Long userId) {
        String presenceKey = PREFIX_PRESENCE + userId;
        String lastSeenKey = PREFIX_LASTSEEN + userId;

        redisTemplate.delete(presenceKey);
        redisTemplate.opsForValue().set(lastSeenKey, String.valueOf(Instant.now().toEpochMilli()));
        log.debug("Presencia: user {} → OFFLINE", userId);
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX_PRESENCE + userId));
    }

    public String getLastSeen(Long userId) {
        Object val = redisTemplate.opsForValue().get(PREFIX_LASTSEEN + userId);
        return val != null ? val.toString() : null;
    }

    // ─── Typing ──────────────────────────────────────────────────────────────

    public void setTyping(Long conversationId, Long userId) {
        String key = PREFIX_TYPING + conversationId + ":" + userId;
        redisTemplate.opsForValue().set(key, "1", TYPING_TTL);
    }

    public void clearTyping(Long conversationId, Long userId) {
        redisTemplate.delete(PREFIX_TYPING + conversationId + ":" + userId);
    }

    public boolean isTyping(Long conversationId, Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX_TYPING + conversationId + ":" + userId));
    }
}