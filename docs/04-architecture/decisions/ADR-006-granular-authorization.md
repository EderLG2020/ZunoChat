# ADR-006 — Autorización con permisos granulares

**Contexto:** El sistema tiene tres roles (USER, ADMIN, SUPERADMIN) con distintos niveles de acceso a recursos. Se necesita control fino por acción, no solo por rol.

**Problema:** Modelar autorización que permita crecer sin hardcodear roles en cada endpoint.

**Alternativas:**
- Control por rol (`hasRole`) — simple, poco flexible, acoplado a la jerarquía de roles
- ACL (Access Control List) — muy granular, alta complejidad operativa
- Permisos en JWT como `GrantedAuthority` — transportados en el token, evaluables con `@PreAuthorize`

**Decisión:** Permisos granulares (`"dashboard:editar"`, `"usuarios:bannear"`) embebidos en el JWT como claim `permissions`, registrados como `GrantedAuthority` por el `JwtFilter`.

**Consecuencias:**
- Los endpoints usan `@PreAuthorize("hasAuthority('usuarios:bannear')")` en lugar de `hasRole('ADMIN')`
- Añadir un permiso a un rol solo requiere modificar el enum `Role`, sin tocar los controllers
- El token crece ligeramente en tamaño al incluir la lista de permisos
- Los permisos son inmutables durante la sesión (24 h); un cambio de rol no se refleja hasta que el usuario haga login de nuevo
