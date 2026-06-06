API
Norma

OpenAPI

Archivos
api/
├── AUTH_API.md
├── USERS_API.md
├── CHAT_API.md
└── WEBSOCKET_API.md

Descripción

Endpoint

Request

Response

Errores

Ejemplos



### Config — `/api/config` (solo SUPERADMIN)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/email` | Estado del servicio de email |
| PUT | `/email` | Activar/desactivar en caliente |

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
