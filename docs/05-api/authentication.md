# Auth — `/api/auth` (público)

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
