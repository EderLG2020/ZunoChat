# Flujo de Autenticación (registro → verificación → login)

Extraído de `04-architecture/components.md` (módulo `auth`).

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

Detalle de clases y componentes involucrados: `04-architecture/components.md` (módulo 1). Endpoints: `05-api/authentication.md`.
