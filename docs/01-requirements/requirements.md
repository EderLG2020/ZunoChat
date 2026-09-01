# Requirements

> Extraído de `README.MD` y del resumen ejecutivo de `ui-ux-specification.md` (ambos ya existentes). No existía un documento de requisitos formal — este es un punto de partida, no un levantamiento nuevo.

## Resumen

**ZunoChat** es una plataforma de mensajería en tiempo real. El backend (Spring Boot 3 · Java 21 · PostgreSQL 16 · Redis 7.2 · RabbitMQ 3.13) está completamente implementado para mensajería 1-a-1. El frontend (móvil y web admin) se especifica en `ui-ux-specification.md` pero su estado de implementación real debe verificarse en `client/` y `zunochat-movil-app/`.

## Características principales (declaradas en README)

- Registro y autenticación JWT
- Verificación OTP por correo
- Mensajería en tiempo real
- Conversaciones privadas
- Presencia de usuarios
- Notificaciones
- Arquitectura modular
- Redis y RabbitMQ opcionales (feature flags, in-memory por defecto en dev)

## Inventario de módulos y estado (según `ui-ux-specification.md`)

| # | Módulo | Estado API |
|---|---|---|
| 1 | Autenticación | ✅ Completo |
| 2 | Conversaciones | ✅ Completo |
| 3 | Mensajería | ✅ Completo |
| 4 | Presencia / Tiempo real | ✅ Completo |
| 5 | Gestión de Usuarios | ✅ Completo |
| 6 | Búsqueda de Usuarios | ✅ Completo |
| 7 | Roles y Permisos | ✅ Completo |
| 8 | Configuración del Sistema | ✅ Completo |
| 9 | Perfil de Usuario | ⚠️ Parcial (campos en DB, sin endpoints PATCH) |
| 10 | Archivos/Media | ⚠️ Parcial (URLs aceptadas, sin upload propio — ver nota) |
| 11 | Notificaciones Push | ❌ No implementado |
| 12 | Dashboard Admin | ⚠️ Placeholder (endpoint existe, sin datos reales) |
| 13 | Auditoría/Logs | ❌ No implementado |
| 14 | Chats Grupales | ❌ No implementado |
| 15 | Recuperación de Contraseña | ⚠️ Email template existe, endpoint no implementado |

> Nota: el README documenta `POST /api/uploads` para subir imágenes/archivos (local o Cloudinary vía `app.storage.provider`), lo que puede haber resuelto parcialmente el punto 10 después de que se escribiera `ui-ux-specification.md`. Verificar contra el código antes de tomar esta tabla como estado actual.

## Pendiente

- [ ] Levantamiento formal de requisitos (no existe ninguno — esta tabla es un proxy inferido del código y specs existentes)
- [ ] `user-stories.md` — no existe
- [ ] `acceptance-criteria.md` — no existe
