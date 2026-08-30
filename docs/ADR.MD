---

# Architecture Decision Records — ZunoChat API

---

## ADR-001 — Framework backend

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

---

## ADR-002 — Base de datos relacional

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

---

## ADR-003 — Mensajería en tiempo real

**Contexto:** ZunoChat requiere entrega de mensajes, indicadores de escritura y presencia en tiempo real entre usuarios conectados.

**Problema:** Elegir el protocolo de comunicación en tiempo real.

**Alternativas:**
- Polling — simple, alto consumo de red, latencia variable
- SSE (Server-Sent Events) — unidireccional, no apto para envío desde cliente
- WebSocket puro — bidireccional, sin protocolo de mensajería sobre él
- WebSocket + STOMP — bidireccional, protocolo de mensajería con destinos y suscripciones

**Decisión:** WebSocket con STOMP sobre SockJS.

**Consecuencias:**
- Mayor complejidad de configuración (handshake, interceptores, session registry)
- Autenticación en dos capas: HTTP handshake (`?token=`) + frame STOMP `CONNECT`
- SockJS provee fallback HTTP para entornos que bloquean WebSocket
- Destinos STOMP (`/app/`, `/topic/`, `/queue/`) dan estructura clara al protocolo

---

## ADR-004 — Broker de mensajería

**Contexto:** Con WebSocket, los mensajes deben enrutarse al destinatario correcto, potencialmente conectado a otra instancia del servidor.

**Problema:** Desacoplar el envío de un mensaje de su entrega al receptor.

**Alternativas:**
- Broker embebido de Spring — simple, solo funciona con una instancia
- Kafka — muy alto throughput, mayor operación, overkill para chat 1-a-1
- RabbitMQ — broker robusto, soporte a STOMP nativo, bajo overhead para mensajería dirigida

**Decisión:** RabbitMQ 3.13 con exchange `chat.exchange`, activable mediante flag `rabbitmq.enabled`.

**Consecuencias:**
- En dev (`rabbitmq.enabled=false`) se usa `DirectMessageProducer` que despacha directo al broker STOMP embebido, sin dependencias externas
- En prod (`rabbitmq.enabled=true`) se usa `MessageProducer` → RabbitMQ → `MessageConsumer` → cliente
- La interfaz `IMessageProducer` permite intercambiar la implementación sin tocar el dominio
- Añade un contenedor más a operar y monitorear

---

## ADR-005 — Presencia y estado de escritura

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

---

## ADR-006 — Autorización con permisos granulares

**Contexto:** El sistema tiene tres roles (USER, ADMIN, SUPERADMIN) con distintos niveles de acceso a recursos. Se necesita control fino por acción, no solo por rol.

**Problema:** Modelar autorización que permita crecer sin hardcodear roles en cada endpoint.

**Alternativas:**
- Control por rol (`hasRole`) — simple, poco flexible, acoplado a la jerarquía de roles
- ACL (Access Control List) — muy granular, alta complejidad operativa
- Permisos en JWT como `GrantedAuthority` — transportados en el token, evaluables con `@PreAuthorize`

**Decisión:** Permisos granulares (`"dashboard:editar"`, `"usuarios:bannear"`) embebidos en el JWT como claim `permissions`, registrados como `GrantedAuthority` por el `JwtFilter`.

**Consecuencias:**
- Los endpoints usan `@PreAuthorize("hasAuthority('usuarios:bannear')")` en lugar de `hasRole('ADMIN')`
- Añadir un permiso a un rol solo requiere modificar el enum `Role`, sin tocar los controllers
- El token crece ligeramente en tamaño al incluir la lista de permisos
- Los permisos son inmutables durante la sesión (24 h); un cambio de rol no se refleja hasta que el usuario haga login de nuevo

---

## ADR-007 — Desnormalización de usernames en conversaciones

**Contexto:** Al listar el inbox de conversaciones, se necesita mostrar el nombre del otro participante. Hay miles de consultas de este tipo.

**Problema:** Evitar JOINs costosos a la tabla `users` en cada listado de conversaciones.

