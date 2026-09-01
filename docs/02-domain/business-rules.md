# Reglas de Negocio

> Dominio de ZunoChat: mensajería 1-a-1. No hay conceptos de marketplace (productos, ofertas, reservas) — se documentan aquí las reglas reales del sistema, extraídas de las decisiones de arquitectura y del modelo de datos.

## Identidad y cuentas
- Un usuario se identifica de forma única por `dni`, `username` y `email` (los tres son `UNIQUE`).
- Una cuenta nueva nace en `PENDING_VERIFICATION` y solo pasa a `ACTIVE` tras verificar un OTP de 6 dígitos (válido 10 min).
- Login admite `username` **o** `email` como identificador.
- Una cuenta `BANNED` o `INACTIVE` no puede iniciar sesión.

## Roles y permisos
- Tres roles: `USER`, `ADMIN`, `SUPERADMIN`.
- La autorización es por **permiso granular** (ej. `usuarios:bannear`), no por rol crudo — ver `04-architecture/decisions/ADR-006-granular-authorization.md` y la tabla completa en `07-security/authorization.md`.
- Los permisos viajan embebidos en el JWT y son inmutables durante la sesión (24 h): un cambio de rol no aplica hasta el siguiente login.

## Conversaciones
- Cada conversación `DIRECT` es única por par de usuarios; la invariante `user1Id < user2Id` evita duplicados sin necesidad de búsqueda dual.
- Los usernames de ambos participantes se desnormalizan en `conversations` para listar el inbox sin JOINs — ver `04-architecture/decisions/ADR-007-username-denormalization.md`.

## Mensajes
- Cuatro tipos: `TEXT`, `IMAGE`, `FILE` (hasta 3 `fileUrls`), `PAYLOAD` (con `payloadType`: `SALES`, `SYSTEM`, `SURVEY`, `CARD`).
- Estado de entrega: `SENT` → `DELIVERED` → `READ`.

## Racha (Streak)
- Solo aplica a conversaciones `DIRECT` (no `GROUP`).
- Cuenta los días consecutivos (UTC) en que **ambos** participantes enviaron al menos un mensaje.
- Activación por **opt-in mutuo**: cualquiera activa el toggle → se crea una solicitud `PENDING` → la racha solo empieza a contar cuando el otro acepta. Si el otro ya la había solicitado, se acepta automáticamente.
- Desactivación es unilateral e inmediata, sin necesidad de confirmación del otro.
- Reglas completas de transición (`INCREMENT` / `RESET` / `NONE`, `AT_RISK` / `BROKEN`) en `04-architecture/decisions/ADR-009-streak-mutual-optin.md` y en `04-architecture/components.md` (módulo `streak`).

## Notificaciones por email
- Un fallo del proveedor de email (Brevo) **nunca** debe romper el flujo de negocio que lo dispara (registro, ban, reset de contraseña) — se captura y solo se loguea.
- El envío de correos es un flag activable en caliente (`email.enabled` en `app_config`), sin redeploy.
