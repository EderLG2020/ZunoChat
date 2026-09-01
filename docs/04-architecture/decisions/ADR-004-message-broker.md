# ADR-004 — Broker de mensajería

**Contexto:** Con WebSocket, los mensajes deben enrutarse al destinatario correcto, potencialmente conectado a otra instancia del servidor.

**Problema:** Desacoplar el envío de un mensaje de su entrega al receptor.

**Alternativas:**
- Broker embebido de Spring — simple, solo funciona con una instancia
- Kafka — muy alto throughput, mayor operación, overkill para chat 1-a-1
- RabbitMQ — broker robusto, soporte a STOMP nativo, bajo overhead para mensajería dirigida

**Decisión:** RabbitMQ 3.13 con exchange `chat.exchange`, activable mediante flag `rabbitmq.enabled`.

**Consecuencias:**
- En dev (`rabbitmq.enabled=false`) se usa `DirectMessageProducer` que despacha directo al broker STOMP embebido, sin dependencias externas
- En prod (`rabbitmq.enabled=true`) se usa `MessageProducer` → RabbitMQ → `MessageConsumer` → cliente
- La interfaz `IMessageProducer` permite intercambiar la implementación sin tocar el dominio
- Añade un contenedor más a operar y monitorear
