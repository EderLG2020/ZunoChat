# Conversaciones y Mensajes

## Conversaciones — `/api/conversations`

**GET** `/api/conversations?page=0&size=20` — lista ordenada por último mensaje.

**POST** `/api/conversations` — crea o retorna conversación existente.
```json
{ "targetUserId": 5 }
```

---

## Mensajes — `/api/messages`

**GET** `/api/messages?conversationId=1&page=0&size=30`

**POST** `/api/messages`
```js
{ "conversationId": 1, "type": "TEXT", "textContent": "Hola" }
{ "conversationId": 1, "type": "PAYLOAD", "payloadType": "SALES", "payload": { ... } }

{ "conversationId": 1, "type": "FILE", "fileUrls": ["https://..."] }
```

**PATCH** `/api/messages/read` — `{ "conversationId": 1 }`

**WebSocket:** ver `04-architecture/architecture.md` para los destinos STOMP de mensajería en tiempo real.
