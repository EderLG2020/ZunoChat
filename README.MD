# ZunoChat — Backend API

API REST + WebSocket en tiempo real para mensajería directa, construida con **Spring Boot 3**, JWT, PostgreSQL, STOMP/WebSocket, Redis y RabbitMQ (opcionales).

---

## Tecnologías

| Categoría | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.3.2 |
| Seguridad | Spring Security + JWT (jjwt) | 0.11.5 |
| Base de datos | PostgreSQL | 16 |
| WebSocket | Spring WebSocket + STOMP + SockJS | — |
| Mensajería async | RabbitMQ (opcional) | 3.13 |
| Caché / Presencia | Redis (opcional) | 7.2 |
| Correo | Brevo (Sendinblue SDK) | 1.1.0 |
| Contenedores | Docker + Docker Compose | — |

---

## Arquitectura

```
HTTP/WebSocket Request
       │
  ┌────┴────┐
JwtFilter   JwtHandshakeInterceptor (WS handshake ?token=)
       │         │
       │    StompAuthChannelInterceptor (STOMP CONNECT)
       │         │
  Controllers    WebSocketController
  (Auth, Admin,  (@MessageMapping)
  Conversation,       │
  Message,    ┌───────┴───────┐
  AppConfig)  IPresenceService  IMessageProducer
              (Redis | Memory)  (Rabbit | Direct)
                     │
              Services → Repositories → PostgreSQL
```

**Principios clave:**
- Interfaces intercambiables: `IPresenceService`, `IWebSocketSessionRegistry`, `IMessageProducer` — Redis/RabbitMQ en prod, en memoria en dev.
- Respuesta unificada `ApiResponse<T>` en todos los endpoints.
- Stateless: el estado de autenticación viaja completamente en el JWT.
- `Conversation` desnormaliza usernames para evitar JOINs frecuentes.

---

## Modelos de Datos

### `users`
| Campo | Tipo | Descripción |
|---|---|---|
| id | BIGINT PK | Autogenerado |
| dni | VARCHAR(20) | Único |
| username | VARCHAR(50) | Único, público |
| email | VARCHAR(120) | Único |
| password | TEXT | BCrypt factor 12 |
| role | ENUM | `USER`, `ADMIN`, `SUPERADMIN` |
| status | ENUM | `PENDING_VERIFICATION`, `ACTIVE`, `BANNED`, `INACTIVE`, `DELETED` |
| otp_code / otp_expiration | — | OTP de 6 dígitos, válido 10 min; se limpia al verificar |

### `conversations`
| Campo | Tipo | Descripción |
|---|---|---|
| id | BIGINT PK | Autogenerado |
| user1_id / user2_id | BIGINT | `user1Id < user2Id` — garantiza unicidad sin búsqueda dual |
| user1_username / user2_username | VARCHAR(50) | Desnormalizados |
| last_message_preview | VARCHAR(50) | Vista previa |
| status | ENUM | `ONLINE`, `TYPING`, `OFFLINE`, `AWAY` |
| unread_count_user1/2 | INT | Mensajes no leídos por usuario |

### `messages`
| Campo | Tipo | Descripción |
|---|---|---|
| id | BIGINT PK | Autogenerado |
| conversation_id / sender_id / receiver_id | BIGINT | — |
| type | ENUM | `TEXT`, `PAYLOAD`, `FILE`, `IMAGE` |
| text_content | TEXT | Contenido de texto |
| payload | JSONB | Objeto estructurado (cuando `type=PAYLOAD`) |
| payload_type | ENUM | `SALES`, `SYSTEM`, `SURVEY`, `CARD` |
| file_urls | JSONB | Array de URLs (máx. 3) |
| status | ENUM | `SENT`, `DELIVERED`, `READ` |

---

## Autenticación

### Flujo de registro
```
1. POST /api/auth/register  → crea usuario PENDING_VERIFICATION, genera OTP (10 min)
2. POST /api/auth/verify-otp → activa cuenta, retorna JWT
3. POST /api/auth/login      → retorna JWT
```

