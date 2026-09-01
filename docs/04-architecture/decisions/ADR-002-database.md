# ADR-002 — Base de datos relacional

**Contexto:** El sistema maneja usuarios, conversaciones y mensajes con relaciones entre entidades y necesidad de consistencia transaccional.

**Problema:** Elegir el motor de base de datos principal.

**Alternativas:**
- MySQL — muy extendido, menor soporte a JSONB nativo
- MongoDB — sin esquema, escalado horizontal, sin JOINs ni transacciones ACID fuertes
- PostgreSQL — relacional, ACID, soporte nativo a `JSONB`, maduro y open source

**Decisión:** PostgreSQL 16.

**Consecuencias:**
- Los campos `payload` y `file_urls` en `messages` se almacenan como `JSONB` sin necesidad de una colección separada
- Esquema estricto con constraints (`UNIQUE`, `NOT NULL`, `ENUM`) aplicados a nivel de DB
- Mayor complejidad de operación vs. MySQL en configuraciones avanzadas
