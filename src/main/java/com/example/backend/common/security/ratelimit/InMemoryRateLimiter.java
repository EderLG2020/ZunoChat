package com.example.backend.common.security.ratelimit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter de ventana fija en memoria local (por instancia) — suficiente
 * para el caso de una sola instancia, que es el default del proyecto. En un
 * despliegue multi-instancia cada nodo cuenta sus propios intentos (un
 * atacante distribuido entre nodos vería el límite multiplicado por N); para
 * un límite realmente compartido, activar app.presence.store=redis (mismo
 * interruptor que ya usan presencia/sesiones WS/UserStatusCache — ver
 * RedisRateLimiter).
 *
 * Se usa un Cache de Guava (ya es dependencia del proyecto, igual que
 * UserStatusCache) solo para acotar memoria — expira claves inactivas por
 * más de 1h, un margen amplio sobre cualquier ventana real usada hoy
 * (minutos). La ventana de rate-limit en sí la controla `Window` manualmente,
 * no el TTL del cache.
 */
@Component
@ConditionalOnProperty(prefix = "app.presence", name = "store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements IRateLimiter {

    private static class Window {
        volatile long windowStartMillis = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final Cache<String, Window> windows = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    @Override
    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        Window w;
        try {
            w = windows.get(key, Window::new);
        } catch (Exception e) {
            return true; // fail-open: un fallo del cache no debe bloquear el login
        }

        synchronized (w) {
            long now = System.currentTimeMillis();
            if (now - w.windowStartMillis >= window.toMillis()) {
                w.windowStartMillis = now;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= maxAttempts;
        }
    }
}
