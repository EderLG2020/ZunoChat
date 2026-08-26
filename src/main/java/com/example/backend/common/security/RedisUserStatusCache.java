package com.example.backend.common.security;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Implementación de IUserStatusCache respaldada en Redis — a diferencia de
 * InMemoryUserStatusCache, invalidate() (llamado al banear/activar/eliminar
 * desde el panel admin) borra la clave compartida, así que TODAS las
 * instancias del backend dejan de servir el status viejo de inmediato, no
 * solo la que ejecutó la acción de moderación.
 *
 * Se activa con: app.presence.store=redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "redis")
public class RedisUserStatusCache implements IUserStatusCache {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;

    @Override
    public UserStatus getStatus(String username) {
        String key = key(username);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return UserStatus.valueOf(cached);
            }

            UserStatus status = userRepository.findByUsername(username)
                    .map(u -> u.getStatus())
                    .orElse(UserStatus.DELETED);
            redis.opsForValue().set(key, status.name(), TTL);
            return status;
        } catch (Exception e) {
            log.warn("[RedisUserStatusCache] No se pudo resolver el status de {}: {}", username, e.getMessage());
            return null;
        }
    }

    @Override
    public void invalidate(String username) {
        redis.delete(key(username));
    }

    private String key(String username) {
        return "userstatus:" + username;
    }
}
