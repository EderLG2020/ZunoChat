package com.example.backend.common.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Rate limiter de ventana fija respaldado en Redis (INCR + EXPIRE) — el
 * límite es correcto entre todas las instancias del backend, no solo la que
 * atendió el request. Se activa junto con app.presence.store=redis.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "redis")
public class RedisRateLimiter implements IRateLimiter {

    private final StringRedisTemplate redis;

    @Override
    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        String redisKey = "ratelimit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count == null) return true; // fail-open ante un Redis momentáneamente inalcanzable

        // Solo el primer incremento de la ventana fija el TTL — los siguientes
        // reutilizan el mismo vencimiento, así la ventana no se extiende
        // indefinidamente con reintentos seguidos.
        if (count == 1L) {
            redis.expire(redisKey, window);
        }
        return count <= maxAttempts;
    }
}
