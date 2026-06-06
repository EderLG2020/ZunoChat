---

# C4 Model — Nivel 2: Diagrama de Contenedores

> **Sistema:** ZunoChat · Arquitectura de contenedores y sus relaciones  
> **Norma:** C4 Model — Container Diagram

---

## Diagrama de contenedores

```
╔══════════════════════════════════════════════════════════════════════════════════╗
║                              SISTEMA: ZunoChat                                  ║
║                                                                                 ║
║  ┌──────────────────┐          ┌──────────────────┐                             ║
║  │   1. Frontend    │          │  2. Mobile App   │                             ║
║  │                  │          │                  │                             ║
║  │  Web SPA         │          │  iOS / Android   │                             ║
║  │  (React /        │          │  (React Native / │                             ║
║  │   Next.js)       │          │   Flutter)       │                             ║
║  │                  │          │                  │                             ║
║  │  @stomp/stompjs  │          │  @stomp/stompjs  │                             ║
║  │  SockJS          │          │  SockJS          │                             ║
║  └────────┬─────────┘          └────────┬─────────┘                             ║
║           │                             │                                       ║
║           │  HTTPS REST  /api/**        │                                       ║
║           │  WSS STOMP   /ws            │                                       ║
║           └──────────────┬──────────────┘                                       ║
║                          │                                                      ║
║              ┌───────────▼──────────────────────────┐                           ║
║              │            3. API                    │                           ║
║              │                                      │                           ║
║              │  Spring Boot 3 · Java 21             │                           ║
║              │  Puerto: 8080                        │                           ║
║              │                                      │                           ║
║              │  REST endpoints   /api/**            │                           ║
║              │  WebSocket/STOMP  /ws                │                           ║
║              │  JWT stateless (HS256, 24h)          │                           ║
║              │  BCrypt cost=12                      │                           ║
║              │                                      │                           ║
║              └──┬──────────┬──────────┬─────────────┘                           ║
║                 │          │          │                                          ║
║          JDBC   │   Redis  │  AMQP    │  HTTPS                                  ║
║                 │  (6379)  │  (5672)  │                                          ║
║     ┌───────────▼──┐  ┌────▼────┐  ┌─▼──────────┐  ┌──────────────────┐        ║
║     │ 4. Database  │  │5. Cache │  │ 6. Broker  │  │   (externo)      │        ║
║     │              │  │         │  │            │  │                  │        ║
║     │ PostgreSQL 16│  │ Redis   │  │ RabbitMQ   │  │  Brevo           │        ║
║     │ Puerto: 5432 │  │ 7.2     │  │ 3.13       │  │  (Email SaaS)    │        ║
║     │ DB: zunochat │  │ Puerto  │  │ Puerto     │  │  API REST        │        ║
║     │              │  │ 6379    │  │ 5672       │  │                  │        ║
║     │ Persistencia │  │         │  │ 15672      │  │  OTP / Welcome   │        ║
║     │ principal    │  │Presence │  │(mgmt UI)   │  │  Ban / Reset     │        ║
║     │              │  │Typing   │  │            │  │                  │        ║
║     │              │  │Session  │  │chat.exchg  │  └──────────────────┘        ║
║     │              │  │TTL      │  │            │                               ║
║     └──────────────┘  └─────────┘  └────────────┘                               ║
║                                                                                 ║
║  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐                           ║
║       7. Storage — no implementado aún               │                           ║
║  │    (planeado: S3 / Cloudinary para archivos                                  ║
║       e imágenes de chat)                            │                           ║
║  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                           ║
╚══════════════════════════════════════════════════════════════════════════════════╝
```

---

## 1. Frontend

**Tipo:** Single Page Application  
**Tecnología:** React / Next.js (no incluido en este repo)

Se comunica con la API de dos formas simultáneas:
- **REST** sobre HTTPS para operaciones CRUD: login, historial de mensajes, lista de conversaciones
- **WebSocket/STOMP** sobre WSS para tiempo real: mensajes entrantes, presencia, typing, read receipts

La autenticación viaja en cada request como `Authorization: Bearer <JWT>`. Para WebSocket, el token se pasa como query param en el handshake (`/ws?token=...`) y también en los headers STOMP del `CONNECT`.

---

## 2. Mobile App

**Tipo:** Aplicación móvil nativa/cross-platform  
**Tecnología:** React Native o Flutter (no incluido en este repo)

Misma interfaz de comunicación que el Frontend. El cliente STOMP recomendado es `@stomp/stompjs` con fallback SockJS, con reconexión automática cada 5 segundos y heartbeat al servidor cada 60 segundos para renovar presencia.

---

## 3. API

**Tipo:** Backend monolítico modular  
**Tecnología:** Spring Boot 3 · Java 21 · Puerto `8080`

El contenedor central del sistema. Expone dos protocolos:

