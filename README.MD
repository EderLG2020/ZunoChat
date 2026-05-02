# ZunoChat — Backend API

> API REST para una plataforma de mensajería en tiempo real, construida con Spring Boot 3, JWT y PostgreSQL.

---

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Arquitectura del Proyecto](#arquitectura-del-proyecto)
- [Estructura de Directorios](#estructura-de-directorios)
- [Modelos de Datos](#modelos-de-datos)
- [Autenticación y Seguridad](#autenticación-y-seguridad)
- [Sistema de Roles y Permisos](#sistema-de-roles-y-permisos)
- [Endpoints de la API](#endpoints-de-la-api)
- [Formato de Respuesta](#formato-de-respuesta)
- [Configuración y Variables de Entorno](#configuración-y-variables-de-entorno)
- [Ejecución con Docker](#ejecución-con-docker)
- [Ejecución Local](#ejecución-local)
- [Datos Iniciales (DataSeeder)](#datos-iniciales-dataseeder)
- [Colección Bruno](#colección-bruno)

---

## Descripción General

ZunoChat es una API REST backend para una plataforma de mensajería directa (DM) entre usuarios. Implementa un flujo completo de autenticación con verificación por OTP vía correo electrónico, sistema de roles con permisos granulares integrados en el JWT, y un módulo de mensajería con soporte para texto plano, payloads estructurados JSON, archivos e imágenes.

La API está diseñada con foco en rendimiento: las conversaciones y mensajes utilizan datos desnormalizados para evitar JOINs en los listados críticos, e índices estratégicos sobre las columnas de mayor consulta.

---

## Tecnologías Utilizadas

| Categoría | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.3.2 |
| Seguridad | Spring Security + JWT (jjwt) | 0.11.5 |
| Persistencia | Spring Data JPA + Hibernate | — |
| Base de datos | PostgreSQL | 16 |
| Correo | Brevo (Sendinblue SDK) | 1.1.0 |
| Utilidades | Lombok | 1.18.32 |
| Bloom Filter | Google Guava | 33.2.1-jre |
| Validación | Jakarta Validation (Bean Validation) | — |
| Build | Maven | 3.x |
| Contenedores | Docker + Docker Compose | — |
| Cliente HTTP interno | OkHttp3 | 4.12.0 |

---

## Arquitectura del Proyecto

El proyecto sigue una arquitectura modular por dominio funcional, con una capa `common` para las utilidades transversales (seguridad, excepciones, respuestas, email).

```
┌─────────────────────────────────────────────────────────┐
│                     HTTP Request                        │
└────────────────────────┬────────────────────────────────┘
                         │
                    JwtFilter
              (valida token, extrae rol
               y permisos → SecurityContext)
                         │
              ┌──────────┴──────────┐
              │                     │
         Controllers           SecurityConfig
     (AuthController,        (rutas públicas /
      AdminController,        rutas protegidas)
      ConversationController,
      MessageController,
      AppConfigController)
              │
         Services / Application Layer
     (AuthService, ConversationService,
      MessageService, AppConfigServiceDomain)
              │
         Repositories (Spring Data JPA)
              │
         PostgreSQL 16
```

### Principios de diseño aplicados

- **Separación por capas**: controller → application (service) → persistence (repository) → domain (entity).
- **Respuesta unificada**: todas las respuestas pasan por `ApiResponse<T>`, con campo `success`, `code`, `status`, `message`, `timestamp` y opcionalmente `data` o `errors`.
- **Códigos de aplicación centralizados**: el enum `AppCode` define todos los códigos de éxito y error con su HTTP status asociado, evitando strings dispersos en el código.
- **Stateless**: no se usa sesión HTTP. El estado de autenticación viaja completamente en el JWT.
- **Desnormalización estratégica**: las entidades `Conversation` y `Message` almacenan datos de usuario (username, avatar) para evitar JOINs en los listados de alta frecuencia.

---

## Estructura de Directorios

```
src/main/java/com/example/backend/
│
├── BackendApplication.java
│
├── common/
│   ├── config/
│   │   ├── DataSeeder.java                   ← Carga inicial de usuarios (Bloom Filter)
│   │   ├── controller/AppConfigController.java
│   │   ├── domain/AppConfigModel.java
│   │   ├── domain/AppConfigServiceDomain.java
│   │   ├── dto/EmailConfigRequest.java
│   │   ├── dto/EmailConfigResponse.java
│   │   └── persistence/AppConfigRepository.java
│   │
│   ├── email/
│   │   ├── EmailService.java                 ← Integración con Brevo
│   │   └── template/EmailTemplates.java      ← HTML para OTP y bienvenida
│   │
│   ├── enums/
│   │   ├── ConversationStatus.java           ← ONLINE, TYPING, OFFLINE, AWAY
│   │   ├── EmailType.java
│   │   ├── MessageStatus.java                ← SENT, DELIVERED, READ
│   │   ├── MessageType.java                  ← TEXT, PAYLOAD, FILE, IMAGE
│   │   ├── PayloadType.java                  ← SALES, SYSTEM, SURVEY, CARD
│   │   ├── Role.java                         ← USER, ADMIN, SUPERADMIN + permisos
│   │   └── UserStatus.java                   ← PENDING_VERIFICATION, ACTIVE, BANNED...
│   │
│   ├── exception/
│   │   ├── AppException.java
│   │   └── GlobalExceptionHandler.java
│   │
│   ├── response/
│   │   ├── ApiResponse.java                  ← Envoltorio unificado de respuestas
│   │   └── AppCode.java                      ← Catálogo de códigos del sistema
│   │
│   ├── security/
│   │   └── filter/
│   │       ├── JwtFilter.java                ← Extrae rol y permisos del JWT
│   │       └── SecurityConfig.java           ← Rutas públicas, STATELESS, BCrypt
│   │
│   ├── service/
│   │   ├── JwtService.java                   ← Generación y validación de tokens
│   │   └── OtpService.java                   ← Generación y validación de OTP
│   │
│   └── util/
│       ├── DateUtils.java
│       └── JwtUtil.java                      ← Extrae userId del header Authorization
│
└── module/
    ├── messagemanagement/
    │   ├── application/
    │   │   ├── ConversationService.java
    │   │   └── MessageService.java
    │   ├── controller/
    │   │   ├── ConversationController.java
    │   │   └── MessageController.java
    │   ├── domain/
    │   │   ├── ConversationModel.java
    │   │   └── MessageModel.java
    │   ├── dto/
    │   │   ├── ConversationResponse.java
    │   │   ├── CreateConversationRequest.java
    │   │   ├── MarkReadRequest.java
    │   │   ├── MessageResponse.java
    │   │   └── SendMessageRequest.java
    │   └── persistence/
    │       ├── ConversationRepository.java
    │       └── MessageRepository.java
    │
    └── usermanagement/
        ├── application/AuthService.java
        ├── controller/
        │   ├── AdminController.java
        │   └── AuthController.java
        ├── domain/UserModel.java
        ├── dto/
        │   ├── AuthResponse.java
        │   ├── LoginRequest.java
        │   ├── RegisterRequest.java
        │   └── VerifyOtpRequest.java
        └── persistence/UserRepository.java
```

---

## Modelos de Datos

### `users`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador autogenerado |
| `dni` | VARCHAR(20) | Documento de identidad, único |
| `username` | VARCHAR(50) | Nombre de usuario único, público |
| `email` | VARCHAR(120) | Correo electrónico, único |
| `password` | TEXT | Hash BCrypt (factor 12) |
| `role` | ENUM | `USER`, `ADMIN`, `SUPERADMIN` |
| `status` | ENUM | `PENDING_VERIFICATION`, `ACTIVE`, `BANNED`, `INACTIVE`, `DELETED` |
| `otp_code` | VARCHAR(6) | Código OTP de 6 dígitos (se limpia tras verificar) |
| `otp_expiration` | TIMESTAMP | Expiración del OTP |
| `created_at` | TIMESTAMP | Auditoría de creación |
| `updated_at` | TIMESTAMP | Auditoría de última modificación |
| `updated_by` | BIGINT | ID del admin que realizó el último cambio |

### `conversations`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador autogenerado |
| `user1_id` | BIGINT | Participante con ID menor (garantiza unicidad) |
| `user2_id` | BIGINT | Participante con ID mayor |
| `user1_username` | VARCHAR(50) | Desnormalizado para evitar JOIN |
| `user2_username` | VARCHAR(50) | Desnormalizado para evitar JOIN |
| `user1_avatar` | VARCHAR(500) | URL de avatar |
| `user2_avatar` | VARCHAR(500) | URL de avatar |
| `last_message_preview` | VARCHAR(50) | Vista previa del último mensaje |
| `last_message_sender_id` | BIGINT | ID del emisor del último mensaje |
| `last_message_at` | TIMESTAMP | Timestamp del último mensaje |
| `status` | ENUM | `ONLINE`, `TYPING`, `OFFLINE`, `AWAY` |
| `unread_count_user1` | INT | Mensajes no leídos del usuario 1 |
| `unread_count_user2` | INT | Mensajes no leídos del usuario 2 |
| `updated_at` | TIMESTAMP | Última actualización |

**Regla clave**: siempre `user1Id < user2Id`, lo que garantiza que dos usuarios solo tengan una única fila de conversación sin necesidad de búsqueda dual.

**Índices**: `idx_conv_user1`, `idx_conv_user2`, `idx_conv_updated (last_message_at DESC)`.

### `messages`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador autogenerado |
| `conversation_id` | BIGINT | FK a `conversations` |
| `sender_id` | BIGINT | ID del emisor |
| `receiver_id` | BIGINT | ID del receptor |
| `type` | ENUM | `TEXT`, `PAYLOAD`, `FILE`, `IMAGE` |
| `text_content` | TEXT | Contenido de texto (requerido para TEXT/IMAGE) |
| `payload` | JSONB | Objeto JSON estructurado (para type PAYLOAD) |
| `payload_type` | ENUM | `SALES`, `SYSTEM`, `SURVEY`, `CARD` |
| `file_urls` | JSONB | Array de URLs (máx. 3 archivos, 5 MB c/u) |
| `status` | ENUM | `SENT`, `DELIVERED`, `READ` |
| `read_at` | TIMESTAMP | Momento de lectura |
| `sent_at` | TIMESTAMP | Timestamp de envío (inmutable) |

**Índices**: `idx_msg_conversation (conversation_id, sent_at DESC)`, `idx_msg_sender`, `idx_msg_status`.

---

## Autenticación y Seguridad

### Flujo de registro y activación

```
1. POST /api/auth/register
   → Crea usuario con status PENDING_VERIFICATION
   → Genera OTP de 6 dígitos (válido 10 minutos)
   → Envía correo vía Brevo (si email.enabled = true)
   → En perfil DEV: el OTP aparece en la respuesta (para pruebas)

2. POST /api/auth/verify-otp
   → Valida el OTP y su expiración
   → Activa la cuenta (status → ACTIVE)
   → Limpia el OTP de la base de datos
   → Envía correo de bienvenida
   → Devuelve JWT con rol y permisos

3. POST /api/auth/login
   → Acepta username o email + contraseña
   → Valida estado de la cuenta (no BANNED, no PENDING, no INACTIVE)
   → Devuelve JWT con rol y permisos
```

### Token JWT

El token tiene una validez de **24 horas** y contiene los siguientes claims:

```json
{
  "sub": "nombre_de_usuario",
  "role": "ADMIN",
  "permissions": ["dashboard:ver", "dashboard:editar", "usuarios:ver", "..."],
  "userId": 42,
  "iat": 1234567890,
  "exp": 1234654290
}
```

El algoritmo de firma es **HS256**. La clave secreta se configura vía `jwt.secret` y debe tener mínimo 32 caracteres.

### JwtFilter

El filtro `JwtFilter` intercepta todas las peticiones autenticadas y:
1. Lee el header `Authorization: Bearer <token>`.
2. Extrae el rol (añadido como `ROLE_<nombre>` para `hasRole()`) y los permisos granulares (añadidos individualmente para `hasAuthority()`).
3. Registra la autenticación en el `SecurityContextHolder`.
4. En caso de token expirado o inválido, responde directamente con HTTP 401.

### Contraseñas

Las contraseñas se almacenan con **BCrypt** usando factor de coste 12.

### Rutas públicas

Las siguientes rutas no requieren token:

```
POST /api/auth/register
POST /api/auth/verify-otp
POST /api/auth/login
```

Todas las demás rutas requieren token JWT válido en el header `Authorization`.

---

## Sistema de Roles y Permisos

Los permisos se definen directamente en el enum `Role` y se incluyen en el JWT. Esto permite usar tanto `hasRole()` como `hasAuthority()` en los controladores.

### Tabla de permisos por rol

| Permiso | USER | ADMIN | SUPERADMIN |
|---|:---:|:---:|:---:|
| `profile:ver` | ✅ | ✅ | ✅ |
| `profile:editar` | ✅ | ✅ | ✅ |
| `chat:enviar` | ✅ | ✅ | ✅ |
| `chat:ver` | ✅ | ✅ | ✅ |
| `dashboard:ver` | — | ✅ | ✅ |
| `dashboard:editar` | — | ✅ | ✅ |
| `usuarios:ver` | — | ✅ | ✅ |
| `usuarios:bannear` | — | ✅ | ✅ |
| `usuarios:activar` | — | ✅ | ✅ |
| `usuarios:eliminar` | — | — | ✅ |
| `roles:asignar` | — | — | ✅ |
| `config:ver` | — | — | ✅ |
| `config:editar` | — | — | ✅ |
| `sistema:configurar` | — | — | ✅ |
| `superadmin:panel` | — | — | ✅ |

### Uso en controladores

```java
@PreAuthorize("hasAuthority('dashboard:ver')")
@PreAuthorize("hasAuthority('usuarios:eliminar')")
@PreAuthorize("hasRole('SUPERADMIN')")
@PreAuthorize("hasAnyAuthority('usuarios:bannear', 'usuarios:eliminar')")

```

---

## Endpoints de la API

### Autenticación — `/api/auth`

#### `POST /api/auth/register`

Registra un nuevo usuario. Requiere verificación OTP para activar la cuenta.

**Body:**
```json
{
  "dni": "12345678",
  "username": "nuevo_usuario",
  "email": "usuario@ejemplo.com",
  "password": "contraseña_segura"
}
```

**Respuesta exitosa** (`201 Created`):
```json
{
  "success": true,
  "code": "OK_REGISTER",
  "status": 201,
  "message": "Usuario registrado. Código OTP: 483921 (válido 10 min)",
  "timestamp": "2026-05-12T10:00:00.000000"
}
```

---

#### `POST /api/auth/verify-otp`

Verifica el OTP recibido por correo y activa la cuenta. Devuelve el JWT.

**Body:**
```json
{
  "email": "usuario@ejemplo.com",
  "otpCode": "483921"
}
```

**Respuesta exitosa** (`200 OK`):
```json
{
  "success": true,
  "code": "OK_OTP_VERIFIED",
  "status": 200,
  "message": "Cuenta verificada correctamente",
  "timestamp": "2026-05-12T10:01:00.000000",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "nuevo_usuario",
    "email": "usuario@ejemplo.com",
    "role": "USER",
    "permissions": ["profile:ver", "profile:editar", "chat:enviar", "chat:ver"]
  }
}
```

---

#### `POST /api/auth/login`

Inicia sesión con username o email más contraseña.

**Body:**
```json
{
  "identifier": "nuevo_usuario",
  "password": "contraseña_segura"
}
```

**Respuesta exitosa** (`200 OK`): igual que `verify-otp`.

**Errores posibles:**

| Código | HTTP | Descripción |
|---|---|---|
| `AUTH_BAD_CREDENTIALS` | 401 | Credenciales incorrectas |
| `USER_BANNED` | 403 | Cuenta suspendida |
| `OTP_PENDING_REQUIRED` | 403 | Cuenta sin verificar |
| `USER_INACTIVE` | 403 | Cuenta inactiva |

---

### Administración — `/api/admin`

> Requiere token JWT. Acceso según permisos del rol.

| Método | Endpoint | Permiso requerido | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/dashboard` | `dashboard:ver` | Ver el dashboard |
| `PUT` | `/api/admin/dashboard` | `dashboard:editar` | Editar el dashboard |
| `GET` | `/api/admin/usuarios` | `usuarios:ver` | Listar todos los usuarios |
| `PATCH` | `/api/admin/usuarios/{id}/ban` | `usuarios:bannear` | Banear un usuario |
| `DELETE` | `/api/admin/usuarios/{id}` | `usuarios:eliminar` | Eliminar un usuario |
| `PATCH` | `/api/admin/usuarios/{id}/rol` | `roles:asignar` | Cambiar el rol de un usuario |

---

### Conversaciones — `/api/conversations`

> Requiere token JWT. El `userId` se extrae automáticamente del token.

#### `GET /api/conversations`

Lista todas las conversaciones del usuario autenticado, ordenadas por último mensaje.

**Query params:**
- `page` (default: `0`)
- `size` (default: `20`)

**Headers:**
```
Authorization: Bearer <token>
```

**Respuesta exitosa** (`200 OK`):
```json
{
  "success": true,
  "code": "OK_GENERIC",
  "status": 200,
  "message": "Operación exitosa",
  "timestamp": "2026-05-12T10:00:00.000000",
  "data": {
    "content": [
      {
        "id": 1,
        "otherUserId": 5,
        "otherUsername": "zuno_42",
        "otherAvatar": null,
        "lastMessagePreview": "Hola, ¿cómo estás?",
        "lastMessageAt": "2026-05-12T09:55:00",
        "unreadCount": 2,
        "status": "ONLINE"
      }
    ],
    "totalElements": 15,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

#### `POST /api/conversations`

Crea una conversación con otro usuario. Si ya existe entre ambos, la retorna sin duplicar.

**Body:**
```json
{
  "targetUserId": 5
}
```

**Respuesta exitosa** (`201 Created`): devuelve el objeto `ConversationResponse`.

**Errores posibles:**

| Código | HTTP | Descripción |
|---|---|---|
| `CONV_SELF_CONVERSATION` | 400 | Intentó crear conversación consigo mismo |
| `USER_NOT_FOUND` | 404 | El usuario destino no existe |

---

### Mensajes — `/api/messages`

> Requiere token JWT. El `userId` se extrae automáticamente del token.

#### `GET /api/messages`

Lista los mensajes de una conversación en orden cronológico inverso (más reciente primero), con paginación.

**Query params:**
- `conversationId` (obligatorio)
- `page` (default: `0`)
- `size` (default: `30`)

**Headers:**
```
Authorization: Bearer <token>
```

**Respuesta exitosa** (`200 OK`): página de objetos `MessageResponse`.

---

#### `POST /api/messages`

Envía un mensaje dentro de una conversación existente. El receptor se infiere de la conversación.

**Body — mensaje de texto:**
```json
{
  "conversationId": 1,
  "type": "TEXT",
  "textContent": "Hola, ¿cómo estás?"
}
```

**Body — mensaje con payload estructurado:**
```json
{
  "conversationId": 1,
  "type": "PAYLOAD",
  "payloadType": "SALES",
  "payload": {
    "title": "Oferta especial",
    "price": 99.9,
    "action": "BUY"
  }
}
```

**Body — mensaje con archivos:**
```json
{
  "conversationId": 1,
  "type": "FILE",
  "fileUrls": [
    "https://cdn.ejemplo.com/archivo1.pdf",
    "https://cdn.ejemplo.com/archivo2.docx"
  ]
}
```

> Máximo 3 archivos por mensaje.

**Tipos de mensaje disponibles:**

| Tipo | Descripción |
|---|---|
| `TEXT` | Texto plano. Requiere `textContent`. |
| `PAYLOAD` | Objeto JSON estructurado. Requiere `payload` y `payloadType`. |
| `FILE` | Archivos adjuntos. Requiere `fileUrls` (máx. 3). |
| `IMAGE` | Imagen. Requiere `textContent` (URL de la imagen). |

**Errores posibles:**

| Código | HTTP | Descripción |
|---|---|---|
| `MSG_TEXT_REQUIRED` | 400 | Falta `textContent` para tipo TEXT |
| `MSG_PAYLOAD_REQUIRED` | 400 | Falta `payload` para tipo PAYLOAD |
| `MSG_FILE_REQUIRED` | 400 | Falta al menos un archivo |
| `MSG_FILE_LIMIT` | 400 | Más de 3 archivos adjuntos |
| `CONV_NOT_FOUND` | 404 | Conversación no encontrada |

---

#### `PATCH /api/messages/read`

Marca todos los mensajes recibidos en la conversación como `READ`. Equivale al doble check azul de WhatsApp.

**Body:**
```json
{
  "conversationId": 1
}
```

**Respuesta exitosa** (`200 OK`):
```json
{
  "success": true,
  "code": "OK_GENERIC",
  "status": 200,
  "message": "5 mensaje(s) marcados como leídos",
  "timestamp": "2026-05-12T10:05:00.000000"
}
```

---

### Configuración del sistema — `/api/config`

> Solo accesible para SUPERADMIN (`config:ver`, `config:editar`).

#### `GET /api/config/email`

Consulta si el servicio de envío de correos está habilitado.

**Respuesta:**
```json
{
  "success": true,
  "code": "OK_GENERIC",
  "status": 200,
  "message": "Configuración de correo obtenida",
  "timestamp": "2026-05-12T10:00:00.000000",
  "data": {
    "enabled": true,
    "updatedAt": "2026-05-12T09:00:00",
    "updatedBy": 1
  }
}
```

#### `PUT /api/config/email`

Activa o desactiva el servicio de correo en tiempo de ejecución, sin reiniciar la aplicación.

**Body:**
```json
{
  "enabled": false
}
```

---

## Formato de Respuesta

Todas las respuestas de la API siguen el mismo envoltorio `ApiResponse<T>`:

**Éxito:**
```json
{
  "success": true,
  "code": "OK_LOGIN",
  "status": 200,
  "message": "Sesión iniciada correctamente",
  "timestamp": "2026-05-12T10:00:00.000000",
  "data": { }
}
```

**Error:**
```json
{
  "success": false,
  "code": "OTP_EXPIRED",
  "status": 400,
  "message": "El código OTP ha expirado. Solicita uno nuevo",
  "timestamp": "2026-05-12T10:00:00.000000"
}
```

**Error de validación de campos:**
```json
{
  "success": false,
  "code": "VALID_FIELDS",
  "status": 400,
  "message": "Hay errores en los campos enviados",
  "timestamp": "2026-05-12T10:00:00.000000",
  "errors": {
    "email": "El correo no tiene un formato válido",
    "password": "La contraseña debe tener al menos 8 caracteres"
  }
}
```

---

## Configuración y Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables:
http://localhost:5050/login?next=/
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=zunochat
DB_USER=admin
DB_PASSWORD=admin123
```

El archivo `application-dev.properties` también requiere:

```properties
# JWT
jwt.secret=zunochat_dev_secret_clave_super_segura_2024!

# Email (Brevo)
brevo.api-key=TU_API_KEY_DE_BREVO
brevo.from.email=tu_correo@dominio.com
brevo.from.name=ZunoChat
email.enabled=true
```

### Perfiles de Spring

| Perfil | Archivo | Comportamiento del OTP |
|---|---|---|
| `dev` | `application-dev.properties` | El OTP aparece en la respuesta del registro para facilitar pruebas |
| `prod` | `application-prod.properties` | El OTP solo se envía por correo, nunca se expone en la respuesta |

El perfil activo se configura en `application.properties`:
```properties
spring.profiles.active=dev
```

---

## Ejecución con Docker

El archivo `docker-compose.yml` levanta PostgreSQL 16 y pgAdmin 4:

```bash
docker-compose up -d
```

| Servicio | Puerto | Descripción |
|---|---|---|
| PostgreSQL | `5432` | Base de datos principal |
| pgAdmin 4 | `5050` | Interfaz web de administración |

**Credenciales pgAdmin:**
- Email: `admin@admin.com`
- Password: `admin123`

Una vez levantada la base de datos, ejecutar la aplicación:

```bash
./mvnw spring-boot:run
```

---

## Ejecución Local

**Requisitos previos:**
- Java 21
- Maven 3.x
- PostgreSQL 16 (o Docker)

```bash
# 1. Clonar el repositorio
git clone <url-del-repo>
cd zunochat

# 2. Levantar la base de datos
docker-compose up -d

# 3. Configurar el perfil de desarrollo
# Revisar src/main/resources/application-dev.properties

# 4. Ejecutar la aplicación
./mvnw spring-boot:run

# La API estará disponible en:
# http://localhost:8080
```

---

## Datos Iniciales (DataSeeder)

Al arrancar la aplicación, `DataSeeder` crea automáticamente los siguientes usuarios si no existen:

| Rol | Username | Email | Password |
|---|---|---|---|
| SUPERADMIN | `superadmin` | `superadmin@zunochat.com` | `Admin1234!` |
| ADMIN | `admin1` | `admin1@zunochat.com` | `Admin1234!` |
| ADMIN | `admin2` | `admin2@zunochat.com` | `Admin1234!` |
| USER | `zuno_42`, `chat99`, ... | generados | `Admin1234!` |

Además, genera **200 usuarios** con usernames únicos usando un **Bloom Filter de Google Guava** para garantizar unicidad eficiente sin consultar la base de datos en cada intento:

- Configurado para 10.000 elementos esperados con tasa de falsos positivos del 1%.
- Los usernames siguen patrones como `zuno_42`, `zuno_42x`, `chat.99`, `nova_07z`, generados a partir de 20 raíces latinas con sufijos numéricos y separadores aleatorios.

---

## Colección Bruno

El directorio `zunochat-bruno/` contiene una colección completa para [Bruno](https://www.usebruno.com/), con todas las peticiones organizadas por módulo.

```
zunochat-bruno/
├── environments/
│   └── dev.bru              ← Variables de entorno (base URL)
├── users/
│   ├── auth/
│   │   ├── register.bru
│   │   ├── verify-otp.bru
│   │   └── login.bru
│   └── admin/
│       ├── dashboard-ver.bru
│       ├── usuario-ban.bru
│       ├── usuario-eliminar.bru
│       ├── usuario-rol.bru
│       └── usuarios-listar.bru
└── message/
    ├── crear conversacion.bru
    ├── listar conversacion.bru
    ├── Enviar mensaje.bru
    ├── Mensajes paginados.bru
    └── Marcar como visto.bru
```

Para usar la colección, abrir Bruno, importar la carpeta `zunochat-bruno/` y seleccionar el entorno `dev`.