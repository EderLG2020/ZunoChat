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

### `streaks`
| Campo | Tipo | Descripción |
|---|---|---|
| id | BIGINT PK | Autogenerado |
| conversation_id | BIGINT | Único — una fila por conversación DIRECT con racha activada; no se crea hasta que alguien la solicita |
| user_a_id / user_b_id | BIGINT | `userAId < userBId`, mismo criterio que `conversations` (no indica quién empezó) |
| current_count / longest_count | INT | Días consecutivos actuales / récord histórico |
| last_interaction_date | DATE | Último día (UTC) en que **ambos** escribieron — el que hace avanzar `currentCount` |
| last_message_date_a / last_message_date_b | DATE | Último día (UTC) en que cada lado escribió, por separado, para detectar cuándo se completa el día mutuo |
| enabled | BOOLEAN | Solo `true` cuando ambos aceptaron el opt-in |
| requested_by_user_id | BIGINT | Quién envió la solicitud pendiente; `NULL` si no hay ninguna en curso |
| request_status | ENUM | `NONE`, `PENDING`, `ACCEPTED`, `DECLINED` |
| status | ENUM | `INACTIVE`, `ACTIVE`, `AT_RISK`, `BROKEN` |
| version | BIGINT | Lock optimista de respaldo (la concurrencia real la resuelve un `PESSIMISTIC_WRITE` en el repositorio) |

> No hay migraciones (Flyway/Liquibase) en el proyecto. En dev (`ddl-auto=update`) la tabla se crea sola; en **prod** (`ddl-auto=validate`) hay que aplicar el DDL de `streaks` manualmente antes de desplegar esta versión.

---
