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
```properties
# application.properties
spring.profiles.active=dev
```
