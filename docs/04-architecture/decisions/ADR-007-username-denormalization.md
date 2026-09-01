# ADR-007 — Desnormalización de usernames en conversaciones

**Contexto:** Al listar el inbox de conversaciones, se necesita mostrar el nombre del otro participante. Hay miles de consultas de este tipo.

**Problema:** Evitar JOINs costosos a la tabla `users` en cada listado de conversaciones.

**Alternativas:**
- JOIN `conversations` → `users` en cada query — correcto en diseño, costoso a escala
- Caché de usuarios en Redis — añade invalidación, complejidad de coherencia
- Desnormalizar `user1_username` y `user2_username` en la tabla `conversations` — redundancia controlada

**Decisión:** Desnormalizar usernames (y avatar cuando se implemente) directamente en `conversations`.

**Consecuencias:**
- El listado del inbox es una query de tabla única, sin JOINs
- Si un usuario cambia su username, hay que actualizar todas sus conversaciones (actualmente no implementado el cambio de username, mitigando el riesgo)
- La invariante `user1Id < user2Id` garantiza unicidad del par sin necesidad de búsqueda dual ni índice compuesto adicional
