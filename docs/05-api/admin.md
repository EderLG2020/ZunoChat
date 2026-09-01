# Admin — `/api/admin` (requiere JWT)

| Método | Endpoint | Permiso |
|---|---|---|
| GET/PUT | `/dashboard` | `dashboard:ver/editar` |
| GET | `/usuarios` | `usuarios:ver` |
| PATCH | `/usuarios/{id}/ban` | `usuarios:bannear` |
| DELETE | `/usuarios/{id}` | `usuarios:eliminar` |
| PATCH | `/usuarios/{id}/rol` | `roles:asignar` |
