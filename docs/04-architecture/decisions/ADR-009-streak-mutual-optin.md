# ADR-009 — Activación de racha por opt-in mutuo

**Contexto:** Se implementa una "racha" (streak) al estilo Snapchat: cuenta los días consecutivos en que dos usuarios de una conversación `DIRECT` intercambiaron al menos un mensaje cada uno.

**Problema:** Decidir si la racha se cuenta automáticamente para toda conversación `DIRECT` desde el primer mensaje, o si requiere una acción explícita de ambos usuarios.

**Alternativas:**
- Auto-activar para toda conversación `DIRECT` — cero fricción, pero cuenta rachas para conversaciones donde ninguno de los dos quiere ese seguimiento (presión social no deseada)
- Toggle unilateral por usuario, sin confirmación del otro — simple, pero un usuario puede activar seguimiento sobre una interacción que el otro no quiere exponer
- Opt-in mutuo: cualquiera activa el toggle en Configuración → se envía una solicitud al otro dentro del chat → la racha solo cuenta si ambos aceptan

**Decisión:** Opt-in mutuo. Activar (`PATCH /api/streaks/{id} {enabled:true}`) crea una solicitud (`requestStatus=PENDING`); si el otro ya la había pedido, se acepta automáticamente (evita fricción cuando ambos activan el switch casi al mismo tiempo). Desactivar, en cambio, es unilateral e inmediato para cualquiera de los dos — no requiere confirmación.

**Consecuencias:**
- La tabla `streaks` no se crea para una conversación hasta que alguien solicita activarla (`StreakService#requestActivation`), a diferencia de `conversations`/`messages` que existen desde el primer mensaje
- El frontend necesita una UI de solicitud/aceptación dentro del chat, no solo un switch de Configuración
- El broadcast de estos eventos (`REQUEST_SENT`, `REQUEST_ACCEPTED`, `REQUEST_DECLINED`, `INCREMENTED`, `RESET`, `AT_RISK`, `BROKEN`, `DISABLED`) usa un tópico STOMP dedicado, `/topic/streak.{conversationId}`, en lugar de reutilizar `/topic/conversation.{id}` — mismo criterio que ya separa `/topic/typing.{id}` y `/topic/read.{id}` de los mensajes
