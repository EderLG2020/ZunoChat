# ADR-005 — Presencia y estado de escritura

**Contexto:** El sistema necesita saber qué usuarios están conectados y si están escribiendo, con expiración automática si la conexión se cae sin evento de desconexión limpia.

**Problema:** Dónde almacenar estado efímero (online/offline/typing) que expira por tiempo.

**Alternativas:**
- Base de datos relacional — sin TTL nativo, costoso en writes frecuentes
- Memoria de la JVM — no sobrevive reinicios, no escala a múltiples instancias
- Redis con TTL — expiración automática, operaciones O(1), diseñado para este caso

**Decisión:** Redis 7.2 con TTL, activable mediante flag `redis.enabled`.

**Consecuencias:**
- `presence:{userId}` expira a los 65 s si no llega heartbeat, garantizando consistencia sin evento de desconexión explícito
- `typing:{convId}:{userId}` expira a los 5 s automáticamente, sin necesidad de evento `typing=false`
- En dev (`redis.enabled=false`) se usa `InMemoryPresenceService` para no requerir Redis local
- La interfaz `IPresenceService` permite el cambio transparente entre implementaciones
