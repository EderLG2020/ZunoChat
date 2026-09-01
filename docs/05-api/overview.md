# API — Visión General

Todos los endpoints siguen el prefijo `/api/**`. WebSocket en `/ws` (ver `04-architecture/architecture.md`).

## Colección Bruno

```
zunochat-bruno/
├── environments/dev.bru
├── users/
│   ├── auth/          register · verify-otp · login
│   └── admin/         dashboard · ban · eliminar · rol · listar
└── message/
    crear · listar conversacion · enviar · paginados · marcar leído
```

Importar en Bruno → seleccionar entorno **dev**.

## Formato de Respuesta

```json
[
  { "success": true, "code": "OK_LOGIN", "status": 200, "message": "...", "timestamp": "...", "data": {} },

  { "success": false, "code": "OTP_EXPIRED", "status": 400, "message": "...", "timestamp": "..." },

  { "success": false, "code": "VALID_FIELDS", "status": 400, "errors": { "email": "Formato inválido" } }
]
```
