# ADR-003 — Mensajería en tiempo real

**Contexto:** ZunoChat requiere entrega de mensajes, indicadores de escritura y presencia en tiempo real entre usuarios conectados.

**Problema:** Elegir el protocolo de comunicación en tiempo real.

**Alternativas:**
- Polling — simple, alto consumo de red, latencia variable
- SSE (Server-Sent Events) — unidireccional, no apto para envío desde cliente
- WebSocket puro — bidireccional, sin protocolo de mensajería sobre él
- WebSocket + STOMP — bidireccional, protocolo de mensajería con destinos y suscripciones

**Decisión:** WebSocket con STOMP sobre SockJS.

**Consecuencias:**
- Mayor complejidad de configuración (handshake, interceptores, session registry)
- Autenticación en dos capas: HTTP handshake (`?token=`) + frame STOMP `CONNECT`
- SockJS provee fallback HTTP para entornos que bloquean WebSocket
- Destinos STOMP (`/app/`, `/topic/`, `/queue/`) dan estructura clara al protocolo
