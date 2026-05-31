package com.example.backend.module.messagemanagement.realtime.presence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class PresenceService implements IPresenceService {

    private static final String PREFIX_PRESENCE = "presence:";
    private static final String PREFIX_LASTSEEN = "presence:lastseen:";
    private static final String PREFIX_TYPING   = "typing:";
    private static final Duration PRESENCE_TTL  = Duration.ofSeconds(65);
    private static final Duration TYPING_TTL    = Duration.ofSeconds(5);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void markOnline(Long userId) {
        redisTemplate.opsForValue().set(PREFIX_PRESENCE + userId, "online", PRESENCE_TTL);
        log.debug("Presencia: user {} → ONLINE", userId);
    }

    public void heartbeat(Long userId) {
        redisTemplate.expire(PREFIX_PRESENCE + userId, PRESENCE_TTL);
    }

    public void markOffline(Long userId) {
        redisTemplate.delete(PREFIX_PRESENCE + userId);
        redisTemplate.opsForValue().set(PREFIX_LASTSEEN + userId,
                String.valueOf(Instant.now().toEpochMilli()));
        log.debug("Presencia: user {} → OFFLINE", userId);
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX_PRESENCE + userId));
    }

    public String getLastSeen(Long userId) {
        Object val = redisTemplate.opsForValue().get(PREFIX_LASTSEEN + userId);
        return val != null ? val.toString() : null;
    }

    public void setTyping(Long conversationId, Long userId) {
        redisTemplate.opsForValue().set(PREFIX_TYPING + conversationId + ":" + userId, "1", TYPING_TTL);
    }

    public void clearTyping(Long conversationId, Long userId) {
        redisTemplate.delete(PREFIX_TYPING + conversationId + ":" + userId);
    }

    public boolean isTyping(Long conversationId, Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX_TYPING + conversationId + ":" + userId));
    }
}