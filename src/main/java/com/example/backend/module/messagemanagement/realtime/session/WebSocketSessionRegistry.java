package com.example.backend.module.messagemanagement.realtime.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class WebSocketSessionRegistry implements IWebSocketSessionRegistry {

    private static final String PREFIX_SESSIONS        = "ws:sessions:";
    private static final String PREFIX_USER_BY_SESSION = "ws:user:";
    private static final Duration SESSION_TTL          = Duration.ofHours(24);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void registerSession(Long userId, String sessionId) {
        redisTemplate.opsForSet().add(PREFIX_SESSIONS + userId, sessionId);
        redisTemplate.expire(PREFIX_SESSIONS + userId, SESSION_TTL);
        redisTemplate.opsForValue().set(PREFIX_USER_BY_SESSION + sessionId,
                userId.toString(), SESSION_TTL);
        log.debug("WS Session registrada: userId={} sessionId={}", userId, sessionId);
    }

    public void removeSession(Long userId, String sessionId) {
        redisTemplate.opsForSet().remove(PREFIX_SESSIONS + userId, sessionId);
        redisTemplate.delete(PREFIX_USER_BY_SESSION + sessionId);
        log.debug("WS Session eliminada: userId={} sessionId={}", userId, sessionId);
    }

    public boolean hasActiveSessions(Long userId) {
        Set<Object> sessions = redisTemplate.opsForSet().members(PREFIX_SESSIONS + userId);
        return sessions != null && !sessions.isEmpty();
    }

    public long countSessions(Long userId) {
        Long count = redisTemplate.opsForSet().size(PREFIX_SESSIONS + userId);
        return count != null ? count : 0;
    }

    public Long getUserIdBySession(String sessionId) {
        Object val = redisTemplate.opsForValue().get(PREFIX_USER_BY_SESSION + sessionId);
        if (val == null) return null;
        try { return Long.parseLong(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}