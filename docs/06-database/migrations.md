# Migraciones

> Nota extraída de `database-design.md`:

No hay migraciones (Flyway/Liquibase) en el proyecto. En dev (`ddl-auto=update`) la tabla se crea sola; en **prod** (`ddl-auto=validate`) hay que aplicar el DDL de cada tabla nueva manualmente antes de desplegar (ejemplo puntual: la tabla `streaks`).

TODO: si se introduce una herramienta de migraciones, documentar aquí la estrategia (versionado, rollback, cómo correrlas en cada entorno).
