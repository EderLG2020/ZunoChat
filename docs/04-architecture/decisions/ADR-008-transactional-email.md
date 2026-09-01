# ADR-008 — Servicio de email transaccional

**Contexto:** El sistema envía correos para OTP, bienvenida, cambio de estado de cuenta y reset de contraseña.

**Problema:** Elegir proveedor de email transaccional y estrategia de integración.

**Alternativas:**
- SMTP propio (JavaMailSender) — control total, gestión de reputación compleja, sin analytics
- SendGrid — popular, bien documentado, costo por volumen
- AWS SES — barato a escala, configuración más compleja, requiere dominio verificado
- Brevo (ex-Sendinblue) — tier gratuito generoso, SDK Java oficial, templates HTML

**Decisión:** Brevo con SDK oficial Java (`brevo-java`).

**Consecuencias:**
- El `EmailService` absorbe toda excepción de la API de Brevo; un fallo de correo nunca rompe el flujo de negocio
- El flag `email.enabled` en `app_config` permite activar/desactivar el envío en caliente sin reiniciar
- En dev, el OTP se expone en la respuesta REST para facilitar pruebas sin dominio verificado en Brevo
- Dependencia de un SaaS externo; si Brevo cae, los correos se pierden silenciosamente (solo log)