**Alternativas:**
- JOIN `conversations` → `users` en cada query — correcto en diseño, costoso a escala
- Caché de usuarios en Redis — añade invalidación, complejidad de coherencia
- Desnormalizar `user1_username` y `user2_username` en la tabla `conversations` — redundancia controlada

**Decisión:** Desnormalizar usernames (y avatar cuando se implemente) directamente en `conversations`.

**Consecuencias:**
- El listado del inbox es una query de tabla única, sin JOINs
- Si un usuario cambia su username, hay que actualizar todas sus conversaciones (actualmente no implementado el cambio de username, mitigando el riesgo)
- La invariante `user1Id < user2Id` garantiza unicidad del par sin necesidad de búsqueda dual ni índice compuesto adicional

---

## ADR-008 — Servicio de email transaccional

**Contexto:** El sistema envía correos para OTP, bienvenida, cambio de estado de cuenta y reset de contraseña.

**Problema:** Elegir proveedor de email transaccional y estrategia de integración.

**Alternativas:**
- SMTP propio (JavaMailSender) — control total, gestión de reputación compleja, sin analytics
- SendGrid — popular, bien documentado, costo por volumen
- AWS SES — barato a escala, configuración más compleja, requiere dominio verificado
- Brevo (ex-Sendinblue) — tier gratuito generoso, SDK Java oficial, templates HTML

**Decisión:** Brevo con SDK oficial Java (`brevo-java`).

**Consecuencias:**
- El `EmailService` absorbe toda excepción de la API de Brevo; un fallo de correo nunca rompe el flujo de negocio
- El flag `email.enabled` en `app_config` permite activar/desactivar el envío en caliente sin reiniciar
- En dev, el OTP se expone en la respuesta REST para facilitar pruebas sin dominio verificado en Brevo
- Dependencia de un SaaS externo; si Brevo cae, los correos se pierden silenciosamente (solo log)

---

## ADR-009 — Activación de racha por opt-in mutuo

**Contexto:** Se implementa una "racha" (streak) al estilo Snapchat: cuenta los días consecutivos en que dos usuarios de una conversación `DIRECT` intercambiaron al menos un mensaje cada uno.

**Problema:** Decidir si la racha se cuenta automáticamente para toda conversación `DIRECT` desde el primer mensaje, o si requiere una acción explícita de ambos usuarios.

**Alternativas:**
- Auto-activar para toda conversación `DIRECT` — cero fricción, pero cuenta rachas para conversaciones donde ninguno de los dos quiere ese seguimiento (presión social no deseada)
- Toggle unilateral por usuario, sin confirmación del otro — simple, pero un usuario puede activar seguimiento sobre una interacción que el otro no quiere exponer
- Opt-in mutuo: cualquiera activa el toggle en Configuración → se envía una solicitud al otro dentro del chat → la racha solo cuenta si ambos aceptan

**Decisión:** Opt-in mutuo. Activar (`PATCH /api/streaks/{id} {enabled:true}`) crea una solicitud (`requestStatus=PENDING`); si el otro ya la había pedido, se acepta automáticamente (evita fricción cuando ambos activan el switch casi al mismo tiempo). Desactivar, en cambio, es unilateral e inmediato para cualquiera de los dos — no requiere confirmación.

**Consecuencias:**
- La tabla `streaks` no se crea para una conversación hasta que alguien solicita activarla (`StreakService#requestActivation`), a diferencia de `conversations`/`messages` que existen desde el primer mensaje
- El frontend necesita una UI de solicitud/aceptación dentro del chat, no solo un switch de Configuración
- El broadcast de estos eventos (`REQUEST_SENT`, `REQUEST_ACCEPTED`, `REQUEST_DECLINED`, `INCREMENTED`, `RESET`, `AT_RISK`, `BROKEN`, `DISABLED`) usa un tópico STOMP dedicado, `/topic/streak.{conversationId}`, en lugar de reutilizar `/topic/conversation.{id}` — mismo criterio que ya separa `/topic/typing.{id}` y `/topic/read.{id}` de los mensajes