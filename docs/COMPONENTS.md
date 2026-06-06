---

# C4 Model — Nivel 3: Módulos Internos de ZunoChat API

> **Sistema:** `zunochat-api` · Spring Boot 3 · Java 21  
> **Nivel:** Component Diagram (zoom-in por módulo)

---

## 1. Módulo `auth` — Autenticación

**Paquete:** `module/usermanagement` · **Controlador:** `AuthController → /api/auth`

Este módulo gestiona el ciclo completo de identidad de un usuario nuevo. El flujo es secuencial y en tres pasos:

```
[Cliente]
    │
    ├─ POST /api/auth/register ──────────────────────────────────────────────┐
    │       │                                                                 │
    │  [AuthController]                                                       │
    │       └─► [AuthService]                                                 │
    │               ├─ valida unicidad (dni / username / email)               │
    │               ├─ genera OTP de 6 dígitos (OtpService)                  │
    │               ├─ crea UserModel con status=PENDING_VERIFICATION         │
    │               ├─ persiste (UserRepository → MySQL)                      │
    │               └─ envía email OTP si email.enabled=true (EmailService)   │
    │                     DEV: el OTP también va en la respuesta              │
    │                     PROD: nunca se expone el OTP en la respuesta        │
    │
    ├─ POST /api/auth/verify-otp ────────────────────────────────────────────┐
    │       └─► [AuthService]                                                 │
    │               ├─ valida OTP no expirado (OtpService.isValid)            │
    │               ├─ activa cuenta: status → ACTIVE, borra otpCode         │
    │               ├─ envía email de bienvenida (EmailService)               │
    │               └─ genera y devuelve JWT (JwtService)                     │
    │
    └─ POST /api/auth/login ─────────────────────────────────────────────────┐
            └─► [AuthService]
                    ├─ busca por username o email
                    ├─ verifica status (BANNED / INACTIVE / PENDING)
                    ├─ valida contraseña con BCrypt
                    └─ genera y devuelve JWT
```

**Componentes internos:**

| Clase | Responsabilidad |
|---|---|
| `AuthController` | Expone los 3 endpoints REST, sin lógica de negocio |
| `AuthService` | Orquesta registro, verificación y login |
| `JwtService` | Genera y valida tokens HS256 con claims `role` + `permissions` + `userId` |
| `OtpService` | Genera OTP de 6 dígitos y calcula expiración (+10 min) |
| `JwtFilter` | Intercepta cada request, extrae JWT y carga permisos como `GrantedAuthority` |
| `SecurityConfig` | Define rutas públicas (`/api/auth/**`, `/ws/**`) y aplica política stateless |