> En perfil **dev**, el OTP aparece en el campo `message` de la respuesta. En **prod**, solo se envía por correo.

### JWT (HS256, 24 h)
```json
{
  "sub": "nombre_usuario",
  "role": "ADMIN",
  "permissions": ["dashboard:ver", "chat:enviar", "..."],
  "userId": 42
}
```

### Autenticación WebSocket (2 capas)
1. **HTTP Handshake**: `ws://localhost:8080/ws?token=<jwt>`
2. **Frame STOMP CONNECT**: header `Authorization: Bearer <token>` (fallback a query param)

---

## Roles y Permisos

| Permiso | USER | ADMIN | SUPERADMIN |
|---|:---:|:---:|:---:|
| profile:ver / editar | ✅ | ✅ | ✅ |
| chat:enviar / ver | ✅ | ✅ | ✅ |
| dashboard:ver / editar | — | ✅ | ✅ |
| usuarios:ver / bannear / activar | — | ✅ | ✅ |
| usuarios:eliminar | — | — | ✅ |
| roles:asignar | — | — | ✅ |
| config:ver / editar / sistema:configurar | — | — | ✅ |

---

## Endpoints REST

### Auth — `/api/auth` (público)

| Método | Endpoint | Body |
|---|---|---|
| POST | `/register` | `{ dni, username, email, password }` |
| POST | `/verify-otp` | `{ email, otpCode }` |
| POST | `/login` | `{ identifier, password }` |

**Errores comunes:**

| Código | HTTP | Descripción |
|---|---|---|
| `AUTH_BAD_CREDENTIALS` | 401 | Credenciales incorrectas |
| `USER_BANNED` | 403 | Cuenta suspendida |
| `OTP_PENDING_REQUIRED` | 403 | Cuenta sin verificar |

---

### Admin — `/api/admin` (requiere JWT)

| Método | Endpoint | Permiso |
|---|---|---|
| GET/PUT | `/dashboard` | `dashboard:ver/editar` |
| GET | `/usuarios` | `usuarios:ver` |
| PATCH | `/usuarios/{id}/ban` | `usuarios:bannear` |
| DELETE | `/usuarios/{id}` | `usuarios:eliminar` |
| PATCH | `/usuarios/{id}/rol` | `roles:asignar` |

---

### Conversaciones — `/api/conversations`

**GET** `/api/conversations?page=0&size=20` — lista ordenada por último mensaje.

**POST** `/api/conversations` — crea o retorna conversación existente.
```json
{ "targetUserId": 5 }
```

---

### Mensajes — `/api/messages`

**GET** `/api/messages?conversationId=1&page=0&size=30`

**POST** `/api/messages`
```js
{ "conversationId": 1, "type": "TEXT", "textContent": "Hola" }
{ "conversationId": 1, "type": "PAYLOAD", "payloadType": "SALES", "payload": { ... } }

{ "conversationId": 1, "type": "FILE", "fileUrls": ["https://..."] }
```

**PATCH** `/api/messages/read` — `{ "conversationId": 1 }`

---

### Config — `/api/config` (solo SUPERADMIN)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/email` | Estado del servicio de email |
| PUT | `/email` | Activar/desactivar en caliente |

---

## WebSocket / STOMP

**Endpoint:** `ws://localhost:8080/ws` · Fallback SockJS: `http://localhost:8080/ws`

### Publicación (cliente → servidor)

| Destino | Body |
|---|---|
| `/app/chat.send` | `{ conversationId, type, textContent?, payload?, fileUrls? }` |
| `/app/chat.typing` | `{ conversationId, typing: true/false }` |
| `/app/chat.read` | `{ conversationId }` |
| `/app/heartbeat` | `""` (cada 60 s) |

### Suscripción (servidor → cliente)

| Destino | Evento |
|---|---|
| `/topic/conversation.{id}` | Nuevo mensaje |
| `/topic/typing.{id}` | Indicador de escritura |
| `/topic/read.{id}` | Confirmación de lectura |
| `/topic/presence.{userId}` | Usuario online/offline |
| `/user/queue/notifications` | Notificación personal |