| Protocolo | Prefijo | Uso |
|---|---|---|
| HTTP REST | `/api/**` | Auth, usuarios, conversaciones, mensajes, config |
| WebSocket/STOMP | `/ws` | Mensajes en tiempo real, presencia, typing |

Características de la API:
- **Stateless**: toda la sesión viaja en el JWT; no hay `HttpSession`
- **Seguridad en dos capas**: `JwtFilter` (HTTP) + `StompAuthChannelInterceptor` (WS)
- **Interfaces intercambiables**: `IPresenceService` e `IMessageProducer` tienen implementaciones in-memory (dev) y distribuidas con Redis/RabbitMQ (prod), activadas con flags `redis.enabled` y `rabbitmq.enabled`
- **Respuesta unificada**: todos los endpoints devuelven `ApiResponse<T>` con código, mensaje y datos

---

## 4. Database

**Tipo:** Base de datos relacional  
**Tecnología:** PostgreSQL 16 · Puerto `5432` · DB `zunochat`

Almacena toda la información persistente del sistema:

| Tabla | Contenido |
|---|---|
| `users` | Identidad, credenciales, rol, estado, OTP |
| `conversations` | Par de participantes, preview del último mensaje, contadores `unread` desnormalizados |
| `messages` | Historial completo de mensajes con tipo, contenido y estado |
| `app_config` | Configuración dinámica del sistema (ej. `email.enabled`) |

La tabla `conversations` desnormaliza los usernames de ambos participantes para evitar JOINs frecuentes al listar el inbox. La invariante `user1Id < user2Id` garantiza unicidad sin duplicados.

Límites definidos en `docker-compose.yml`: 1 CPU · 512 MB RAM.

---

## 5. Cache

**Tipo:** Almacén en memoria  
**Tecnología:** Redis 7.2 Alpine · Puerto `6379`

Usado exclusivamente por el módulo de tiempo real para presencia y estado de escritura:

| Key pattern | Valor | TTL |
|---|---|---|
| `presence:{userId}` | `"online"` | 65 s (renovado por heartbeat) |
| `presence:lastseen:{userId}` | epoch ms | sin TTL |
| `typing:{convId}:{userId}` | `"1"` | 5 s |

Política de evicción: `allkeys-lru` · Memoria máxima: 128 MB · Sin persistencia (`appendonly no`).

Activación opcional: si `redis.enabled=false`, la API usa `InMemoryPresenceService` (válido para instancia única en dev).

Límites: 0.5 CPU · 192 MB RAM.

---

## 6. Broker

**Tipo:** Message broker  
**Tecnología:** RabbitMQ 3.13 · Puerto `5672` (AMQP) · `15672` (Management UI)  
**Virtual host:** `zunochat`

Desacopla el envío de mensajes del destinatario. El flujo es:

```
WebSocketController
  └─► MessageProducer → RabbitMQ [chat.exchange]
                                       └─► MessageConsumer
                                               └─► SimpMessagingTemplate
                                                       └─► /queue/user.{id}
                                                               └─► cliente destino
```

También enruta eventos de presencia (`PresenceRabbitEvent`) y read receipts (`ReadReceiptRabbitEvent`).

Activación opcional: si `rabbitmq.enabled=false`, la API usa `DirectMessageProducer` que despacha directamente al broker STOMP embebido (válido para dev con un solo nodo).

Límites: 0.5 CPU · 512 MB RAM · Watermark de memoria al 40%.

---

## 7. Storage

**Estado:** No implementado — planeado  
**Tecnología prevista:** AWS S3 o Cloudinary

El modelo de mensajes ya contempla este contenedor: el tipo `MessageType.FILE` e `IMAGE` almacena `fileUrls` (lista de hasta 3 URLs). La API actualmente acepta las URLs como strings en el payload, asumiendo que el cliente ya subió los archivos directamente a un servicio externo. La integración de un servicio de upload propio está pendiente.

---

## Flujos de comunicación — resumen

```
[Frontend / Mobile]
    │
    ├─ HTTPS REST ──────────────────────────────────► [API :8080]
    │                                                      │
    │                                                      ├─ JDBC ──────────► [PostgreSQL :5432]
    │                                                      ├─ Redis ─────────► [Redis :6379]
    │                                                      ├─ AMQP ──────────► [RabbitMQ :5672]
    │                                                      └─ HTTPS ─────────► [Brevo API]
    │
    └─ WSS STOMP ───────────────────────────────────► [API :8080]
                                                           │
                                              AMQP back ◄──┘
                                              (eventos de vuelta al cliente
                                               via /queue/user.{id})
```

---

## Herramientas de administración

Incluidas en `docker-compose.yml` para entorno de desarrollo:

| Herramienta | Puerto | Propósito |
|---|---|---|
| **pgAdmin 4** | `5050` | Gestión visual de PostgreSQL |
| **RabbitMQ Management UI** | `15672` | Monitoreo de colas y exchanges |
| **RedisInsight** | `5540` | Visualización de keys y TTLs en Redis |