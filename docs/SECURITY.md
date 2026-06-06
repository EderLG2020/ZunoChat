
Norma

Basada en:

OWASP
ISO 27001
Estructura
1. Autenticación
2. Autorización
3. Gestión de secretos
4. JWT
5. OAuth2
6. Rate limiting
7. Protección CSRF
8. Protección XSS
9. Protección SQL Injection
10. Auditoría



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
