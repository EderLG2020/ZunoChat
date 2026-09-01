# Racha (Streaks) — `/api/streaks` (requiere JWT)

Solo aplica a conversaciones `DIRECT`. Opt-in mutuo: activar (`enabled: true`) envía una solicitud dentro del chat; la racha solo empieza a contar cuando el otro participante también acepta. Desactivar (`enabled: false`) es unilateral e inmediato, sin confirmación del otro. Ver decisión completa en `04-architecture/decisions/ADR-009-streak-mutual-optin.md`.

| Método | Endpoint | Body | Descripción |
|---|---|---|---|
| GET | `/{conversationId}` | — | Estado actual de la racha |
| PATCH | `/{conversationId}` | `{ "enabled": true }` | Envía/acepta solicitud de activación (ver nota abajo) |
| PATCH | `/{conversationId}` | `{ "enabled": false }` | Desactiva de inmediato |
| POST | `/{conversationId}/respond` | `{ "accept": true }` | Acepta o rechaza una solicitud pendiente del otro usuario |

> Si el otro usuario ya había enviado una solicitud (`requestStatus=PENDING`) y tú también activas el switch, se toma como aceptación automática — no hace falta pasar por `/respond`.

**Response** (`StreakResponse`):
```json
{
  "conversationId": 1,
  "enabled": true,
  "currentCount": 4,
  "longestCount": 7,
  "lastInteractionDate": "2026-08-28",
  "status": "AT_RISK",
  "requestStatus": "ACCEPTED",
  "requestedByUserId": 5
}
```
`status`: `INACTIVE` · `ACTIVE` · `AT_RISK` · `BROKEN`. `requestStatus`: `NONE` · `PENDING` · `ACCEPTED` · `DECLINED`.

**Errores:**

| Código | HTTP | Descripción |
|---|---|---|
| `STREAK_NOT_DIRECT` | 400 | La racha solo aplica a conversaciones directas (no grupos) |
| `STREAK_NO_PENDING_REQUEST` | 400 | No hay solicitud pendiente que responder |
| `STREAK_OWN_REQUEST` | 400 | No puedes aceptar/rechazar tu propia solicitud |

**WebSocket:** suscribirse a `/topic/streak.{conversationId}` — ver `04-architecture/architecture.md`.
