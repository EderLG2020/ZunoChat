package com.example.backend.module.messagemanagement.realtime.presence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria de IPresenceService.
 * Estado local a la instancia — pensado para un nodo único.
 * Activa por defecto (app.presence.store=memory o sin definir).
 * Para varias instancias del backend, ver RedisPresenceService.
 *
 * El método evictExpired() limpia entradas expiradas cada 60 segundos
 * para evitar crecimiento ilimitado de los mapas.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "memory", matchIfMissing = true)
public class InMemoryPresenceService implements IPresenceService {

    // userId → expireEpochMs
    private final Map<Long, Long>   onlineUsers = new ConcurrentHashMap<>();
    // userId → epochMs (sin TTL — persiste hasta nueva sesión)
    private final Map<Long, Long>   lastSeen    = new ConcurrentHashMap<>();
    // "convId:userId" → expireEpochMs
    private final Map<String, Long> typing      = new ConcurrentHashMap<>();

    private static final long PRESENCE_TTL_MS = 65_000;
    private static final long TYPING_TTL_MS   =  5_000;

    // ─── Online ──────────────────────────────────────────────────────────────

    @Override
    public void markOnline(Long userId) {
        onlineUsers.put(userId, now() + PRESENCE_TTL_MS);
        log.debug("Presencia (memory): user {} → ONLINE", userId);
    }

    @Override
    public void heartbeat(Long userId) {
        // computeIfPresent: solo renueva si la clave ya existe (usuario realmente conectado)
        onlineUsers.computeIfPresent(userId, (k, v) -> now() + PRESENCE_TTL_MS);
    }

    @Override
    public void markOffline(Long userId) {
        onlineUsers.remove(userId);
        lastSeen.put(userId, now());
        log.debug("Presencia (memory): user {} → OFFLINE", userId);
    }

    @Override
    public boolean isOnline(Long userId) {
        Long exp = onlineUsers.get(userId);
        if (exp == null) return false;
        if (now() > exp) {
            onlineUsers.remove(userId);
            return false;
        }
        return true;
    }

    @Override
    public String getLastSeen(Long userId) {
        Long ts = lastSeen.get(userId);
        return ts != null ? ts.toString() : null;
    }

    // ─── Typing ──────────────────────────────────────────────────────────────

    @Override
    public void setTyping(Long conversationId, Long userId) {
        typing.put(typingKey(conversationId, userId), now() + TYPING_TTL_MS);
    }

    @Override
    public void clearTyping(Long conversationId, Long userId) {
        typing.remove(typingKey(conversationId, userId));
    }

    @Override
    public boolean isTyping(Long conversationId, Long userId) {
        Long exp = typing.get(typingKey(conversationId, userId));
        if (exp == null) return false;
        if (now() > exp) {
            typing.remove(typingKey(conversationId, userId));
            return false;
        }
        return true;
    }

    // ─── Limpieza periódica ───────────────────────────────────────────────────

    /**
     * Elimina entradas expiradas de los mapas cada 60 segundos.
     * Evita que onlineUsers y typing crezcan sin límite en servidores de larga vida.
     * lastSeen no se limpia — es útil conservarlo indefinidamente.
     */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        long now = now();
        int removedOnline = 0;
        int removedTyping = 0;

        removedOnline = removeExpiredFrom(onlineUsers, now);
        removedTyping = removeExpiredFrom(typing, now);

        if (removedOnline > 0 || removedTyping > 0) {
            log.debug("evictExpired: eliminados {} online, {} typing", removedOnline, removedTyping);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private <K> int removeExpiredFrom(Map<K, Long> map, long now) {
        int[] count = {0};
        map.entrySet().removeIf(e -> {
            if (now > e.getValue()) { count[0]++; return true; }
            return false;
        });
        return count[0];
    }

    private String typingKey(Long convId, Long userId) {
        return convId + ":" + userId;
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }
}
