# Flujo de Mensajería en Tiempo Real

Extraído de `04-architecture/components.md` (módulo `chat`).

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CAPA REST (historial)                       │
│                                                                     │
│  GET  /api/conversations          → lista conversaciones paginadas  │
│  POST /api/conversations          → crea o retorna conversación     │
│  GET  /api/conversations/{id}/messages → historial paginado         │
│  POST /api/messages               → envía mensaje (REST)            │
│  POST /api/messages/mark-read     → marca mensajes como leídos      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   CAPA REAL-TIME (WebSocket / STOMP)                │
│                                                                     │
│  CONNECT  /ws  ──────────────────────────────────────────────────┐  │
│                  JwtHandshakeInterceptor valida JWT               │  │
│                  StompAuthChannelInterceptor carga Principal      │  │
│                  WebSocketSessionRegistry registra sessionId      │  │
│                  PresenceService.markOnline(userId) → Redis       │  │
│                  RabbitMQ publica PresenceEvent (online=true)     │  │
│                                                                   │  │
│  SEND /app/chat.send ────────────────────────────────────────┐   │  │
│        │                                                      │   │  │
│   [WebSocketController]                                       │   │  │
│        ├─ MessageService.sendMessage() → persiste en MySQL    │   │  │
│        └─ MessageProducer.publishMessage()                    │   │  │
│                └─► RabbitMQ exchange "chat.exchange"          │   │  │
│                         └─► MessageConsumer                   │   │  │
│                                 └─► SimpMessagingTemplate     │   │  │
│                                       └─► /queue/user.{id}   │   │  │
│                                                               │   │  │
│  SEND /app/chat.typing → publica TypingEvent                 │   │  │
│        └─► /topic/typing.{conversationId}                    │   │  │
│                                                               │   │  │
│  SEND /app/chat.read → markAsRead + ReadReceiptRabbitEvent   │   │  │
│  SEND /app/heartbeat → renueva TTL en Redis (65 seg)         │   │  │
│                                                               │   │  │
│  DISCONNECT  ─────────────────────────────────────────────┐  │   │  │
│                SessionRegistry.removeSession()            │  │   │  │
│                Si no quedan sesiones:                     │  │   │  │
│                  PresenceService.markOffline() → Redis    │  │   │  │
│                  RabbitMQ publica PresenceEvent           │  │   │  │
│                  (online=false, lastSeen=epoch_ms)        │  │   │  │
└─────────────────────────────────────────────────────────────────────┘
```

Detalle de clases y componentes involucrados: `04-architecture/components.md` (módulo 3). Endpoints REST y STOMP: `05-api/chat.md` y `04-architecture/architecture.md`.
