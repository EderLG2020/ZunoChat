# Autenticación

## Flujo de registro
```
1. POST /api/auth/register  → crea usuario PENDING_VERIFICATION, genera OTP (10 min)
2. POST /api/auth/verify-otp → activa cuenta, retorna JWT
3. POST /api/auth/login      → retorna JWT
```

> En perfil **dev**, el OTP aparece en el campo `message` de la respuesta. En **prod**, solo se envía por correo.

## JWT (HS256, 24 h)
```json
{
  "sub": "nombre_usuario",
  "role": "ADMIN",
  "permissions": ["dashboard:ver", "chat:enviar", "..."],
  "userId": 42
}
```

## Autenticación WebSocket (2 capas)
1. **HTTP Handshake**: `ws://localhost:8080/ws?token=<jwt>`
2. **Frame STOMP CONNECT**: header `Authorization: Bearer <token>` (fallback a query param)
