Norma

Basado en IEEE e ingeniería de datos.

Objetivo

Documentar entidades.

Estructura
Entidad
Descripción
Atributos
Relaciones
Reglas



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
