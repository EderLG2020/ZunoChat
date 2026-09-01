# Documentación — ZunoChat Backend

Reorganizado desde una carpeta plana (mayúsculas, estilo C4/ADR) a esta estructura numerada. El dominio de ZunoChat es **mensajería 1-a-1**, no un marketplace: donde la plantilla original traía `products/offers/reservations`, aquí se usa `02-domain/` con las reglas reales (auth, conversaciones, mensajes, racha).

| Documentación | Pregunta | Estado |
| --- | --- | --- |
| [01-requirements](01-requirements/) | ¿Qué necesita el usuario/negocio? | Parcial — inferido del código, sin levantamiento formal |
| [02-domain](02-domain/) | ¿Qué reglas debe cumplir el dominio? | ✅ |
| [03-workflows](03-workflows/) | ¿Cuál es el flujo del proceso? | ✅ (auth, mensajería, racha) |
| [04-architecture](04-architecture/) | ¿Cómo está organizado el sistema? | ✅ C4 (context/containers/components) + ADRs |
| [05-api](05-api/) | ¿Cómo se comunican los sistemas? | ✅ |
| [06-database](06-database/) | ¿Cómo almacenamos la información? | Parcial — sin ER diagram, sin migraciones formales |
| [07-security](07-security/) | ¿Quién puede hacer qué? | Parcial — solo auth/authz; resto pendiente |
| [08-testing](08-testing/) | ¿Cómo verificamos que funciona? | ❌ Pendiente |
| [09-deployment](09-deployment/) | ¿Cómo llevamos el sistema a un entorno? | ✅ vars/config, ❌ ambientes formales |
| [10-development](10-development/) | ¿Cómo trabajan los desarrolladores? | Parcial — solo setup |

## Mapeo con la documentación anterior

| Antes (plano) | Ahora |
| --- | --- |
| `ADR.MD` | `04-architecture/decisions/ADR-001..009-*.md` (uno por decisión) |
| `API.md` | `05-api/*.md` (overview, authentication, admin, chat, streaks, config) |
| `ARCHITECTURE.md` | `04-architecture/architecture.md` |
| `COMPONENTS.md` | `04-architecture/components.md` |
| `CONTAINERS.md` | `04-architecture/containers.md` |
| `CONTEXT.md` | `04-architecture/context.md` |
| `DATASEEDER.md` | `06-database/seed-data.md` |
| `DATA_MODEL.md` | `06-database/database-design.md` |
| `DEPLOYMENT.md` | `09-deployment/deployment.md` |
| `ESPECIFICATIOUI.md` | `01-requirements/ui-ux-specification.md` |
| `OPERATIONS.md` | `09-deployment/operations.md` |
| `SECURITY.md` | `07-security/authentication.md` + `authorization.md` |
| `TESTING.md` | `08-testing/testing-strategy.md` |
| `TROUBLESHOOTING.md` | `09-deployment/troubleshooting.md` |

Todos los archivos marcados `TODO` en sus carpetas son huecos reales de documentación, no contenido movido — no existían antes de esta reorganización.
