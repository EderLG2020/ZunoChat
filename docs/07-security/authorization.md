# Roles y Permisos

| Permiso | USER | ADMIN | SUPERADMIN |
|---|:---:|:---:|:---:|
| profile:ver / editar | ✅ | ✅ | ✅ |
| chat:enviar / ver | ✅ | ✅ | ✅ |
| dashboard:ver / editar | — | ✅ | ✅ |
| usuarios:ver / bannear / activar | — | ✅ | ✅ |
| usuarios:eliminar | — | — | ✅ |
| roles:asignar | — | — | ✅ |
| config:ver / editar / sistema:configurar | — | — | ✅ |

Modelo completo de permisos con `@PreAuthorize`: ver `04-architecture/decisions/ADR-006-granular-authorization.md`.
