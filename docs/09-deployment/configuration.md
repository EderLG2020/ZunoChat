# Configuración — Escalabilidad y Storage

> Extraído de `README.MD`.

## Escalabilidad horizontal (opcional)

Por defecto el backend está pensado para correr como **una sola instancia**: presencia, sesiones WebSocket y el broker STOMP viven en memoria local. Eso alcanza perfectamente para desarrollo y para producción con un solo nodo.

Si en algún momento se necesita más de una instancia detrás de un load balancer, hay dos interruptores independientes — ambos apagados por defecto, sin ningún efecto en el comportamiento actual:

| Propiedad | Default | Efecto al activarla |
|---|---|---|
| `app.presence.store=redis` | `memory` | Presencia (online/typing/last-seen) y sesiones WS en Redis en vez de `ConcurrentHashMap` — correcto aunque el usuario esté conectado a otra instancia. |
| `app.websocket.relay.enabled=true` | `false` | El broker STOMP pasa de `enableSimpleBroker` (en memoria) a `enableStompBrokerRelay` contra RabbitMQ (plugin STOMP) — todas las instancias comparten el mismo fan-out de mensajes. |

Ambos requieren los servicios correspondientes de `docker-compose.yml` (`redis` / `rabbitmq`, ya incluidos pero no obligatorios para el flujo de un solo nodo). Variables de entorno equivalentes: `PRESENCE_STORE`, `WS_RELAY_ENABLED`, `REDIS_HOST`, `REDIS_PORT`, `WS_RELAY_HOST`, `WS_RELAY_PORT`, `WS_RELAY_LOGIN`, `WS_RELAY_PASSCODE` (ver `application.properties`).

## Subida de archivos

`POST /api/uploads` (multipart, campo `files`, hasta 3 archivos de 5MB c/u) sube imágenes/archivos y devuelve sus URLs, listas para mandar en `SendMessageRequest#fileUrls`. El proveedor real lo elige `app.storage.provider` — el código que lo usa (`UploadController`) no sabe cuál está activo:

| Propiedad | Default | Efecto |
|---|---|---|
| `app.storage.provider=local` | ✅ default | Guarda en disco (`app.storage.local.dir`, default `./uploads`) y lo sirve en `/uploads/**`. Sin cuenta externa — funciona igual en dev que en un despliegue de un solo nodo. Límite: con más de una instancia, o en un host de almacenamiento efímero (la mayoría de PaaS), los archivos no se comparten entre instancias ni sobreviven un redeploy. |
| `app.storage.provider=cloudinary` | — | Sube a [Cloudinary](https://cloudinary.com) — persiste y se comparte entre instancias. Requiere `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` en el entorno (ver `.env.example`). |

Variables de entorno equivalentes: `STORAGE_PROVIDER`, `STORAGE_LOCAL_DIR`, `STORAGE_LOCAL_BASE_URL`, `STORAGE_MAX_FILE_SIZE_MB`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
