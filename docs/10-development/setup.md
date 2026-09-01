# Setup — Inicio Rápido

> Extraído de `README.MD`.

```bash
git clone <repo> && cd zunochat
cp .env.example .env        # editar valores
docker compose up -d        # levantar BD
./mvnw spring-boot:run      # API en http://localhost:8080
```

**Perfiles:** `dev` (OTP visible en respuesta) · `prod` (OTP solo por correo)

> `spring-boot:run` no carga `.env` solo — son variables reales del sistema operativo. Antes de correrlo en una terminal (fuera del IDE), expórtalas: `set -a && source .env && set +a && ./mvnw spring-boot:run` (bash) o el equivalente en PowerShell. Si usas IntelliJ, el plugin EnvFile (o similar) ya se encarga de esto.

## Servicios y credenciales de desarrollo

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

## Datos de prueba (dev)

Ver `06-database/seed-data.md` para los usuarios, conversaciones y mensajes que siembra `DataSeeder` en el perfil `dev`.
