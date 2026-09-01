# Ambientes

Perfiles Spring documentados: `dev` (OTP visible en respuesta API, sin requerir Redis/RabbitMQ locales) y `prod` (OTP solo por correo, `ddl-auto=validate`).

TODO: no hay documentación formal de ambientes (staging, URLs por entorno, ownership). Ver `deployment.md` para variables de entorno y `configuration.md` para flags de escalabilidad horizontal.
