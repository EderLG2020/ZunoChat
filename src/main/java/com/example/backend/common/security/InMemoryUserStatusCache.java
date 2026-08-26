package com.example.backend.common.security;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementación en memoria de IUserStatusCache (Guava) — estado local a la
 * instancia. Correcta para un nodo único (el default del proyecto); en
 * multi-instancia, ver RedisUserStatusCache.
 *
 * Trade-off deliberado: hasta 30s de ventana en que un usuario recién
 * baneado puede seguir usando la API — aceptable para un chat, y muchísimo
 * más barato que consultar la BD en cada request autenticado.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "memory", matchIfMissing = true)
public class InMemoryUserStatusCache implements IUserStatusCache {

    private final LoadingCache<String, UserStatus> cache;

    public InMemoryUserStatusCache(UserRepository userRepository) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build(new CacheLoader<>() {
                    @Override
                    public UserStatus load(String username) {
                        return userRepository.findByUsername(username)
                                .map(u -> u.getStatus())
                                .orElse(UserStatus.DELETED);
                    }
                });
    }

    @Override
    public UserStatus getStatus(String username) {
        try {
            return cache.get(username);
        } catch (ExecutionException e) {
            log.warn("[InMemoryUserStatusCache] No se pudo resolver el status de {}: {}", username, e.getMessage());
            return null;
        }
    }

    @Override
    public void invalidate(String username) {
        cache.invalidate(username);
    }
}
