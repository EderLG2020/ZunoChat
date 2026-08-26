1. Nombre del proyecto
2. Descripción
3. Características principales
4. Arquitectura resumida
5. Stack tecnológico
6. Inicio rápido
7. Estructura del repositorio
8. Documentación
9. Contribución
10. Licencia


# ZunoChat Backend

Backend de mensajería en tiempo real construido con Spring Boot, PostgreSQL y WebSocket STOMP.

## Características

- Registro y autenticación JWT
- Verificación OTP por correo
- Mensajería en tiempo real
- Conversaciones privadas
- Presencia de usuarios
- Notificaciones
- Arquitectura modular
- Redis y RabbitMQ opcionales

## Arquitectura

Cliente
↓
REST API + WebSocket
↓
Spring Boot
↓
PostgreSQL

Componentes opcionales:

- Redis
- RabbitMQ

## Stack Tecnológico

| Tecnología | Versión |
|------------|----------|
| Java | 21 |
| Spring Boot | 3.3.2 |
| PostgreSQL | 16 |
| Redis | 7 |
| RabbitMQ | 3 |
| Docker | Latest |

## Inicio Rápido

```bash
git clone <repo>
cd zunochat

cp .env.example .env

docker compose up -d

./mvnw spring-boot:run

```


---
---



---



| Servicio | URL | Credenciales |
|---|---|---|
| API REST | http://localhost:8080 | — |
| pgAdmin 4 | http://localhost:5050 | admin@admin.com / admin123 |
| RedisInsight | http://localhost:5540 | — |
| RabbitMQ Management | http://localhost:15672 | rabbit / rabbit123 |

### Conexión a PostgreSQL desde pgAdmin

> pgAdmin corre dentro de Docker, por lo que **no puede usar `localhost`** para llegar a la base de datos. Usar `host.docker.internal` en su lugar.

| Campo | Valor |
|---|---|
| Host | `host.docker.internal` |
| Port | `5432` |
| Database | `zunochat` |
| Username | `admin` |
| Password | `admin123` |
 
---

## Ejecución Local

```bash
git clone <repo> && cd zunochat
cp .env.example .env        # editar valores
docker compose up -d        # levantar BD
./mvnw spring-boot:run      # API en http://localhost:8080
```

**Perfiles:** `dev` (OTP visible en respuesta) · `prod` (OTP solo por correo)
```

> `spring-boot:run` no carga `.env` solo — son variables reales del sistema
> operativo. Antes de correrlo en una terminal (fuera del IDE), expórtalas:
> `set -a && source .env && set +a && ./mvnw spring-boot:run` (bash) o el
> equivalente en PowerShell. Si usas IntelliJ, el plugin EnvFile (o similar)
> ya se encarga de esto.

## Escalabilidad horizontal (opcional)

Por defecto el backend está pensado para correr como **una sola instancia**:
presencia, sesiones WebSocket y el broker STOMP viven en memoria local. Eso
alcanza perfectamente para desarrollo y para producción con un solo nodo.

Si en algún momento necesitas más de una instancia detrás de un load
balancer, hay dos interruptores independientes — ambos apagados por
defecto, sin ningún efecto en el comportamiento actual:

| Propiedad | Default | Efecto al activarla |
|---|---|---|
| `app.presence.store=redis` | `memory` | Presencia (online/typing/last-seen) y sesiones WS en Redis en vez de `ConcurrentHashMap` — correcto aunque el usuario esté conectado a otra instancia. |
| `app.websocket.relay.enabled=true` | `false` | El broker STOMP pasa de `enableSimpleBroker` (en memoria) a `enableStompBrokerRelay` contra RabbitMQ (plugin STOMP) — todas las instancias comparten el mismo fan-out de mensajes. |

Ambos requieren los servicios correspondientes de `docker-compose.yml`
(`redis` / `rabbitmq`, ya incluidos pero no obligatorios para el flujo de un
solo nodo). Variables de entorno equivalentes: `PRESENCE_STORE`,
`WS_RELAY_ENABLED`, `REDIS_HOST`, `REDIS_PORT`, `WS_RELAY_HOST`,
`WS_RELAY_PORT`, `WS_RELAY_LOGIN`, `WS_RELAY_PASSCODE` (ver
`application.properties`).

## Subida de archivos

`POST /api/uploads` (multipart, campo `files`, hasta 3 archivos de 5MB c/u)
sube imágenes/archivos y devuelve sus URLs, listas para mandar en
`SendMessageRequest#fileUrls`. El proveedor real lo elige `app.storage.provider`
— el código que lo usa (`UploadController`) no sabe cuál está activo:

| Propiedad | Default | Efecto |
|---|---|---|
| `app.storage.provider=local` | ✅ default | Guarda en disco (`app.storage.local.dir`, default `./uploads`) y lo sirve en `/uploads/**`. Sin cuenta externa — funciona igual en dev que en un despliegue de un solo nodo. Límite: con más de una instancia, o en un host de almacenamiento efímero (la mayoría de PaaS), los archivos no se comparten entre instancias ni sobreviven un redeploy. |
| `app.storage.provider=cloudinary` | — | Sube a [Cloudinary](https://cloudinary.com) — persiste y se comparte entre instancias. Requiere `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` en el entorno (ver `.env.example`). |

Variables de entorno equivalentes: `STORAGE_PROVIDER`, `STORAGE_LOCAL_DIR`,
`STORAGE_LOCAL_BASE_URL`, `STORAGE_MAX_FILE_SIZE_MB`,
`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.

```properties
# application.properties
spring.profiles.active=dev
```
# application.properties
spring.profiles.active=dev
```
