
## DataSeeder

`common/config/DataSeeder.java` — corre automáticamente al iniciar el backend **solo con `spring.profiles.active=dev`** (`@Profile("dev")`, valor por defecto en `application.properties`). No corre en `prod`.

### Usuarios

| Rol | Username | Email | Contraseña |
|---|---|---|---|
| SUPERADMIN | superadmin | superadmin@zunochat.com | Super@2024! |
| ADMIN | admin1 / admin2 | admin1@zunochat.com / admin2@zunochat.com | Admin@2024! |
| USER | ×200 (DataFaker ES) | `<nombre>_<random>@mail.com` | User@2024! |

Todos (incluidos superadmin/admins) reciben una foto de perfil real vía `avatar` — URL de `https://i.pravatar.cc/300?img=N` (servicio gratuito, sin API key; se referencia la URL, no se descarga nada).

### Conversaciones y mensajes

Además de los usuarios, siembra **40 conversaciones** entre usuarios USER al azar, con **5 a 20 mensajes cada una** (frases en español tipo chat, `MessageType.TEXT`), fechas repartidas en los últimos ~20 días, y contadores de no leídos/estado del último mensaje ya calculados — así el listado de conversaciones y el chat no arrancan vacíos.

### Idempotencia

| Qué | Regla |
|---|---|
| superadmin / admin1 / admin2 | Se saltea si el username ya existe |
| Usuarios USER | Completa hasta 200 si faltan; no duplica |
| Conversaciones + mensajes | Solo corre si `conversationRepository.count() == 0` — **no completa** en reinicios posteriores |

Esto significa que si ya tenías una base con los 200 usuarios pero sin conversaciones, un reinicio las agrega. Pero si ya tenías usuarios seedeados **antes** de que existiera el campo `avatar`, esos usuarios no se actualizan retroactivamente (solo los que se crean de cero reciben avatar).

### Resetear para re-seedear desde cero

```bash
cd backend
docker compose down -v   # borra el volumen de Postgres
docker compose up -d
# levantar el backend con perfil dev
```

---
