package com.example.backend.common.security.ratelimit;

import java.time.Duration;

/**
 * Rate limiter de ventana fija por clave arbitraria (ej. "login:203.0.113.5",
 * "otp:usuario@correo.com"). Implementaciones: InMemoryRateLimiter
 * (default, por instancia) y RedisRateLimiter (compartido entre
 * instancias — ver app.presence.store).
 */
public interface IRateLimiter {

    /**
     * Registra un intento bajo `key` y devuelve true si sigue dentro del
     * límite (`maxAttempts` intentos por `window`), o false si ya se superó
     * — en cuyo caso el intento igual queda contado, para que el atacante no
     * pueda "resetear" el contador reintentando.
     */
    boolean tryConsume(String key, int maxAttempts, Duration window);
}