**JWT generado — claims:**
```json
{
  "sub": "juanperez",
  "role": "USER",
  "permissions": ["profile:ver", "profile:editar", "chat:enviar", "chat:ver"],
  "userId": 42,
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

## 2. Módulo `users` — Gestión de Usuarios

**Paquetes:** `module/usermanagement` · **Controladores:** `UserController`, `AdminController`

Maneja el modelo de usuario, búsqueda y acciones administrativas con permisos granulares.

```
[Cliente autenticado]
    │
    ├─ GET /api/users/search?q=xxx ──────────────────────────────────────────┐
    │       └─► [UserController]                                              │
    │               ├─ requiere q.length >= 2                                 │
    │               ├─ excluye al propio usuario del resultado                │
    │               ├─ filtra solo status=ACTIVE                              │
    │               └─ devuelve máx. 10 resultados (UserSearchResponse)       │
    │
    └─ /api/admin/** (solo ADMIN / SUPERADMIN) ──────────────────────────────┐
            ├─ GET    /dashboard          → permiso: dashboard:ver
            ├─ PUT    /dashboard          → permiso: dashboard:editar
            ├─ GET    /usuarios           → permiso: usuarios:ver
            ├─ PATCH  /usuarios/{id}/ban  → permiso: usuarios:bannear
            ├─ DELETE /usuarios/{id}      → permiso: usuarios:eliminar
            └─ PATCH  /usuarios/{id}/rol  → permiso: roles:asignar
```

**Modelo `UserModel` — campos clave:**

| Campo | Tipo | Detalle |
|---|---|---|
| `id` | `Long` | PK autoincremental |
| `dni` | `String(20)` | Único, identidad nacional |
| `username` | `String(50)` | Único, visible públicamente |
| `email` | `String(120)` | Único |
| `password` | `String` | BCrypt cost=12 |
| `role` | `Role` enum | `USER / ADMIN / SUPERADMIN` |
| `status` | `UserStatus` enum | `ACTIVE / PENDING_VERIFICATION / BANNED / INACTIVE` |
| `otpCode` | `String(6)` | Nulo tras verificar |
| `otpExpiration` | `LocalDateTime` | Nulo tras verificar |

**Tabla de roles y permisos:**

| Permiso | USER | ADMIN | SUPERADMIN |
|---|:---:|:---:|:---:|
| `profile:ver` / `profile:editar` | ✓ | ✓ | ✓ |
| `chat:ver` / `chat:enviar` | ✓ | ✓ | ✓ |
| `dashboard:ver` / `dashboard:editar` | — | ✓ | ✓ |
| `usuarios:ver` / `usuarios:bannear` | — | ✓ | ✓ |
| `usuarios:eliminar` / `roles:asignar` | — | — | ✓ |
| `config:ver` / `config:editar` | — | — | ✓ |
| `sistema:configurar` / `superadmin:panel` | — | — | ✓ |

---

## 3. Módulo `chat` — Mensajería en Tiempo Real

**Paquete:** `module/messagemanagement` · El módulo más complejo del sistema.

Opera en dos capas paralelas: **REST** para historial y **WebSocket/STOMP + RabbitMQ** para tiempo real.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CAPA REST (historial)                       │
│                                                                     │
│  GET  /api/conversations          → lista conversaciones paginadas  │
│  POST /api/conversations          → crea o retorna conversación     │
│  GET  /api/conversations/{id}/messages → historial paginado         │
│  POST /api/messages               → envía mensaje (REST)            │
│  POST /api/messages/mark-read     → marca mensajes como leídos      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   CAPA REAL-TIME (WebSocket / STOMP)                │
│                                                                     │
│  CONNECT  /ws  ──────────────────────────────────────────────────┐  │
│                  JwtHandshakeInterceptor valida JWT               │  │
│                  StompAuthChannelInterceptor carga Principal      │  │
│                  WebSocketSessionRegistry registra sessionId      │  │
│                  PresenceService.markOnline(userId) → Redis       │  │
│                  RabbitMQ publica PresenceEvent (online=true)     │  │
│                                                                   │  │
│  SEND /app/chat.send ────────────────────────────────────────┐   │  │
│        │                                                      │   │  │
│   [WebSocketController]                                       │   │  │
│        ├─ MessageService.sendMessage() → persiste en MySQL    │   │  │
│        └─ MessageProducer.publishMessage()                    │   │  │
│                └─► RabbitMQ exchange "chat.exchange"          │   │  │
│                         └─► MessageConsumer                   │   │  │
│                                 └─► SimpMessagingTemplate     │   │  │
│                                       └─► /queue/user.{id}   │   │  │
│                                                               │   │  │
│  SEND /app/chat.typing → publica TypingEvent                 │   │  │
│        └─► /topic/typing.{conversationId}                    │   │  │
│                                                               │   │  │
│  SEND /app/chat.read → markAsRead + ReadReceiptRabbitEvent   │   │  │
│  SEND /app/heartbeat → renueva TTL en Redis (65 seg)         │   │  │
│                                                               │   │  │
│  DISCONNECT  ─────────────────────────────────────────────┐  │   │  │
│                SessionRegistry.removeSession()            │  │   │  │
│                Si no quedan sesiones:                     │  │   │  │
│                  PresenceService.markOffline() → Redis    │  │   │  │
│                  RabbitMQ publica PresenceEvent           │  │   │  │
│                  (online=false, lastSeen=epoch_ms)        │  │   │  │
└─────────────────────────────────────────────────────────────────────┘
```

**Componentes internos:**

| Clase | Responsabilidad |
|---|---|
| `ConversationService` | Crea/lista conversaciones; invariante: `user1Id < user2Id` para evitar duplicados |
| `MessageService` | Persiste mensajes, actualiza preview, incrementa `unreadCount` del receptor |
| `WebSocketController` | Punto de entrada STOMP; orquesta eventos de conexión, mensajes, typing y lectura |
| `PresenceService` | Estado online/offline en Redis con TTL de 65 seg; typing con TTL de 5 seg |
| `InMemoryPresenceService` | Implementación alternativa sin Redis (perfil sin `redis.enabled=true`) |
| `WebSocketSessionRegistry` | Mapea `userId → Set<sessionId>` para manejar multi-tab correctamente |
| `MessageProducer` | Publica a RabbitMQ (`chat.exchange`) |
| `DirectMessageProducer` | Publica mensajes directamente al broker STOMP (sin RabbitMQ, perfil dev) |
| `MessageConsumer` | Consume de RabbitMQ y despacha al destinatario vía `SimpMessagingTemplate` |

**Tipos de mensaje soportados (`MessageType`):**

| Tipo | Descripción | Campos requeridos |
|---|---|---|
| `TEXT` | Texto plano | `textContent` |
| `IMAGE` | Imagen adjunta | `fileUrls` (máx. 3) |
| `FILE` | Archivo adjunto | `fileUrls` (máx. 3) |
| `PAYLOAD` | Contenido estructurado | `payload` + `payloadType` (`SALES / SYSTEM / SURVEY / CARD`) |

**Redis keys usadas por Presence:**
```
presence:{userId}            → "online"  TTL: 65s
presence:lastseen:{userId}   → epoch_ms  sin TTL
typing:{conversationId}:{userId} → "1"   TTL: 5s
```

---

## 4. Módulo `notifications` — Correos Transaccionales

**Paquete:** `common/email` · Implementado sobre **Brevo** (ex-Sendinblue).

```
[AuthService / AdminService]
        │
        └─► EmailService.send*(...)
                │
                ├─ shouldSend() → consulta AppConfigServiceDomain → MySQL
                │
                ├─ [email.enabled=false] → solo log, no envía
                │
                └─ [email.enabled=true]  → Brevo API (TransactionalEmailsApi)
                        └─ HTML desde EmailTemplates.*()
```

**Métodos públicos:**

| Método | Asunto del correo | Trigger |
|---|---|---|
| `sendOtp(email, username, otp)` | `Tu código de verificación — ZunoChat` | `AuthService.register()` |
| `sendWelcome(email, username, bannerUrl)` | `¡Bienvenido a ZunoChat, {user}! 🎉` | `AuthService.verifyOtp()` |
| `sendAccountStatusChanged(...)` | Varía según nuevo estado | Admin banea / activa usuario |
| `sendPasswordResetConfirm(email, username)` | `Tu contraseña fue restablecida` | Reset de contraseña |

**Comportamiento según perfil:**

| Perfil | `email.enabled` | Comportamiento |
|---|---|---|
| `dev` | `true` | Envía correo real; OTP también va en la respuesta API |
| `dev` | `false` | No envía; OTP va en la respuesta API |
| `prod` | `true` | Envía correo real; respuesta API NO expone el OTP |
| `prod` | `false` | No envía; respuesta API NO expone nada sensible |

> El `EmailService` captura toda `ApiException` internamente — un fallo de correo **nunca rompe el flujo principal**.

---

## 5. Módulo `config` — Configuración Dinámica del Sistema

**Paquete:** `common/config` · Permite cambiar comportamiento en caliente sin redeployar.

```
GET  /api/config/email   → permiso: config:ver    → devuelve {enabled, updatedAt, updatedBy}
PUT  /api/config/email   → permiso: config:editar → activa/desactiva envío de correos
```

**`AppConfigModel`** persiste pares clave-valor en tabla `app_config`:

```
key: "email.enabled"   value: "true" | "false"
```

El `AppConfigServiceDomain` mantiene la config en memoria con `@Cacheable` (o lectura directa a MySQL). Es consumido por `EmailService` en cada envío para decidir si procede.

---

## 6. Módulo `search` — Búsqueda de Usuarios

**Paquete:** `module/usermanagement` · **Endpoint:** `GET /api/users/search`

Módulo simple, sin servicio propio — la lógica vive directamente en `UserController` + `UserRepository`.

```
GET /api/users/search?q={término}
        │
   [UserController]
        ├─ guarda: q.length >= 2 (evita queries vacías)
        ├─ resuelve userId del principal autenticado
        └─ UserRepository.searchByUsername(q, myId, ACTIVE, PageRequest(0, 10))
                └─ JPQL: LIKE %q% excluye al propio usuario y no-ACTIVE
                └─ devuelve List<UserSearchResponse> {id, username, avatar?}
```

**Restricciones de diseño:**
- Mínimo 2 caracteres en el término de búsqueda
- Máximo 10 resultados por consulta
- Solo devuelve usuarios con `status = ACTIVE`
- El usuario autenticado queda excluido de sus propios resultados

---

## Dependencias entre módulos

```
auth ──────────────────────► users (UserRepository, UserModel)
auth ──────────────────────► notifications (EmailService)
auth ──────────────────────► config (AppConfigServiceDomain)
chat ──────────────────────► users (UserRepository para resolver usernames)
notifications ─────────────► config (consulta email.enabled en cada envío)
admin (users) ──────────────► users (UserRepository para ban/delete)
config ─────────────────────► (standalone, sin dependencias de módulo)
search ─────────────────────► users (UserRepository.searchByUsername)
```