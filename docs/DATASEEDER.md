
## DataSeeder

Al iniciar, crea usuarios fijos si no existen:

| Rol | Username | Email                                                 | Contraseña |
|---|---|-------------------------------------------------------|---|
| SUPERADMIN | superadmin | superadmin@zunochat.com                               | Super@2024! |
| ADMIN | admin1 / admin2 | admin1@zunochat.com                                   | Admin@2024! |
| USER | ×200 (DataFaker ES) | alberto_94,rodrigo_1077david_9776 | User@2024! |

> En reinicios posteriores detecta los existentes y no duplica. Si faltan usuarios USER, solo crea los necesarios.

---