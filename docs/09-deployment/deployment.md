Norma

DevOps / SRE

Estructura
1. Ambientes
2. Variables
3. Docker
4. Kubernetes
5. Azure
6. CI/CD
7. Rollback




## Variables de Entorno (`.env`)

```env
# Base de datos
DB_HOST=localhost  DB_PORT=5432  DB_NAME=zunochat  DB_USER=admin  DB_PASSWORD=admin123

# JWT (mín. 32 caracteres)
JWT_SECRET=zunochat_dev_secret_clave_super_segura_2024

# Email
BREVO_API_KEY=TU_API_KEY

# Redis
REDIS_HOST=localhost  REDIS_PORT=6379  REDIS_PASSWORD=redis123

# RabbitMQ
RABBITMQ_HOST=localhost  RABBITMQ_PORT=5672  RABBITMQ_USERNAME=rabbit  RABBITMQ_PASSWORD=rabbit123  RABBITMQ_VHOST=zunochat

# Feature flags (false = implementación en memoria, no requiere los servicios)
REDIS_ENABLED=false
RABBITMQ_ENABLED=false
```

---

## Docker

```bash
# Solo PostgreSQL (modo dev)
docker compose up -d

# Con Redis
docker compose --profile redis up -d

# Con RabbitMQ
docker compose --profile rabbitmq up -d

# Todo
docker compose --profile redis --profile rabbitmq up -d
```