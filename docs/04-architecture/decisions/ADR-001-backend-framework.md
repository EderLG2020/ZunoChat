# ADR-001 — Framework backend

**Contexto:** Se necesita un framework JVM para construir una API REST con WebSocket, seguridad, ORM y soporte a mensajería asíncrona.

**Problema:** Elegir el stack base del servidor.

**Alternativas:**
- Quarkus — menor footprint, arranque más rápido, ecosistema más pequeño
- Micronaut — compilación en tiempo de build, menos reflexión, curva alta
- Spring Boot — maduro, ecosistema amplio, integración nativa con Security, JPA, AMQP, WebSocket y Redis

**Decisión:** Spring Boot 3 con Java 21.

**Consecuencias:**
- Mayor consumo de memoria en arranque vs. Quarkus/Micronaut
- Acceso inmediato a todo el ecosistema Spring sin configuración extra
- Virtual threads de Java 21 disponibles para escalar I/O sin cambiar el modelo de programación
