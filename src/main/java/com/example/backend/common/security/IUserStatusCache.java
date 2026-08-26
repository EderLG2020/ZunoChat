package com.example.backend.common.security;

import com.example.backend.common.enums.UserStatus;

/**
 * Cache acotado (30s) del UserStatus por username, para que JwtFilter pueda
 * rechazar a un usuario baneado/inactivo sin esperar a que expire su JWT
 * (hasta 24h) — sin pagar el costo de una consulta a BD en cada request.
 *
 * Implementaciones: InMemoryUserStatusCache (default, Guava, por instancia)
 * y RedisUserStatusCache (ver app.presence.store=redis) — necesaria en
 * multi-instancia: con la versión en memoria, invalidate() solo limpia el
 * cache de la instancia que atendió el ban/activate/delete, así que las
 * demás instancias podían seguir sirviendo el status viejo hasta por 30s.
 */
public interface IUserStatusCache {

    /**
     * Devuelve el status actual (posiblemente cacheado hasta 30s). Si la
     * consulta falla (ej. BD momentáneamente inalcanzable), falla "abierto"
     * — devuelve null y JwtFilter deja pasar el request — para que un
     * problema transitorio no tumbe la autenticación de todo el mundo.
     */
    UserStatus getStatus(String username);

    /** Invalida la entrada de un usuario — usar cuando se banea/reactiva/elimina desde el panel admin. */
    void invalidate(String username);
}
