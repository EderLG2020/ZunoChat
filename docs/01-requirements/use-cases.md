# Casos de Uso

> Extraído verbatim de `ui-ux-specification.md`.

## Módulo 1 — Autenticación
- UC-01: Usuario nuevo se registra → recibe OTP → verifica → accede al chat
- UC-02: Usuario registrado inicia sesión → recibe JWT
- UC-03: Usuario intenta login con cuenta baneada → error con mensaje claro
- UC-04: OTP expirado → el usuario necesita re-registrarse (sin endpoint de re-envío aún)

## Módulo 2 — Conversaciones
- UC-05: Usuario abre app → ve su inbox con conversaciones ordenadas por actividad
- UC-06: Usuario busca a otro por username → inicia conversación
- UC-07: Llega mensaje nuevo → badge con número de no leídos se actualiza en tiempo real
- UC-08: El otro usuario se conecta → indicador cambia a ONLINE en tiempo real

## Módulo 3 — Mensajería
- UC-09: Enviar texto → entrega en tiempo real al receptor conectado
- UC-10: Receptor abre conversación → todos los mensajes se marcan READ → emisor ve ✓✓
- UC-11: Usuario empieza a escribir → el otro ve "escribiendo..." hasta 5 segundos sin actividad
- UC-12: Usuario envía imagen → se muestra preview en burbuja
- UC-13: Usuario recibe payload de tipo SALES → ve tarjeta con oferta y CTA

## Módulo 4 — Gestión de Usuarios (Admin)
- UC-14: Admin reporta usuario → lo banea → usuario recibe email de suspensión
- UC-15: SUPERADMIN asigna rol ADMIN a un usuario activo
- UC-16: Admin busca usuario por username y revisa su estado

## Módulo 5 — Configuración del Sistema
- UC-17: SUPERADMIN desactiva email antes de mantenimiento de Brevo
- UC-18: SUPERADMIN ve quién activó el email por última vez y cuándo

Detalle funcional completo de cada módulo (componentes, APIs, roles): `ui-ux-specification.md`.