### Conexión
```js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS(`http://localhost:8080/ws?token=${token}`),
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe('/topic/conversation.42', frame => console.log(JSON.parse(frame.body)));
    setInterval(() => client.publish({ destination: '/app/heartbeat', body: '' }), 60000);
  },
});
client.activate();
```

---

## Formato de Respuesta

```json
[
  { "success": true, "code": "OK_LOGIN", "status": 200, "message": "...", "timestamp": "...", "data": {} },

  { "success": false, "code": "OTP_EXPIRED", "status": 400, "message": "...", "timestamp": "..." },

  { "success": false, "code": "VALID_FIELDS", "status": 400, "errors": { "email": "Formato inválido" } }
]

```

---

## Variables de Entorno (`.env`)

```env
# Base de datos
DB_HOST=localhost  DB_PORT=5432  DB_NAME=zunochat  DB_USER=admin  DB_PASSWORD=admin123

# JWT (mín. 32 caracteres)
JWT_SECRET=zunochat_dev_secret_clave_super_segura_2024

# Email
BREVO_API_KEY=TU_API_KEY

# Redis
REDIS_HOST=localhost  REDIS_PORT=6379  REDIS_PASSWORD=redis123

# RabbitMQ
RABBITMQ_HOST=localhost  RABBITMQ_PORT=5672  RABBITMQ_USERNAME=rabbit  RABBITMQ_PASSWORD=rabbit123  RABBITMQ_VHOST=zunochat

# Feature flags (false = implementación en memoria, no requiere los servicios)
REDIS_ENABLED=false
RABBITMQ_ENABLED=false
```

---

## Docker

```bash
# Solo PostgreSQL (modo dev)
docker compose up -d

# Con Redis
docker compose --profile redis up -d

# Con RabbitMQ
docker compose --profile rabbitmq up -d

# Todo
docker compose --profile redis --profile rabbitmq up -d
```

| Servicio | URL | Credenciales |
|---|---|---|
| API REST | http://localhost:8080 | — |
| pgAdmin 4 | http://localhost:5050 | admin@admin.com / admin123 |
| RedisInsight | http://localhost:5540 | — |
| RabbitMQ Management | http://localhost:15672 | rabbit / rabbit123 |

### Conexión a PostgreSQL desde pgAdmin

> pgAdmin corre dentro de Docker, por lo que **no puede usar `localhost`** para llegar a la base de datos. Usar `host.docker.internal` en su lugar.

| Campo | Valor |
|---|---|
| Host | `host.docker.internal` |
| Port | `5432` |
| Database | `zunochat` |
| Username | `admin` |
| Password | `admin123` |
 
---

## Ejecución Local

```bash
git clone <repo> && cd zunochat
cp .env.example .env        # editar valores
docker compose up -d        # levantar BD
./mvnw spring-boot:run      # API en http://localhost:8080
```

**Perfiles:** `dev` (OTP visible en respuesta) · `prod` (OTP solo por correo)
```properties
# application.properties
spring.profiles.active=dev
```

---

## DataSeeder

Al iniciar, crea usuarios fijos si no existen:

| Rol | Username | Email | Contraseña |
|---|---|---|---|
| SUPERADMIN | superadmin | superadmin@zunochat.com | Super@2024! |
| ADMIN | admin1 / admin2 | admin1@zunochat.com | Admin@2024! |
| USER | ×200 (DataFaker ES) | — | User@2024! |

> En reinicios posteriores detecta los existentes y no duplica. Si faltan usuarios USER, solo crea los necesarios.

---

## Colección Bruno

```
zunochat-bruno/
├── environments/dev.bru
├── users/
│   ├── auth/          register · verify-otp · login
│   └── admin/         dashboard · ban · eliminar · rol · listar
└── message/
    crear · listar conversacion · enviar · paginados · marcar leído
```

Importar en Bruno → seleccionar entorno **dev**.