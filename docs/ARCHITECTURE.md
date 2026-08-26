Norma

ISO/IEC/IEEE 42010

Objetivo

Describir cómo está construido el sistema.

Estructura
1. Objetivo
2. Alcance
3. Stakeholders
4. Actores
5. Contexto del sistema
6. Componentes
7. Integraciones externas
8. Diagramas
9. Decisiones arquitectónicas
10. Restricciones
11. Riesgos


## WebSocket / STOMP

**Endpoint:** `ws://localhost:8080/ws` · Fallback SockJS: `http://localhost:8080/ws`

### Publicación (cliente → servidor)

| Destino | Body |
|---|---|
| `/app/chat.send` | `{ conversationId, type, textContent?, payload?, fileUrls? }` |
| `/app/chat.typing` | `{ conversationId, typing: true/false }` |
| `/app/chat.read` | `{ conversationId }` |
| `/app/heartbeat` | `""` (cada 60 s) |

### Suscripción (servidor → cliente)

| Destino | Evento |
|---|---|
| `/topic/conversation.{id}` | Nuevo mensaje |
| `/topic/typing.{id}` | Indicador de escritura |
| `/topic/read.{id}` | Confirmación de lectura |
| `/topic/presence.{userId}` | Usuario online/offline |
| `/user/queue/notifications` | Notificación personal |

### Conexión
```js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS(`http://localhost:8080/ws?token=${token}`),
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe('/topic/conversation.42', frame => console.log(JSON.parse(frame.body)));
    setInterval(() => client.publish({ destination: '/app/heartbeat', body: '' }), 60000);
  },
});
client.activate();
```

---


## Arquitectura

```
HTTP/WebSocket Request
       │
  ┌────┴────┐
JwtFilter   JwtHandshakeInterceptor (WS handshake ?token=)
       │         │
       │    StompAuthChannelInterceptor (STOMP CONNECT)
       │         │
  Controllers    WebSocketController
  (Auth, Admin,  (@MessageMapping)
  Conversation,       │
  Message,    ┌───────┴───────┐
  AppConfig)  IPresenceService  IMessageProducer
              (en memoria)      (broadcast directo)
                     │
              Services → Repositories → PostgreSQL
```

**Principios clave:**
- Interfaces `IPresenceService`, `IWebSocketSessionRegistry`, `IMessageProducer` con una única implementación en memoria/directa — pensado para una sola instancia del backend (sin Redis/RabbitMQ).
- Respuesta unificada `ApiResponse<T>` en todos los endpoints.
- Stateless: el estado de autenticación viaja completamente en el JWT.
- `Conversation` desnormaliza usernames para evitar JOINs frecuentes.

---