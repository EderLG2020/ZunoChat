# ZunoChat — UI/UX Product Specification
> Generado por: Product Manager · UX Architect · Systems Analyst · Solution Architect · Messaging Specialist  
> Versión: 1.0.0 — Junio 2026  
> Fuente: Análisis completo de `zunochat-api` (Spring Boot 3 · Java 21)

---

## ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Inventario de Módulos](#inventario-de-módulos)
3. [Inventario Funcional Completo](#inventario-funcional-completo)
4. [Inventario de Pantallas — Mobile App](#inventario-de-pantallas--mobile-app)
5. [Inventario de Pantallas — Web Admin](#inventario-de-pantallas--web-admin)
6. [Pantallas Faltantes vs Competencia](#pantallas-faltantes-vs-competencia)
7. [User Flows](#user-flows)
8. [Arquitectura de Navegación](#arquitectura-de-navegación)
9. [Prioridad de Desarrollo](#prioridad-de-desarrollo)
10. [Componentes de Diseño Reutilizables](#componentes-de-diseño-reutilizables)

---

## RESUMEN EJECUTIVO

**ZunoChat** es una plataforma de mensajería en tiempo real con backend Spring Boot 3. El API está **completamente implementado** para mensajería 1-a-1, pero el frontend (móvil y web admin) aún **no existe**. Este documento especifica el 100% de las pantallas necesarias.

### Stack técnico detectado
- **Backend:** Spring Boot 3 · Java 21 · PostgreSQL 16 · Redis 7.2 · RabbitMQ 3.13
- **Tiempo real:** WebSocket STOMP + SockJS · Eventos: mensajes, typing, presencia, read receipts
- **Auth:** JWT HS256 · 24h · OTP 6 dígitos · BCrypt
- **Email:** Brevo (OTP, bienvenida, ban, reset password)
- **3 roles:** `USER` · `ADMIN` · `SUPERADMIN`

### Tipos de mensaje soportados
| Tipo | Descripción |
|---|---|
| `TEXT` | Texto plano |
| `IMAGE` | Imagen adjunta (hasta 3 URLs) |
| `FILE` | Archivo adjunto (hasta 3 URLs) |
| `PAYLOAD/SALES` | 🛒 Oferta comercial (JSON estructurado) |
| `PAYLOAD/SYSTEM` | ⚙️ Notificación del sistema |
| `PAYLOAD/SURVEY` | 📋 Encuesta |
| `PAYLOAD/CARD` | 🃏 Tarjeta informativa |

---

## INVENTARIO DE MÓDULOS

| # | Módulo | Estado API | Pantallas Mobile | Pantallas Web Admin |
|---|---|---|---|---|
| 1 | Autenticación | ✅ Completo | Pendientes | N/A |
| 2 | Conversaciones | ✅ Completo | Pendientes | N/A |
| 3 | Mensajería | ✅ Completo | Pendientes | N/A |
| 4 | Presencia / Tiempo real | ✅ Completo | Pendientes | N/A |
| 5 | Gestión de Usuarios | ✅ Completo | Pendientes | Pendientes |
| 6 | Búsqueda de Usuarios | ✅ Completo | Pendientes | N/A |
| 7 | Roles y Permisos | ✅ Completo | N/A | Pendientes |
| 8 | Configuración del Sistema | ✅ Completo | N/A | Pendientes |
| 9 | Perfil de Usuario | ⚠️ Parcial (campos en DB, sin endpoints PATCH) | Pendientes | N/A |
| 10 | Archivos/Media | ⚠️ Parcial (URLs aceptadas, sin upload propio) | Pendientes | N/A |
| 11 | Notificaciones Push | ❌ No implementado | Pendientes | N/A |
| 12 | Dashboard Admin | ⚠️ Placeholder (endpoint existe, sin datos reales) | N/A | Pendiente |
| 13 | Auditoría/Logs | ❌ No implementado | N/A | Pendiente |
| 14 | Chats Grupales | ❌ No implementado | Futuro | N/A |
| 15 | Recuperación de Contraseña | ⚠️ Email template existe, endpoint no implementado | Pendientes | N/A |

---

## INVENTARIO FUNCIONAL COMPLETO

---

### MÓDULO 1 — Autenticación

**Funcionalidades detectadas:**
- Registro con DNI peruano (8 dígitos), username, email y contraseña segura
- Verificación de cuenta mediante OTP de 6 dígitos (válido 10 minutos)
- Login con username **o** email + contraseña
- Generación y envío de JWT con rol y permisos granulares
- Email de bienvenida tras verificación exitosa (via Brevo)
- Email con OTP al registrarse
- Bloqueo de cuentas baneadas/inactivas en login
- Contraseña requiere: mayúscula + minúscula + número + carácter especial + 8-64 chars

**APIs relacionadas:**
```
POST /api/auth/register
POST /api/auth/verify-otp
POST /api/auth/login
```

**Roles involucrados:** Público (sin autenticación)

**Casos de uso:**
- UC-01: Usuario nuevo se registra → recibe OTP → verifica → accede al chat
- UC-02: Usuario registrado inicia sesión → recibe JWT
- UC-03: Usuario intenta login con cuenta baneada → error con mensaje claro
- UC-04: OTP expirado → el usuario necesita re-registrarse (sin endpoint de re-envío aún)

---

### MÓDULO 2 — Conversaciones

**Funcionalidades detectadas:**
- Listar todas las conversaciones del usuario autenticado (paginadas, ordenadas por último mensaje)
- Crear nueva conversación con otro usuario (o recuperar existente)
- Vista previa del último mensaje (máx. 50 chars)
- Indicador "Tú:" cuando el último mensaje lo envió el usuario actual
- Contador de mensajes no leídos por conversación
- Estado del otro participante: ONLINE / OFFLINE / TYPING / AWAY
- Timestamp del último mensaje
- Avatar del otro participante (campo en DB, pendiente de implementar upload)

**APIs relacionadas:**
```
GET  /api/conversations?page=0&size=20
POST /api/conversations { "targetUserId": 5 }
GET  /api/users/search?q=xxx
```

**Roles involucrados:** USER, ADMIN, SUPERADMIN

**Casos de uso:**
- UC-05: Usuario abre app → ve su inbox con conversaciones ordenadas por actividad
- UC-06: Usuario busca a otro por username → inicia conversación
- UC-07: Llega mensaje nuevo → badge con número de no leídos se actualiza en tiempo real
- UC-08: El otro usuario se conecta → indicador cambia a ONLINE en tiempo real

---

### MÓDULO 3 — Mensajería

**Funcionalidades detectadas:**
- Enviar mensajes de texto
- Enviar imágenes (hasta 3 URLs por mensaje)
- Enviar archivos adjuntos (hasta 3 URLs por mensaje)
- Enviar payloads estructurados (SALES, SYSTEM, SURVEY, CARD)
- Historial de mensajes paginado (orden cronológico inverso)
- Marcar mensajes como leídos (en lote por conversación)
- Indicador de estado del mensaje: SENT → DELIVERED → READ
- Indicador "escribiendo..." en tiempo real
- Read receipts en tiempo real (el emisor ve cuando el receptor lee)
- Envío doble canal: REST y WebSocket STOMP
- Preview especial para cada tipo: 📷 Imagen / 📎 Archivo / 🛒 Oferta / etc.

**APIs relacionadas:**
```
GET   /api/messages?conversationId=X&page=0&size=30
POST  /api/messages
PATCH /api/messages/read { "conversationId": X }

WebSocket:
  SEND  /app/chat.send
  SEND  /app/chat.typing
  SEND  /app/chat.read
  SEND  /app/heartbeat
  SUB   /topic/conversation.{id}
  SUB   /topic/typing.{id}
  SUB   /topic/read.{id}
  SUB   /topic/presence.{userId}
  SUB   /user/queue/notifications
```

**Roles involucrados:** USER, ADMIN, SUPERADMIN

**Casos de uso:**
- UC-09: Enviar texto → entrega en tiempo real al receptor conectado
- UC-10: Receptor abre conversación → todos los mensajes se marcan READ → emisor ve ✓✓
- UC-11: Usuario empieza a escribir → el otro ve "escribiendo..." hasta 5 segundos sin actividad
- UC-12: Usuario envía imagen → se muestra preview en burbuja
- UC-13: Usuario recibe payload de tipo SALES → ve tarjeta con oferta y CTA

---

### MÓDULO 4 — Gestión de Usuarios (Admin)

**Funcionalidades detectadas:**
- Listar todos los usuarios
- Banear usuario (cambia status a BANNED, notifica por email)
- Activar usuario baneado/inactivo
- Eliminar usuario (solo SUPERADMIN)
- Asignar/cambiar rol de usuario (solo SUPERADMIN)
- Ver dashboard de administración
- Editar dashboard (solo ADMIN+)

**APIs relacionadas:**
```
GET    /api/admin/usuarios
PATCH  /api/admin/usuarios/{id}/ban
DELETE /api/admin/usuarios/{id}
PATCH  /api/admin/usuarios/{id}/rol
GET    /api/admin/dashboard
PUT    /api/admin/dashboard
```

**Roles involucrados:**
- ADMIN: ver lista, banear, activar
- SUPERADMIN: todo lo anterior + eliminar + asignar roles

**Casos de uso:**
- UC-14: Admin reporta usuario → lo banea → usuario recibe email de suspensión
- UC-15: SUPERADMIN asigna rol ADMIN a un usuario activo
- UC-16: Admin busca usuario por username y revisa su estado

---

### MÓDULO 5 — Configuración del Sistema

**Funcionalidades detectadas:**
- Ver estado actual del servicio de email (enabled/disabled)
- Activar/desactivar envío de emails en tiempo de ejecución (sin reinicio)
- Auditoría de quién cambió la config y cuándo

**APIs relacionadas:**
```
GET /api/config/email   (requiere: config:ver)
PUT /api/config/email   (requiere: config:editar)
```

**Roles involucrados:** Solo SUPERADMIN

**Casos de uso:**
- UC-17: SUPERADMIN desactiva email antes de mantenimiento de Brevo
- UC-18: SUPERADMIN ve quién activó el email por última vez y cuándo

---

### MÓDULO 6 — Perfil de Usuario

**Funcionalidades detectadas en DB (sin endpoints PATCH implementados):**
- Campos existentes: id, dni, username, email, role, status, createdAt, updatedAt
- Campo avatar en conversations (pendiente de upload propio)
- Email templates: `sendPasswordResetConfirm` (listo, sin endpoint de trigger)

**Funcionalidades PENDIENTES de implementar en API:**
- Cambiar foto de perfil
- Cambiar contraseña
- Ver perfil propio
- Cambiar username / email
- Reset de contraseña (template de email listo, falta endpoint)

**Roles involucrados:** USER, ADMIN, SUPERADMIN

---

## INVENTARIO DE PANTALLAS — MOBILE APP

---

### SECCIÓN A: AUTENTICACIÓN

---

#### A-01 — Splash / Onboarding
**Tipo:** Mobile  
**Objetivo:** Primera impresión, branding, navegación a auth  
**Componentes:**
- Logo ZunoChat animado
- Tagline
- Botón "Crear cuenta"
- Botón "Iniciar sesión"
- Link "Términos de uso" y "Política de privacidad"

**Acciones:** Navegar a Registro / Login  
**Estados:** Solo visual  
**Prioridad:** P0

**Promp**
Diseña la pantalla móvil "Splash / Onboarding" para una aplicación de mensajería moderna llamada ZunoChat.

Objetivo:
Crear una primera impresión profesional comparable a WhatsApp, Telegram y Discord, transmitiendo confianza, comunicación en tiempo real y simplicidad.

Estilo visual:

* Moderno y minimalista
* Mobile First
* Inspirado en WhatsApp y Telegram
* Diseño premium
* Material Design 3
* Bordes suaves
* Espaciado amplio
* Microanimaciones sugeridas

Paleta de colores:

* Color primario: Verde moderno (#22C55E)
* Secundario: Verde oscuro (#16A34A)
* Fondo: Blanco puro
* Texto principal: #111827
* Texto secundario: #6B7280

Tipografía:

* Inter
* Títulos semibold
* Texto altamente legible

Contenido de la pantalla:

Header:

* Logo moderno de ZunoChat
* Icono de chat abstracto
* Nombre "ZunoChat"

Centro:

* Ilustración grande relacionada con mensajería instantánea
* Personas conversando
* Mensajes flotantes
* Sensación de comunicación en tiempo real

Título principal:
"Conecta con quien importa"

Subtítulo:
"Mensajería rápida, segura y en tiempo real para mantenerte siempre conectado."

Parte inferior:

Botón primario:
"Crear cuenta"

Botón secundario:
"Iniciar sesión"

Links:
"Términos de uso"
"Política de privacidad"

Estados:

* Diseño para modo claro
* Preparar componentes reutilizables para modo oscuro

Requisitos UX:

* Safe Area para iPhone y Android
* Responsive
* Accesibilidad AA
* Área táctil mínima de 44px
* Preparado para internacionalización

Entregables:

* Pantalla completa de onboarding
* Componentes reutilizables
* Design Tokens
* Variantes para Android e iOS
* Auto Layout configurado


---

#### A-02 — Registro
**Tipo:** Mobile  
**Objetivo:** Capturar DNI, username, email y contraseña para crear cuenta  
**Componentes:**
- Campo DNI (8 dígitos, teclado numérico)
- Campo Username (4-30 chars, reglas de formato)
- Campo Email
- Campo Contraseña (con toggle de visibilidad)
- Campo Confirmar Contraseña
- Indicador de fortaleza de contraseña
- Botón "Registrarse"
- Link "Ya tengo cuenta"

**Acciones:** Submit → navegar a Verificar OTP  
**Estados:** Default / Loading / Error / Éxito  
**Validaciones:**
- DNI: exactamente 8 dígitos numéricos
- Username: 4-30 chars, solo letras/números/punto/guión bajo, sin inicio/fin con `.` o `_`
- Email: formato válido, máx 120 chars
- Password: mín 8, máx 64, mayúscula + minúscula + número + especial, sin espacios

**Errores de API:** `USER_DNI_EXISTS` / `USER_USERNAME_EXISTS` / `USER_EMAIL_EXISTS` / `VALID_FIELDS`  
**Prioridad:** P0
**Promp**
Diseña la pantalla móvil "Registro" para ZunoChat.

Objetivo:
Permitir que nuevos usuarios creen una cuenta de forma rápida, segura y confiable.

Mantener exactamente el mismo Design System utilizado en Splash Screen.

Paleta:

Primario: #22C55E
Primario Hover: #16A34A
Background: #FFFFFF
Surface: #F8FAFC
Border: #E5E7EB
Texto Principal: #111827
Texto Secundario: #6B7280
Error: #EF4444
Success: #22C55E

Tipografía:
Inter

Estilo:

- Material Design 3
- Mobile First
- Minimalista
- Premium
- Similar a WhatsApp y Telegram
- Auto Layout completo
- Accesibilidad AA

Header:

- Botón volver
- Logo pequeño de ZunoChat
- Título:
  "Crear cuenta"

- Subtítulo:
  "Completa tus datos para comenzar."

Formulario:

1. DNI
    - teclado numérico
    - placeholder: 12345678

2. Username
    - placeholder: usuario.zuno

3. Email
    - placeholder: correo@ejemplo.com

4. Contraseña
    - toggle mostrar/ocultar

5. Confirmar contraseña
    - toggle mostrar/ocultar

Password Strength Indicator:

- Muy débil
- Débil
- Media
- Fuerte

Mostrar checklist visual:

✓ Mayúscula
✓ Minúscula
✓ Número
✓ Caracter especial

Botón principal:

"Registrarse"

Full Width

Estados:

- Enabled
- Disabled
- Loading

Footer:

Texto:
"¿Ya tienes cuenta?"

Link:
"Iniciar sesión"

Diseñar:

- Estado normal
- Estado error de validación
- Estado loading
- Estado éxito

Preparar componentes reutilizables:

- TextField
- PasswordField
- PasswordStrengthIndicator
- PrimaryButton

Diseñar variantes Android e iOS.

---

#### A-03 — Verificación OTP
**Tipo:** Mobile  
**Objetivo:** Ingresar código de 6 dígitos enviado al email  
**Componentes:**
- Texto informativo con email destino (parcialmente oculto: `j***@gmail.com`)
- 6 inputs individuales para cada dígito (estilo WhatsApp/SMS)
- Temporizador de expiración (10 minutos, cuenta regresiva)
- Botón "Verificar"
- Link "¿No recibiste el código?" (re-envío — pendiente de implementar en API)

**Acciones:** Verificar → navegar a Lista de Chats  
**Estados:** Default / Loading / Error / Expirado  
**Validaciones:** Exactamente 6 dígitos numéricos  
**Errores de API:** `OTP_INVALID` / `OTP_EXPIRED` / `USER_ALREADY_ACTIVE`  
**Prioridad:** P0
**Promp**
Diseña la pantalla móvil "Verificación OTP" para ZunoChat.

Mantener exactamente el mismo Design System del onboarding.

Objetivo:
Permitir que el usuario valide su cuenta mediante un código OTP de 6 dígitos enviado por email.

Paleta:

Primario #22C55E
Secundario #16A34A
Background #FFFFFF

Header:

Botón volver

Título:
"Verifica tu cuenta"

Subtítulo:
"Hemos enviado un código a"

correo parcial:
j***@gmail.com

Centro:

Componente OTP profesional:

[1] [2] [3] [4] [5] [6]

Características:

- Autofocus
- Auto avance
- Pegado completo
- Estados activos

Temporizador:

09:59

Indicador visual circular de tiempo restante.

Botón principal:

"Verificar"

Estados:

- Disabled
- Loading
- Success
- Error

Zona informativa:

"¿No recibiste el código?"

Link:
"Reenviar código"

Diseñar:

- OTP correcto
- OTP inválido
- OTP expirado
- Loading

Agregar ilustración minimalista relacionada con seguridad y verificación.

Preparar componente reusable:

OtpInput

Accesibilidad AA.

---

#### A-04 — Login
**Tipo:** Mobile  
**Objetivo:** Autenticar usuario existente  
**Componentes:**
- Campo Identificador (username o email)
- Campo Contraseña (con toggle visibilidad)
- Link "¿Olvidaste tu contraseña?"
- Botón "Iniciar sesión"
- Link "Crear cuenta nueva"
- Logo/branding en header

**Acciones:** Login → navegar a Lista de Chats  
**Estados:** Default / Loading / Error  
**Errores de API:** `AUTH_BAD_CREDENTIALS` / `USER_BANNED` / `OTP_PENDING_REQUIRED` / `USER_INACTIVE`  
**Prioridad:** P0
**Promp**
Diseña la pantalla móvil "Login" para ZunoChat.

Objetivo:
Permitir acceso rápido y seguro a usuarios existentes.

Mantener exactamente la identidad visual de Splash y Registro.

Estilo:

- WhatsApp
- Telegram
- Material Design 3
- Minimalista
- Premium

Header:

Logo ZunoChat

Título:

"Bienvenido"

Subtítulo:

"Inicia sesión para continuar"

Formulario:

Campo:

Usuario o Email

Campo:

Contraseña

- Toggle visibilidad

Link:

"¿Olvidaste tu contraseña?"

Botón principal:

"Iniciar sesión"

Full Width

Botón secundario:

"Crear cuenta"

Estados:

- Default
- Loading
- Error credenciales
- Usuario baneado
- Cuenta pendiente de verificación

Diseñar alertas inline modernas.

Footer:

Política de privacidad
Términos de uso

Preparar componentes reutilizables:

- LoginForm
- ErrorBanner
- AuthButton

Dark Mode Ready.


---

#### A-05 — Recuperar Contraseña (solicitud)
**Tipo:** Mobile  
**Objetivo:** Solicitar email de reset  
**Componentes:**
- Campo Email
- Botón "Enviar instrucciones"
- Link "Volver al login"

**Estados:** Default / Loading / Éxito (mensaje de confirmación)  
**Nota:** El template de email `PASSWORD_RESET_CONFIRM` está implementado en el backend, pero el endpoint de trigger **no está implementado** — tarea de API pendiente.  
**Prioridad:** P1
**Promp**
Diseña la pantalla móvil "Recuperar Contraseña — Paso 1" para ZunoChat.

Objetivo:
Permitir que el usuario solicite un enlace de recuperación ingresando su email registrado.

Mantener exactamente el mismo Design System de toda la aplicación.

Header:

Botón volver

Título:
"Recupera tu acceso"

Subtítulo:
"Ingresa tu email y te enviaremos instrucciones para restablecer tu contraseña."

Ilustración:

Ícono o ilustración minimalista relacionada con seguridad o recuperación de cuenta
(candado abierto, sobre con clave, etc.)

Formulario:

Campo Email:
- Placeholder: correo@ejemplo.com
- Validación inline
- Ícono de correo a la izquierda

Botón principal:
"Enviar instrucciones"
Full Width

Estados del botón:
- Enabled
- Disabled (campo vacío o email inválido)
- Loading (spinner mientras espera respuesta)

Estado éxito (pantalla o banner):
- Mostrar mensaje de confirmación:
  "Revisa tu bandeja de entrada"
- Subtítulo:
  "Si el email está registrado, recibirás las instrucciones en breve."
- Ícono de check o sobre enviado
- Botón secundario: "Volver al inicio de sesión"

Estado error:
- Mostrar alerta inline si el servidor devuelve error inesperado

Footer:

Link:
"¿Recordaste tu contraseña? Inicia sesión"

Nota técnica para el equipo:
El template de email PASSWORD_RESET_CONFIRM está implementado en backend.
El endpoint de trigger está pendiente — diseñar UI completa para cuando esté disponible.

Preparar componente reutilizable:
EmailInputField

Accesibilidad AA.
Dark Mode Ready.

---

#### A-06 — Recuperar Contraseña (nueva contraseña)
**Tipo:** Mobile  
**Objetivo:** Ingresar nueva contraseña tras recibir link/OTP  
**Componentes:**
- Campo Nueva Contraseña (con indicador de fortaleza)
- Campo Confirmar Contraseña
- Botón "Restablecer contraseña"

**Prioridad:** P1
**Promp**
Diseña la pantalla móvil "Recuperar Contraseña — Paso 2" para ZunoChat.

Objetivo:
Permitir al usuario establecer una nueva contraseña después de validar su identidad
mediante el enlace o token recibido por email.

Mantener exactamente el mismo Design System de toda la aplicación.

Header:

Botón volver

Título:
"Nueva contraseña"

Subtítulo:
"Crea una contraseña segura para proteger tu cuenta."

Formulario:

1. Campo "Nueva contraseña"
    - Toggle mostrar/ocultar
    - Indicador de fortaleza debajo del campo

2. Campo "Confirmar nueva contraseña"
    - Toggle mostrar/ocultar
    - Validación inline: mostrar error si no coinciden

Password Strength Indicator:

Barra de progreso con 4 niveles:
- Muy débil (rojo)
- Débil (naranja)
- Media (amarillo)
- Fuerte (verde #22C55E)

Checklist visual debajo de la barra:
✓ Mínimo 8 caracteres
✓ Al menos una mayúscula
✓ Al menos una minúscula
✓ Al menos un número
✓ Al menos un carácter especial

Botón principal:
"Restablecer contraseña"
Full Width

Estados del botón:
- Disabled (mientras no se cumplan todas las reglas)
- Enabled
- Loading

Estado éxito:
- Banner o pantalla de confirmación:
  "¡Contraseña actualizada!"
- Subtítulo:
  "Ya puedes iniciar sesión con tu nueva contraseña."
- Botón: "Ir al inicio de sesión"

Diseñar variantes:
- Estado normal
- Contraseña débil (barra roja)
- Contraseña fuerte (barra verde)
- Contraseñas no coinciden (error inline)
- Loading
- Éxito

Preparar componentes reutilizables:
- PasswordField (ya definido en A-02, reutilizar)
- PasswordStrengthIndicator (ya definido en A-02, reutilizar)

Accesibilidad AA.
Dark Mode Ready.

---

### SECCIÓN B: CHATS / MENSAJERÍA

---

#### B-01 — Lista de Conversaciones (Inbox)
**Tipo:** Mobile  
**Objetivo:** Vista principal de conversaciones activas  
**Componentes:**
- Header: Avatar del usuario + "ZunoChat" + Icono Búsqueda + Icono Nueva conversación
- Barra de búsqueda rápida (busca en conversaciones locales)
- Lista de conversaciones (scroll infinito):
    - Avatar del otro usuario (placeholder si no tiene)
    - Username del otro usuario
    - Preview del último mensaje ("Tú: ..." o texto directo)
    - Timestamp relativo (hace 2 min / Ayer / Lun)
    - Badge con contador de no leídos (número en círculo verde)
    - Indicador de estado: punto verde (ONLINE) / punto gris (OFFLINE) / animación (TYPING)
- FAB (Floating Action Button) para nueva conversación
- Tabs o bottom navigation (Chats / Perfil / Configuración)

**Acciones:**
- Tap conversación → navegar a Chat Individual
- Tap FAB → navegar a Nueva Conversación (búsqueda de usuarios)
- Swipe left en conversación → opciones (archivar/silenciar/eliminar — P2)
- Long press → selección múltiple (P2)

**Estados:** Empty state ("Aún no tienes conversaciones") / Loading skeleton / Lista  
**Tiempo real:** Actualización por WebSocket de nuevos mensajes, presencia, unread count  
**Permisos:** Autenticado (`chat:ver`)  
**Prioridad:** P0
**Promp**
Diseña la pantalla principal "Inbox de Conversaciones" para ZunoChat.

Inspiración:

- WhatsApp
- Telegram
- Messenger
- Discord Mobile

Objetivo:
Mostrar conversaciones activas con alta densidad de información y excelente legibilidad.

Paleta:

Primario #22C55E
Background #FFFFFF
Surface #F8FAFC

Header:

Avatar usuario

Logo ZunoChat

Icono búsqueda

Icono nueva conversación

Search Bar:

"Buscar conversaciones"

Lista:

Cada Conversation Item incluye:

- Avatar
- Username
- Último mensaje
- Prefijo "Tú:"
- Hora
- Badge unread
- Indicador ONLINE
- Indicador OFFLINE
- Indicador TYPING

Diseñar casos:

- 1 mensaje no leído
- 99+ mensajes no leídos
- Mensaje imagen
- Mensaje archivo
- Mensaje oferta

FAB:

Botón flotante verde

Icono +

Bottom Navigation:

Chats
Perfil
Configuración

Estados:

- Empty
- Loading Skeleton
- Lista normal

Agregar microanimaciones sugeridas.

Crear componente reusable:

ConversationListItem

Accesibilidad AA.


---

#### B-02 — Nueva Conversación / Buscar Usuario
**Tipo:** Mobile  
**Objetivo:** Encontrar un usuario para iniciar chat  
**Componentes:**
- Header con botón "Atrás" + título "Nueva conversación"
- Campo de búsqueda con ícono lupa (mínimo 2 caracteres)
- Lista de resultados (máx. 10):
    - Avatar
    - Username
    - Estado (ACTIVE, texto pequeño)
- Estado vacío: "Busca por nombre de usuario"
- Estado sin resultados: "No se encontraron usuarios"
- Loading spinner durante búsqueda

**Acciones:**
- Typing en campo → debounce 300ms → POST /api/conversations + navegar a Chat
- Tap resultado → crear/recuperar conversación → navegar al chat

**Validaciones:** Mínimo 2 caracteres, solo usuarios ACTIVE, excluye al propio usuario  
**Prioridad:** P0
**Promp**
Diseña la pantalla móvil "Nueva Conversación" para ZunoChat.

Objetivo:
Permitir encontrar usuarios rápidamente para iniciar chats.

Mantener el mismo Design System.

Header:

Botón atrás

Título:
"Nueva conversación"

Search Field:

Placeholder:
"Buscar usuario"

Resultados:

Avatar

Username

Estado ACTIVE

Indicador online

Diseñar:

- Estado vacío
- Sin resultados
- Loading
- Resultados encontrados

Agregar debounce visual con loader pequeño.

Estilo:

Telegram + WhatsApp

Preparar componente reusable:

UserSearchResultItem

Accesibilidad AA.

---

#### B-03 — Chat Individual
**Tipo:** Mobile  
**Objetivo:** Visualizar y enviar mensajes en una conversación  
**Componentes:**
- **Header:**
    - Botón "Atrás"
    - Avatar + Username del interlocutor
    - Estado de presencia (En línea / Última vez hace X min / Escribiendo...)
    - Ícono de llamada (P3) + Ícono de menú (...)
- **Lista de mensajes** (scroll, carga paginada al scrollear arriba):
    - Burbujas propias (derecha, color primario)
    - Burbujas del otro (izquierda, color secundario)
    - Timestamp por grupo de mensajes
    - Indicadores de estado en mensajes propios: ✓ (SENT) / ✓✓ gris (DELIVERED) / ✓✓ azul (READ)
    - Separador de fecha (Hoy / Ayer / DD/MM/YYYY)
    - Burbujas especiales según tipo de mensaje (ver B-04)
- **Barra de input (footer):**
    - Ícono de adjunto (abre selector de tipo)
    - Campo de texto multilínea (auto-expand)
    - Ícono emoji (P2)
    - Botón enviar (aparece al escribir, reemplaza al ícono de audio en P3)
- **Indicador de carga** al paginar mensajes anteriores

**Acciones:**
- Escribir → emite evento TYPING por WebSocket (timeout 5s auto-clear)
- Enviar texto → POST /api/messages + evento WS
- Tap adjunto → bottom sheet de opciones (imagen/archivo/payload)
- Scroll al top → carga siguiente página de mensajes (paginación)
- Abrir conversación → PATCH /api/messages/read automático
- Long press en mensaje → menú contextual (copiar/responder/reenviar — P2)

**Estados:**
- Loading skeleton al entrar
- Chat vacío: "Di 'Hola' a [username]"
- Enviando mensaje (indicador de loading en burbuja)
- Error al enviar (burbuja con ícono de error + retry)

**Tiempo real (WebSocket):**
- Suscribe a `/topic/conversation.{id}` → nuevos mensajes
- Suscribe a `/topic/typing.{id}` → indicador escribiendo
- Suscribe a `/topic/read.{id}` → actualiza ✓✓ en mensajes propios
- Suscribe a `/topic/presence.{userId}` → actualiza estado en header

**Permisos:** `chat:ver` + `chat:enviar`  
**Prioridad:** P0

**Promp**
Diseña la pantalla móvil "Chat Individual" para ZunoChat.

Objetivo:
Crear una experiencia de mensajería premium comparable a WhatsApp, Telegram y Discord.

Mantener identidad visual de toda la aplicación.

Header:

Botón atrás

Avatar

Username

Estado:

- En línea
- Última vez visto
- Escribiendo...

Acciones:

Videollamada (placeholder)
Menú (...)

Zona mensajes:

Mensajes propios:

- alineados derecha
- color verde ZunoChat

Mensajes recibidos:

- alineados izquierda
- fondo gris suave

Estados:

✓ enviado

✓✓ entregado

✓✓ leído

Separadores:

Hoy
Ayer
Fecha

Input Area:

Adjuntar

Campo multilinea

Emoji

Enviar

Diseñar casos:

- Mensaje corto
- Mensaje largo
- Imagen
- Archivo
- Error de envío
- Loading
- Chat vacío

Indicador:

"Escribiendo..."

Microanimaciones:

200ms

Preparar componentes:

ChatHeader
MessageBubble
ChatInput
TypingIndicator

Dark Mode Ready

Auto Layout completo.

---

#### B-04 — Burbujas de Mensajes Especiales
**Tipo:** Componente reutilizable (Mobile)  
**Objetivo:** Renderizar los distintos tipos de mensaje con UI específica

**Componentes por tipo:**

**TEXT:** Burbuja estándar con texto

**IMAGE:**
- Thumbnail de imagen (máx. 3 imágenes en grid 1/2/3 columnas)
- Tap → abre visor de imagen en full screen
- Placeholder de carga

**FILE:**
- Ícono de tipo de archivo (PDF, DOCX, ZIP, etc.)
- Nombre del archivo
- Peso (si disponible)
- Botón descargar

**PAYLOAD/SALES 🛒:**
- Card con: título, descripción, precio, imagen (opcional)
- Botón CTA (ej: "Ver oferta")
- Fondo diferenciado (amarillo/naranja suave)

**PAYLOAD/SYSTEM ⚙️:**
- Texto centrado en gris (sin burbuja)
- Ej: "Esta conversación fue iniciada el 10/06/2026"

**PAYLOAD/SURVEY 📋:**
- Card con título + opciones como botones
- Estado: pendiente / respondida

**PAYLOAD/CARD 🃏:**
- Card visual con título, subtítulo e imagen
- Sin CTA interactivo

**Prioridad:** P0

**Promp**
Diseña el sistema completo de "Burbujas de Mensajes Especiales" para ZunoChat.

Objetivo:
Crear todos los componentes de burbuja según el tipo de mensaje soportado por la API.
Deben ser visualmente distinguibles, coherentes con el Design System y altamente legibles.

Mantener exactamente el mismo Design System de toda la aplicación.

---

TIPO 1 — TEXT (estándar)

Burbuja propia (derecha):
- Fondo: #22C55E
- Texto: blanco
- Radio de borde: 18px (esquina inferior derecha: 4px)

Burbuja recibida (izquierda):
- Fondo: #F3F4F6
- Texto: #111827
- Radio de borde: 18px (esquina inferior izquierda: 4px)

Ambas incluyen:
- Timestamp pequeño (12px, esquina inferior derecha de la burbuja)
- Indicadores de estado en mensajes propios: ✓ enviado / ✓✓ gris entregado / ✓✓ azul leído

---

TIPO 2 — IMAGE

Burbuja con imagen:
- Thumbnail redondeado dentro de la burbuja
- Soporte para 1, 2 o 3 imágenes:
    - 1 imagen: ocupa todo el ancho de la burbuja
    - 2 imágenes: grid 2 columnas
    - 3 imágenes: grid 2+1 o 3 columnas
- Placeholder de carga con shimmer animation
- Ícono de lupa en overlay (tap para abrir visor)
- Indicadores de estado y timestamp igual que TEXT
- Texto opcional debajo de las imágenes

---

TIPO 3 — FILE

Burbuja de archivo:
- Ícono de tipo de archivo a la izquierda (PDF, DOCX, ZIP, MP4, etc.)
  Usar colores semánticos: PDF=rojo, DOCX=azul, ZIP=amarillo, genérico=gris
- Nombre del archivo (bold, truncado con ellipsis si es largo)
- Tamaño del archivo (13px, color secundario)
- Botón de descarga a la derecha (ícono de flecha hacia abajo)
- Fondo diferenciado: blanco con borde suave para recibidos
- Mismos indicadores de estado

---

TIPO 4 — PAYLOAD/SALES 🛒

Card de oferta comercial:
- Fondo suave cálido: #FFFBEB (amarillo muy tenue)
- Borde izquierdo de acento: 3px solid #F59E0B
- Encabezado: etiqueta "Oferta especial" con ícono 🛒 (13px, color #92400E)
- Título del producto (bold, 15px)
- Descripción breve (13px, máx 2 líneas, con ellipsis)
- Precio destacado (20px, bold, #22C55E)
- Precio anterior tachado si aplica (13px, gris)
- Imagen del producto (opcional, thumbnail a la derecha)
- Botón CTA primario: "Ver oferta" (verde, full width de la card)
- Timestamp y estado igual que TEXT

---

TIPO 5 — PAYLOAD/SYSTEM ⚙️

Notificación del sistema:
- Sin burbuja — texto centrado flotante
- Fondo: píldora gris muy suave (#F3F4F6)
- Texto: 13px, #6B7280, centrado
- Ícono pequeño ⚙️ o 🔒 a la izquierda del texto
- Sin timestamp ni indicadores de estado
- Ejemplos:
  "Esta conversación fue iniciada el 10/06/2026"
  "Los mensajes están protegidos de extremo a extremo"

---

TIPO 6 — PAYLOAD/SURVEY 📋

Card de encuesta:
- Fondo: #F0F9FF (azul muy tenue)
- Borde izquierdo de acento: 3px solid #0EA5E9
- Etiqueta "Encuesta" con ícono 📋 (13px, color #0C4A6E)
- Pregunta principal (bold, 15px)
- Opciones de respuesta como botones outline:
    - Estado pendiente: botones habilitados, fondo blanco
    - Estado respondida: opción elegida con check ✓ y fondo verde tenue
- Si ya respondida: mostrar "Ya respondiste esta encuesta" en texto pequeño
- Timestamp y estado igual que TEXT

---

TIPO 7 — PAYLOAD/CARD 🃏

Tarjeta informativa:
- Card con imagen superior (ratio 16:9 o 4:3, redondeada arriba)
- Título (bold, 15px)
- Subtítulo o descripción (13px, máx 3 líneas)
- Sin botón CTA interactivo (solo informativa)
- Borde suave, sombra muy sutil
- Timestamp y estado igual que TEXT

---

Diseñar todas las variantes en:
- Estado recibido (izquierda)
- Estado propio (derecha) — cuando aplique
- Estado loading/enviando
- Estado error de envío (borde rojo + ícono retry)

Preparar como componentes reutilizables:
- MessageBubble (base)
- ImageBubble
- FileBubble
- SalesPayloadCard
- SystemMessage
- SurveyCard
- InfoCard

Auto Layout completo.
Accesibilidad AA (contraste mínimo 4.5:1 en todo texto).
Dark Mode Ready.

---

#### B-05 — Visor de Imagen
**Tipo:** Mobile (modal full screen)  
**Objetivo:** Ver imagen en pantalla completa  
**Componentes:**
- Imagen a pantalla completa con zoom (pinch-to-zoom)
- Contador si hay múltiples imágenes (1/3)
- Botón cerrar (X)
- Botón descargar
- Fondo negro

**Acciones:** Swipe para cambiar imagen si hay múltiples / Tap para mostrar/ocultar controles  
**Prioridad:** P1
**Promp**
Diseña el componente modal "Visor de Imagen en Pantalla Completa" para ZunoChat.

Objetivo:
Permitir ver imágenes compartidas en el chat con zoom, navegación entre imágenes
y opción de descarga. Se activa al tocar un thumbnail en una burbuja tipo IMAGE.

Estilo:
- Fondo negro puro (#000000)
- Overlay oscuro
- Controles blancos con transparencia
- Minimalista — la imagen es el protagonista

Estructura:

Header (overlay superior, aparece/desaparece al tocar):
- Botón cerrar (X) en esquina superior izquierda
- Username del remitente + timestamp en el centro
- Botón descarga (ícono flecha hacia abajo) en esquina superior derecha
- Fondo: degradado negro de arriba hacia transparente

Zona central:
- Imagen a pantalla completa (object-fit: contain)
- Fondo negro puro
- Soporte para gestos:
    - Pinch to zoom (0.5x — 4x)
    - Double tap: zoom 2x / volver a 1x
    - Pan cuando está en zoom
    - Swipe horizontal para navegar entre imágenes del mismo mensaje

Footer (overlay inferior, aparece/desaparece al tocar):
- Contador de imágenes: "2 / 3"
- Miniaturas horizontales (si hay más de 1 imagen): 48x48px, thumbnail activo con borde blanco
- Fondo: degradado negro de abajo hacia transparente

Estados:
- Loading: spinner blanco centrado sobre fondo negro
- Una sola imagen: sin contador ni miniaturas, sin flechas
- Múltiples imágenes: flechas de navegación laterales visibles
- Zoom activo: ocultar header y footer automáticamente
- Error al cargar: ícono de imagen rota + texto "No se pudo cargar la imagen"

Transición:
- Apertura: shared element transition desde thumbnail (200ms ease-out)
- Cierre: swipe hacia abajo con drag dismiss, o tap en X

Accesibilidad:
- Botones con área táctil mínima 44px
- Labels accesibles para lectores de pantalla
- Soporte para gestos alternativos con botones visibles

Preparar componente reutilizable:
ImageViewer

Dark Mode: ya nativo (fondo negro).

---

#### B-06 — Selector de Adjunto (Bottom Sheet)
**Tipo:** Mobile (modal)  
**Objetivo:** Elegir qué tipo de contenido adjuntar  
**Componentes:**
- Fondo semi-transparente
- Bottom sheet con opciones:
    - 📷 Cámara (tomar foto)
    - 🖼️ Galería (seleccionar imagen existente)
    - 📎 Documento (seleccionar archivo)
    - 🎤 Audio (P3)
- Botón Cancelar

**Prioridad:** P1
**Promp**
Diseña el componente modal "Bottom Sheet de Adjuntos" para ZunoChat.

Objetivo:
Permitir al usuario elegir qué tipo de contenido adjuntar en el chat,
activado al tocar el ícono de clip/adjunto en la barra de entrada de texto.

Mantener exactamente el mismo Design System de toda la aplicación.

Comportamiento:
- Se desliza desde abajo hacia arriba (slide up)
- Fondo semi-transparente oscuro detrás (#000 al 50% de opacidad)
- Tap fuera del sheet → cierra
- Swipe hacia abajo → cierra
- Handle bar en la parte superior del sheet (píldora gris)

Estructura del Bottom Sheet:

Handle bar (píldora de arrastre, centrada)

Título opcional:
"Compartir"
(14px, bold, centrado, color secundario)

Grid de opciones (2 columnas x 2 filas):

Opción 1 — Cámara:
- Ícono de cámara en círculo verde (#22C55E)
- Label: "Cámara"

Opción 2 — Galería:
- Ícono de imagen/foto en círculo azul
- Label: "Galería"

Opción 3 — Documento:
- Ícono de archivo/clip en círculo naranja
- Label: "Documento"

Opción 4 — Audio (placeholder, estado deshabilitado P3):
- Ícono de micrófono en círculo gris
- Label: "Audio"
- Badge "Próximamente" encima del ícono

Cada opción:
- Círculo de 56px con ícono de 24px
- Label debajo de 12px
- Tap: animación de escala (scale 0.92) + ripple
- Estado deshabilitado: opacidad 40%

Separador

Botón cancelar:
"Cancelar"
- Full width
- Fondo blanco, texto gris
- Borde superior suave

Variantes a diseñar:
- Estado normal (todas habilitadas)
- Estado con Audio deshabilitado
- Estado loading (después de seleccionar, mientras procesa)

Safe Area inferior: respetar el home indicator en iPhone y la barra de Android.

Animaciones:
- Apertura: slide up 250ms ease-out
- Cierre: slide down 200ms ease-in
- Fondo: fade in/out sincronizado con el sheet

Preparar componente reutilizable:
AttachmentBottomSheet

Accesibilidad AA.
Dark Mode Ready.

---

### SECCIÓN C: PERFIL

---

#### C-01 — Mi Perfil
**Tipo:** Mobile  
**Objetivo:** Ver y editar la información personal del usuario  
**Componentes:**
- Avatar grande con botón de edición superpuesto (cámara)
- Username (editable — P1)
- Email (visible, editable — P1)
- DNI (solo lectura)
- Rol (solo lectura, con badge: USER / ADMIN / SUPERADMIN)
- Estado de cuenta (ACTIVE con badge verde)
- Fecha de registro
- Sección "Seguridad":
    - "Cambiar contraseña" (navega a C-02)
    - "Sesiones activas" (P2)
- Sección "Cuenta":
    - "Desactivar cuenta" (P2)
    - "Cerrar sesión" (con confirmación)

**Acciones:** Tap avatar → seleccionar/tomar foto / Guardar cambios  
**Permisos:** `profile:ver` + `profile:editar`  
**Prioridad:** P1
**Promp**
Diseña la pantalla móvil "Mi Perfil" para ZunoChat.

Objetivo:
Permitir al usuario ver y editar su información personal, gestionar su seguridad
y acceder a opciones de cuenta.

Mantener exactamente el mismo Design System de toda la aplicación.

Header:

Título: "Mi perfil"
Botón "Editar" en la esquina superior derecha (modo edición toggle)

Zona superior — Avatar y datos principales:

Avatar circular grande (96px):
- Foto de perfil del usuario
- Placeholder con iniciales si no tiene foto
- Botón de edición superpuesto (ícono de cámara, círculo verde pequeño en esquina inferior derecha)
- Tap → bottom sheet con opciones: "Tomar foto" / "Elegir de galería" / "Eliminar foto"

Username: texto bold 20px
Email: texto secundario 15px
Badge de rol: píldora con color según rol:
- USER: gris
- ADMIN: azul
- SUPERADMIN: violeta
  Badge de estado: punto verde + "Activo"

Sección "Información":

Lista de campos en formato tarjeta:
- DNI: valor (solo lectura, ícono de candado)
- Usuario: valor (editable en modo edición)
- Email: valor (editable en modo edición)
- Miembro desde: fecha de registro (solo lectura)

En modo edición:
- Campos editables se transforman en TextFields
- Botón "Guardar cambios" aparece al pie

Sección "Seguridad":

- Ítem: "Cambiar contraseña" → navega a C-02 (ícono chevron derecho)
- Ítem: "Sesiones activas" → placeholder P2 (badge "Próximamente")

Sección "Cuenta":

- Ítem: "Desactivar cuenta" → placeholder P2, color naranja de advertencia
- Ítem: "Cerrar sesión" → color rojo
    - Tap: modal de confirmación: "¿Cerrar sesión?" con botones "Cancelar" / "Cerrar sesión"

Diseñar estados:
- Vista normal (solo lectura)
- Modo edición (campos activos)
- Loading al guardar
- Éxito al guardar (toast verde "Perfil actualizado")
- Error al guardar (mensaje inline)

Preparar componentes reutilizables:
- ProfileAvatar (con botón de edición)
- ProfileField (solo lectura / editable)
- SectionCard (tarjeta de sección con ítems)

Accesibilidad AA.
Dark Mode Ready.
---

#### C-02 — Cambiar Contraseña
**Tipo:** Mobile  
**Objetivo:** Actualizar contraseña desde el perfil  
**Componentes:**
- Campo "Contraseña actual"
- Campo "Nueva contraseña" (con indicador de fortaleza)
- Campo "Confirmar nueva contraseña"
- Botón "Actualizar contraseña"

**Validaciones:** Mismas reglas que registro  
**Prioridad:** P1

**Promp**
Diseña la pantalla móvil "Cambiar Contraseña" para ZunoChat.

Objetivo:
Permitir al usuario actualizar su contraseña desde la sección de seguridad de su perfil.
Requiere ingresar la contraseña actual para confirmar identidad.

Mantener exactamente el mismo Design System de toda la aplicación.

Header:

Botón volver

Título: "Cambiar contraseña"

Formulario:

1. Campo "Contraseña actual"
    - Toggle mostrar/ocultar
    - Placeholder: ••••••••

2. Separador visual entre contraseña actual y nueva

3. Campo "Nueva contraseña"
    - Toggle mostrar/ocultar
    - Indicador de fortaleza debajo

4. Campo "Confirmar nueva contraseña"
    - Toggle mostrar/ocultar
    - Validación inline: error si no coincide

Password Strength Indicator:
Misma implementación que en A-02 y A-06.

Checklist:
✓ Mínimo 8 caracteres
✓ Mayúscula
✓ Minúscula
✓ Número
✓ Carácter especial

Botón principal:
"Actualizar contraseña"
Full Width
Deshabilitado hasta que todos los requisitos se cumplan y las contraseñas coincidan.

Estados:
- Normal
- Loading (spinner en botón)
- Error: contraseña actual incorrecta → alert inline rojo: "La contraseña actual no es correcta"
- Error: contraseñas no coinciden → inline bajo campo confirmar
- Éxito → toast verde + volver automáticamente a Mi Perfil

Nota de seguridad (pequeña, al pie del formulario):
"Por seguridad, se cerrará sesión en otros dispositivos al cambiar tu contraseña."
(ícono de escudo, texto 12px, color secundario)

Accesibilidad AA.
Dark Mode Ready.

---

#### C-03 — Perfil de otro Usuario
**Tipo:** Mobile  
**Objetivo:** Ver información pública de un contacto  
**Componentes:**
- Avatar grande
- Username
- Estado de presencia (En línea / Última vez hace X)
- Botón "Enviar mensaje"
- Botón "Bloquear usuario" (P2)

**Acciones:** Tap "Enviar mensaje" → crear conversación y navegar al chat  
**Prioridad:** P2
**Promp**
Diseña la pantalla móvil "Perfil de Otro Usuario" para ZunoChat.

Objetivo:
Mostrar la información pública de un contacto y permitir acciones rápidas.
Se accede tocando el avatar o nombre en un chat.

Mantener exactamente el mismo Design System de toda la aplicación.

Diseño: modal bottom sheet de altura media (70% de pantalla) o pantalla completa.

Header:

Botón cerrar (X) en esquina superior derecha

Zona principal:

Avatar circular grande (80px)
Indicador de presencia (punto verde si ONLINE, gris si OFFLINE)

Username: bold, 20px
Estado textual:
- "En línea" (verde)
- "Última vez hace X minutos/horas/días" (gris)

Acciones principales (botones grandes, full width):

Botón primario:
"Enviar mensaje"
→ Crea o recupera conversación y navega al chat

Botón secundario (outline):
"Bloquear usuario" (P2, estado deshabilitado con badge "Próximamente")

Información adicional (si disponible):

Miembro desde: fecha
(No mostrar email, DNI ni datos sensibles — solo datos públicos)

Estados:
- Cargando perfil (skeleton)
- Perfil cargado
- Usuario no encontrado / inactivo: mostrar mensaje de error

Transición:
- Apertura desde avatar: shared element transition suave
- Cierre: swipe hacia abajo o tap en X

Preparar componente reutilizable:
UserProfileSheet

Accesibilidad AA.
Dark Mode Ready.


---

SECCIÓN D — CONFIGURACIÓN (Mobile)

---

#### D-01 — Configuración
**Tipo:** Mobile  
**Objetivo:** Ajustes generales de la app  
**Componentes:**
- Sección "Notificaciones":
    - Toggle activar/desactivar notificaciones push (P1)
    - Toggle sonido / vibración (P2)
- Sección "Privacidad":
    - "Última vez visto": Todos / Mis contactos / Nadie (P2)
    - "Foto de perfil": Todos / Mis contactos / Nadie (P2)
- Sección "Chats":
    - "Tamaño de fuente" (P3)
    - "Fondo de chat" (P3)
- Sección "Almacenamiento y datos" (P2)
- Sección "Ayuda" (P3)
- Sección "Acerca de ZunoChat":
    - Versión de la app
    - Términos de uso / Política de privacidad

**Prioridad:** P1
**Promp**
Diseña la pantalla móvil "Configuración" para ZunoChat.

Objetivo:
Centralizar todos los ajustes de la aplicación organizados por categoría.
Diseño tipo Settings de iOS / Android nativo — limpio y escaneable.

Mantener exactamente el mismo Design System de toda la aplicación.

Header:

Título: "Configuración"

Estructura de la pantalla (lista de secciones con separadores):

─── NOTIFICACIONES ───
- Notificaciones push [Toggle ON/OFF]
- Sonido [Toggle ON/OFF]
- Vibración [Toggle ON/OFF]

─── PRIVACIDAD ───
- Última vez visto: "Todos" [Chevron → submenu con opciones: Todos / Mis contactos / Nadie] — P2
- Foto de perfil: "Todos" [Chevron → submenu] — P2

─── CHATS ───
- Tamaño de fuente: "Normal" [Chevron → submenu P3]
- Fondo de chat: "Predeterminado" [Chevron → selector P3]

─── APARIENCIA ───
- Tema: "Claro" [Chevron → opciones: Claro / Oscuro / Sistema] — P1

─── ALMACENAMIENTO Y DATOS ─── (P2)
- Uso de almacenamiento [Chevron]
- Descarga automática de medios [Toggle]

─── AYUDA ─── (P3)
- Centro de ayuda [Chevron + ícono externo]
- Reportar un problema [Chevron]

─── ACERCA DE ───
- Versión de la app: "1.0.0 (build 100)"
- Términos de uso [Chevron]
- Política de privacidad [Chevron]

Diseño de ítems:

Ítem con toggle:
- Ícono izquierda (16px, color primario o secundario según importancia)
- Label
- Toggle a la derecha

Ítem navegable:
- Ícono izquierda
- Label
- Valor actual (gris) + chevron a la derecha

Ítem de información:
- Label izquierda
- Valor derecha (gris)

Ítem destructivo / peligroso:
- Sin ícono o con ícono de advertencia
- Texto en rojo
- Solo: "Cerrar sesión" (que ya está en Mi Perfil — no duplicar aquí)

Estados:
- Toggle activado / desactivado
- Ítem deshabilitado con badge "Próximamente" para funciones P2/P3

Preparar componentes reutilizables:
- SettingsSection (título de sección)
- SettingsItem (toggle / navigable / info)
- SettingsToggle

Accesibilidad AA (soporte para Dynamic Type — texto escalable).
Dark Mode Ready.
---

#### D-02 — Notificaciones
**Tipo:** Mobile  
**Objetivo:** Gestionar preferencias de notificaciones push  
**Componentes:**
- Toggle global
- Toggle por tipo: mensajes / menciones / llamadas (P3)
- Horario silencioso (P2)

**Prioridad:** P1
**Promp**
Diseña la pantalla móvil "Notificaciones" para ZunoChat.

Objetivo:
Gestionar en detalle las preferencias de notificaciones push.
Se accede desde Configuración → Notificaciones.

Mantener exactamente el mismo Design System de toda la aplicación.

Header:

Botón volver
Título: "Notificaciones"

Sección principal:

Toggle global grande:
"Activar notificaciones"
Descripción: "Recibe alertas de nuevos mensajes incluso con la app cerrada."
Estado: ON / OFF
Cuando está OFF → todos los ítems debajo se deshabilitan visualmente (opacidad 40%)

─── TIPOS DE NOTIFICACIÓN ─── (habilitados solo si toggle global está ON)

- Nuevos mensajes [Toggle]
  Descripción: "Notificar cuando recibes un mensaje"

- Solicitudes de conversación [Toggle] — P2
  Descripción: "Cuando alguien inicia un chat contigo"

─── SONIDO Y VIBRACIÓN ───

- Sonido [Toggle]
  Selector de tono: "Predeterminado" [Chevron → P2]

- Vibración [Toggle]

─── NO MOLESTAR ─── (P2)

- Horario silencioso [Toggle]
  Sub-ítems (cuando activo):
    - Desde: 22:00
    - Hasta: 08:00

Estado de permisos del sistema:

Banner informativo si los permisos del sistema están desactivados:
Fondo amarillo tenue, ícono de advertencia
"Las notificaciones están bloqueadas en la configuración de tu teléfono"
Botón: "Ir a ajustes" → abre configuración del sistema operativo

Diseñar estados:
- Notificaciones completamente activas
- Toggle global desactivado (todo en gris)
- Permisos del sistema denegados (banner de advertencia)

Accesibilidad AA.
Dark Mode Ready.

---

## INVENTARIO DE PANTALLAS — WEB ADMIN

> Accesible solo con roles ADMIN y SUPERADMIN. Panel responsive (desktop-first).

---

### SECCIÓN E: AUTENTICACIÓN ADMIN

---

#### E-01 — Login Admin
**Tipo:** Desktop / Responsive  
**Objetivo:** Acceso seguro al panel de administración  
**Componentes:**
- Logo ZunoChat Admin
- Campo identificador (username o email)
- Campo contraseña
- Botón "Iniciar sesión"
- Mensaje de error contextual

**Acciones:** Login exitoso → Dashboard  
**Redirección:** Si ya tiene token válido → redirect a Dashboard  
**Prioridad:** P0
**Promp**
Diseña la pantalla Web Admin Login para ZunoChat.

Objetivo:
Acceso seguro al panel administrativo.

Inspiración:

- Linear
- Vercel
- Stripe Dashboard
- Clerk

Desktop First.

Paleta:

Primario #22C55E

Layout:

Panel dividido 50/50.

Izquierda:

Branding ZunoChat Admin

Ilustración tecnológica moderna

Derecha:

Card Login

Campos:

Usuario o Email

Contraseña

Botón:

"Iniciar sesión"

Diseñar:

- Estado normal
- Loading
- Error

Material Design 3

Responsive

Dark Mode Ready.
---

### SECCIÓN F: DASHBOARD

---

#### F-01 — Dashboard Principal
**Tipo:** Desktop / Responsive  
**Objetivo:** Vista general del estado del sistema  
**Componentes:**
- **KPI Cards:**
    - Total usuarios registrados
    - Usuarios activos hoy
    - Usuarios baneados
    - Mensajes enviados hoy
    - Conversaciones activas
- **Gráfico:** Registros por día (últimos 30 días)
- **Gráfico:** Mensajes por hora (hoy)
- **Tabla:** Últimos 10 registros de usuarios
- **Estado del sistema:**
    - Email (enabled/disabled con badge)
    - Redis (online/offline)
    - RabbitMQ (online/offline)
- **Accesos rápidos:** Banear usuario / Ver logs / Config email

**Permisos:** `dashboard:ver`  
**Nota:** El endpoint `GET /api/admin/dashboard` existe pero devuelve placeholder. La API necesita implementar los datos reales.  
**Prioridad:** P0
**Promp**
Diseña el "Dashboard Principal" del panel de administración web de ZunoChat.

Objetivo:
Ofrecer una vista general del estado del sistema, métricas clave y accesos rápidos.
Es la pantalla de inicio al hacer login como ADMIN o SUPERADMIN.

Inspiración visual:
- Linear
- Vercel Dashboard
- Stripe Dashboard
- Clerk Admin
- shadcn/ui + Tailwind

Desktop First. Layout responsive (sidebar colapsable en tablets).
Paleta: Primario #22C55E · Fondo general: #F8FAFC · Sidebar: #111827

Layout general:

Sidebar izquierdo (fijo, 240px):
- Logo ZunoChat Admin
- Navegación: Dashboard (activo), Usuarios, Roles, Configuración, Logs, Métricas
- Al pie: Avatar admin + Username + "Cerrar sesión"

Contenido principal (el resto del ancho):

─── HEADER DE PÁGINA ───
Título: "Dashboard"
Subtítulo: "Bienvenido, [username]"
Badge con fecha y hora actual
Botón "Actualizar" (ícono refresh + texto)

─── FILA 1: KPI CARDS (5 tarjetas en grid) ───

Card 1: Total usuarios registrados
- Número grande (ej: 1,284)
- Variación vs ayer: "+12 hoy" en verde

Card 2: Usuarios activos hoy
- Número grande
- Subtítulo: "Conectados ahora: 47"

Card 3: Usuarios baneados
- Número grande
- Color de advertencia si > 0

Card 4: Mensajes enviados hoy
- Número grande
- Variación: "+8% vs ayer"

Card 5: Conversaciones activas
- Número grande
- Subtítulo: "Iniciadas hoy: 23"

─── FILA 2: GRÁFICOS ───

Gráfico izquierdo (60% ancho):
Línea o barras: "Registros por día — últimos 30 días"
Eje X: fechas · Eje Y: cantidad de registros
Tooltip al hover con el dato exacto

Gráfico derecho (40% ancho):
Barras horizontales: "Mensajes por hora — hoy"
Muestra la distribución de actividad durante el día

─── FILA 3: TABLA + ESTADO DEL SISTEMA ───

Tabla izquierda (65% ancho):
"Últimos 10 usuarios registrados"
Columnas: Avatar | Username | Email | Rol | Estado | Fecha registro
Botón "Ver todos" al pie → navega a G-01

Panel derecho (35% ancho):
"Estado del sistema"
- Email (Brevo): badge ACTIVO (verde) / INACTIVO (rojo) + toggle rápido para SUPERADMIN
- Redis: badge ONLINE / OFFLINE
- RabbitMQ: badge ONLINE / OFFLINE
- WebSocket activo: contador de conexiones activas

Accesos rápidos (debajo del estado):
Botón: "Banear usuario" → abre G-03
Botón: "Ver logs"      → navega a J-01
Botón: "Config email"  → navega a I-01

─── NOTA TÉCNICA ───
El endpoint GET /api/admin/dashboard existe pero devuelve datos placeholder.
Diseñar con datos de ejemplo realistas para la presentación.

Estados del dashboard:
- Loading inicial (skeleton en todas las cards y gráficos)
- Datos cargados (estado normal)
- Error de carga (banner rojo + botón reintentar)
- Sistema con algún servicio caído (badge rojo en estado del sistema)

Preparar componentes reutilizables:
- KPICard
- SystemStatusBadge
- QuickActionButton
- AdminSidebar

Responsive: en tablet (< 1024px) mostrar sidebar colapsado con solo íconos.
Dark Mode Ready.
Accesibilidad AA.

---

### SECCIÓN G: GESTIÓN DE USUARIOS

---

#### G-01 — Listado de Usuarios
**Tipo:** Desktop / Responsive  
**Objetivo:** Ver y gestionar todos los usuarios del sistema  
**Componentes:**
- Filtros: por rol (USER/ADMIN/SUPERADMIN) / por estado (ACTIVE/BANNED/INACTIVE/PENDING) / búsqueda por username o email
- Tabla de usuarios:
    - ID, Username, Email, DNI, Rol, Estado, Fecha de registro, Última actividad
    - Acciones por fila: Ver / Banear / Activar / Cambiar rol / Eliminar
- Paginación
- Exportar a CSV (P2)
- Total de resultados

**Permisos:**
- `usuarios:ver` → ver listado
- `usuarios:bannear` → acción banear
- `usuarios:eliminar` → acción eliminar (solo SUPERADMIN)
- `roles:asignar` → cambiar rol (solo SUPERADMIN)

**Prioridad:** P0
**Promp**
Diseña la pantalla web "Listado de Usuarios" del panel admin de ZunoChat.

Objetivo:
Permitir a ADMIN y SUPERADMIN buscar, filtrar, ver y moderar todos los usuarios
del sistema con acciones contextuales por fila.

Mantener el mismo Design System del panel admin (sidebar de F-01).

Layout: Sidebar izquierdo (igual que F-01) + contenido principal.

─── HEADER DE PÁGINA ───

Título: "Usuarios"
Subtítulo: "Gestiona y modera todos los usuarios registrados."
Contador: "1,284 usuarios en total"

─── BARRA DE FILTROS Y BÚSQUEDA ───

Fila de filtros (alineada horizontalmente):

1. Campo de búsqueda:
    - Placeholder: "Buscar por username, email o DNI..."
    - Ícono de lupa
    - Borrar búsqueda (X)

2. Filtro por Rol:
    - Dropdown: Todos los roles / USER / ADMIN / SUPERADMIN

3. Filtro por Estado:
    - Dropdown: Todos los estados / ACTIVE / BANNED / INACTIVE / PENDING

4. Botón "Limpiar filtros" (aparece solo cuando hay filtros activos)

─── TABLA DE USUARIOS ───

Encabezados de columna (con ordenamiento):
# | Avatar+Username | Email | DNI | Rol | Estado | Registro | Acciones

Por fila:
- Nº de ID
- Avatar circular (32px) + Username (bold) + email pequeño debajo
- Email completo
- DNI (•••••678 parcialmente oculto por defecto, ícono para revelar)
- Badge de rol con color: USER=gris / ADMIN=azul / SUPERADMIN=violeta
- Badge de estado: ACTIVE=verde / BANNED=rojo / INACTIVE=naranja / PENDING=amarillo
- Fecha de registro (formato DD/MM/YYYY)
- Botones de acción por fila:
    - Ver [ícono ojo] → navega a G-02
    - Banear [ícono prohibido] → abre modal G-03 (solo si ACTIVE)
    - Activar [ícono check] → acción directa (solo si BANNED o INACTIVE)
    - Cambiar rol [ícono escudo] → abre modal G-04 (solo SUPERADMIN)
    - Eliminar [ícono papelera, rojo] → confirmación modal (solo SUPERADMIN)

Los botones de acción no disponibles aparecen en gris/deshabilitados.

─── PAGINACIÓN ───

Mostrando 1-20 de 1,284 usuarios
Selector de filas por página: 10 / 20 / 50
Controles: Anterior / páginas numeradas / Siguiente

─── ESTADOS ───

Loading: skeleton en toda la tabla (10 filas)
Tabla con datos: estado normal
Sin resultados para los filtros aplicados:
- Ilustración pequeña
- "No se encontraron usuarios con estos filtros"
- Botón "Limpiar filtros"
  Error de carga: banner rojo + reintentar

Diseñar también:
- Hover en fila (fondo sutil)
- Fila seleccionada (checkbox para selección múltiple — P2)
- Toast de éxito tras banear/activar

Preparar componentes reutilizables:
- UserTableRow
- RoleBadge
- StatusBadge
- ActionButton
- FilterBar
- Pagination

Responsive: en tablet, ocultar columnas menos importantes (DNI, fecha).
Dark Mode Ready.
Accesibilidad AA.
---

#### G-02 — Detalle de Usuario
**Tipo:** Desktop / Responsive  
**Objetivo:** Ver información completa de un usuario  
**Componentes:**
- Datos del usuario: ID, DNI, username, email, rol, estado
- Historial de cambios de estado (quién baneó, cuándo — P2)
- Botones de acción según permisos:
    - Banear / Activar / Eliminar / Cambiar rol

**Prioridad:** P1

**Promp**
Diseña la pantalla web "Detalle de Usuario" del panel admin de ZunoChat.

Objetivo:
Mostrar toda la información de un usuario específico y permitir acciones de moderación.
Se accede desde G-01 al pulsar "Ver" en una fila.

Mantener el mismo Design System del panel admin.

Layout: Sidebar + contenido principal. Puede ser página completa o drawer lateral (preferir página completa para más espacio).

─── HEADER DE PÁGINA ───

Breadcrumb: Usuarios › @username
Botón "Volver al listado"

─── PERFIL DEL USUARIO (card superior) ───

Avatar circular grande (64px) + iniciales si no tiene foto
Username (bold, 20px)
Email
Badge de rol + Badge de estado

Fila de acciones rápidas (según permisos del admin logueado):
- [Banear] botón rojo outline → modal G-03
- [Activar] botón verde → acción directa
- [Cambiar rol] botón outline → modal G-04
- [Eliminar] botón rojo → modal de confirmación (solo SUPERADMIN)

─── SECCIÓN: INFORMACIÓN DE LA CUENTA ───

Grid de datos en 2 columnas:
- ID interno
- DNI (parcialmente oculto + botón revelar)
- Username
- Email
- Rol actual
- Estado de cuenta
- Fecha de registro
- Fecha de última actualización

─── SECCIÓN: HISTORIAL DE ACCIONES (P2 — diseñar como placeholder) ───

Tabla: Fecha | Acción | Realizado por | Motivo
Placeholder: "El historial de acciones estará disponible próximamente."
Badge "Próximamente"

─── SECCIÓN: ESTADÍSTICAS DE USO ───

Cards pequeñas:
- Total mensajes enviados
- Total conversaciones
- Último mensaje: hace X días

(Nota: requiere endpoints adicionales — diseñar con datos de ejemplo)

Estados:
- Loading (skeleton)
- Datos cargados
- Error

Preparar componentes:
- UserDetailCard
- DataGrid
- ActionButtonGroup

Dark Mode Ready.
Accesibilidad AA.

---

#### G-03 — Modal: Banear Usuario
**Tipo:** Desktop (modal)  
**Objetivo:** Confirmar acción de ban con motivo  
**Componentes:**
- Nombre del usuario a banear
- Campo "Motivo" (textarea — se enviará en email automático)
- Botón "Confirmar ban" (rojo)
- Botón "Cancelar"

**Nota:** El `EmailService.sendAccountStatusChanged` está implementado — el motivo se puede incluir en el email.  
**Prioridad:** P0
**Promp**
Diseña el modal "Banear Usuario" del panel admin web de ZunoChat.

Objetivo:
Confirmar la acción de ban con posibilidad de ingresar un motivo que se enviará
automáticamente al usuario por email (vía Brevo EmailService).

Mantener el Design System del panel admin.

Nota técnica: El EmailService.sendAccountStatusChanged está implementado en backend.
El motivo puede incluirse en el email de notificación.

Presentación:
- Overlay oscuro semi-transparente sobre el contenido (faux-viewport en la UI)
- Modal centrado, ancho máximo 480px
- Bordes redondeados, fondo blanco

─── ESTRUCTURA DEL MODAL ───

Encabezado del modal:
- Ícono de advertencia (círculo rojo con signo !)
- Título: "Banear usuario"
- Botón X para cerrar (esquina superior derecha)

Información del usuario a banear:
- Avatar + Username + Email en una fila compacta
- Badge de estado actual: "ACTIVE"

Cuerpo:

Texto explicativo:
"Esta acción suspenderá la cuenta de @username inmediatamente.
El usuario recibirá un email de notificación."

Campo "Motivo del ban" (textarea):
- Placeholder: "Describe el motivo de la suspensión (opcional)..."
- Máximo 500 caracteres, contador visible
- El motivo se incluirá en el email enviado al usuario

Checkbox de confirmación:
☐ "Confirmo que deseo banear a @username"
(El botón de confirmar permanece deshabilitado hasta marcar el checkbox)

Footer del modal:

Botón "Cancelar" (outline, izquierda)
Botón "Confirmar ban" (rojo sólido, derecha)
- Deshabilitado hasta que el checkbox esté marcado
- Estado loading al confirmar

─── ESTADOS DEL MODAL ───

Normal: campos vacíos, checkbox sin marcar
Checkbox marcado: botón confirmar se habilita
Loading: spinner en botón, campos bloqueados
Éxito: modal se cierra, toast verde: "Usuario @username baneado correctamente"
Error: mensaje de error inline en el modal

Accesibilidad:
- Focus trap dentro del modal
- Escape para cerrar
- Enter no debe confirmar por accidente

Dark Mode Ready.

---

#### G-04 — Modal: Cambiar Rol
**Tipo:** Desktop (modal)  
**Objetivo:** Asignar nuevo rol a un usuario  
**Componentes:**
- Nombre del usuario
- Selector de rol: USER / ADMIN / SUPERADMIN
- Resumen de permisos del rol seleccionado (tabla compacta)
- Botón confirmar / cancelar

**Permisos:** Solo `roles:asignar` (SUPERADMIN)  
**Prioridad:** P1
**Promp**
Diseña el modal "Cambiar Rol de Usuario" del panel admin web de ZunoChat.

Objetivo:
Permitir a SUPERADMIN asignar un nuevo rol a un usuario con vista previa
de los permisos que tendrá con ese rol.

Solo accesible con permiso: roles:asignar (SUPERADMIN únicamente).

Mantener el Design System del panel admin.

Presentación:
- Modal centrado, ancho máximo 560px
- Overlay oscuro

─── ESTRUCTURA DEL MODAL ───

Encabezado:
- Ícono de escudo/rol
- Título: "Cambiar rol de usuario"
- Botón X para cerrar

Información del usuario:
- Avatar + Username + Email
- Badge del rol actual: ej. "USER"

Selector de nuevo rol:

3 opciones como radio buttons o cards seleccionables:

Card USER:
- Badge gris "USER"
- Descripción: "Acceso básico al chat"
- Lista compacta de permisos: chat:ver, chat:enviar, profile:ver, profile:editar

Card ADMIN:
- Badge azul "ADMIN"
- Descripción: "Acceso al panel de administración"
- Lista compacta de permisos: (incluye los de USER +) usuarios:ver, usuarios:bannear, dashboard:ver

Card SUPERADMIN:
- Badge violeta "SUPERADMIN"
- Descripción: "Control total del sistema"
- Lista compacta de permisos: (incluye los de ADMIN +) usuarios:eliminar, roles:asignar, config:editar
- Borde de acento violeta cuando está seleccionado

La card del rol actual aparece marcada por defecto.

Advertencia contextual:
Si se selecciona SUPERADMIN:
Banner amarillo: "⚠️ Estás a punto de otorgar acceso total al sistema. Esta acción es irreversible sin intervención manual."

Footer:

Botón "Cancelar" (outline)
Botón "Confirmar cambio" (color según rol seleccionado: gris/azul/violeta)
Deshabilitado si el rol seleccionado es el mismo que el actual.

Estados:
- Normal (rol actual seleccionado)
- Nuevo rol seleccionado (botón se habilita)
- SUPERADMIN seleccionado (banner de advertencia)
- Loading
- Éxito: modal cierra, toast: "Rol actualizado a ADMIN"
- Error: mensaje inline

Dark Mode Ready.
Accesibilidad AA.

---

### SECCIÓN H: ROLES Y PERMISOS

---

#### H-01 — Tabla de Roles y Permisos
**Tipo:** Desktop / Responsive  
**Objetivo:** Visualizar la matriz de permisos por rol  
**Componentes:**
- Tabla matriz: Permisos (filas) × Roles (columnas)
- Checkmarks por celda (solo lectura — los permisos están hardcoded en el enum)
- Descripción de cada permiso
- Leyenda de colores por rol

**Nota:** Los permisos están definidos en el enum `Role.java`. Para hacerlos editables se necesita refactor de la API (P3).  
**Prioridad:** P1
**Promp**
Diseña la pantalla web "Roles y Permisos" del panel admin de ZunoChat.

Objetivo:
Mostrar de forma clara la matriz completa de permisos del sistema
agrupados por módulo y con indicación por rol. Solo lectura.

Nota técnica: Los permisos están hardcoded en el enum Role.java del backend.
Para hacerlos editables se requeriría refactor de API (P3 futuro).

Mantener el Design System del panel admin.

─── HEADER DE PÁGINA ───

Título: "Roles y Permisos"
Subtítulo: "Matriz de permisos del sistema. Solo lectura — los roles están definidos en el backend."
Badge informativo: "Solo lectura"

─── LEYENDA DE ROLES ───

3 badges explicativos en fila:
- 🔵 ADMIN: gestión de usuarios y monitoreo
- 🟣 SUPERADMIN: control total del sistema
- ⚫ USER: acceso básico al chat

─── TABLA MATRIZ ───

Diseño: tabla fija con columnas para cada rol.

Encabezados de columna:
Permiso | Módulo | USER | ADMIN | SUPERADMIN

Filas agrupadas por módulo (separador visual entre grupos):

Módulo CHAT:
- chat:ver        | USER ✓ | ADMIN ✓ | SUPERADMIN ✓
- chat:enviar     | USER ✓ | ADMIN ✓ | SUPERADMIN ✓

Módulo PERFIL:
- profile:ver     | USER ✓ | ADMIN ✓ | SUPERADMIN ✓
- profile:editar  | USER ✓ | ADMIN ✓ | SUPERADMIN ✓

Módulo USUARIOS (Admin):
- usuarios:ver    | USER ✗ | ADMIN ✓ | SUPERADMIN ✓
- usuarios:bannear| USER ✗ | ADMIN ✓ | SUPERADMIN ✓
- usuarios:eliminar|USER ✗ | ADMIN ✗ | SUPERADMIN ✓

Módulo ROLES:
- roles:asignar   | USER ✗ | ADMIN ✗ | SUPERADMIN ✓

Módulo DASHBOARD:
- dashboard:ver   | USER ✗ | ADMIN ✓ | SUPERADMIN ✓
- dashboard:editar| USER ✗ | ADMIN ✓ | SUPERADMIN ✓

Módulo CONFIGURACIÓN:
- config:ver      | USER ✗ | ADMIN ✗ | SUPERADMIN ✓
- config:editar   | USER ✗ | ADMIN ✗ | SUPERADMIN ✓

Diseño de celdas:
✓ (check verde) = permiso concedido
✗ (X roja o –  gris) = no tiene permiso

Columna de rol con header coloreado según rol:
- USER: fondo gris suave
- ADMIN: fondo azul suave
- SUPERADMIN: fondo violeta suave

Filas alternando fondo blanco / gris muy claro para legibilidad.

─── NOTA AL PIE ───

"Los permisos son asignados automáticamente según el rol. Para modificarlos, se requiere intervención técnica en el backend."

Dark Mode Ready.
Responsive: en mobile, convertir a vista de cards por permiso con badges de rol.
Accesibilidad AA.

---

### SECCIÓN I: CONFIGURACIÓN DEL SISTEMA

---

#### I-01 — Configuración — Email
**Tipo:** Desktop / Responsive  
**Objetivo:** Controlar el servicio de envío de emails en tiempo real  
**Componentes:**
- Card con estado actual: "Servicio de email: ACTIVO / INACTIVO"
- Toggle de activación/desactivación
- Información de auditoría: "Modificado por: [admin_id] el [fecha]"
- Botón "Guardar"
- Confirmación antes de desactivar (modal)

**Permisos:**
- `config:ver` → ver estado actual
- `config:editar` → modificar (solo SUPERADMIN)

**Prioridad:** P0
**Promp**
Diseña la pantalla web "Configuración — Email" del panel admin de ZunoChat.

Objetivo:
Permitir a SUPERADMIN controlar en tiempo de ejecución el servicio de envío
de emails transaccionales (Brevo) sin necesidad de reiniciar el servidor.

Mantener el Design System del panel admin.

─── HEADER DE PÁGINA ───

Breadcrumb: Configuración › Email
Título: "Configuración de Email"
Subtítulo: "Controla el servicio de correo transaccional en tiempo real."
Badge de permiso: "Solo SUPERADMIN" (violeta)

─── CARD PRINCIPAL: ESTADO DEL SERVICIO ───

Card grande centrada (max 640px) con:

Estado actual visual:
- Si ACTIVO:
    - Ícono de sobre grande en verde
    - Título: "Servicio de email activo"
    - Badge verde: "ACTIVO"
    - Descripción: "Los emails transaccionales se están enviando correctamente."

- Si INACTIVO:
    - Ícono de sobre con X, en rojo
    - Título: "Servicio de email desactivado"
    - Badge rojo: "INACTIVO"
    - Descripción: "No se enviarán emails hasta que reactives el servicio."

Toggle grande y visible:
"Activar servicio de email"
[Toggle ON/OFF]

Al desactivar → modal de confirmación antes de aplicar:
Título: "¿Desactivar el servicio de email?"
"No se enviarán OTPs, notificaciones de ban ni emails de bienvenida mientras esté desactivado."
Botón: "Cancelar" / "Desactivar"

─── CARD SECUNDARIA: AUDITORÍA ───

Título de sección: "Último cambio"

Datos:
- Modificado por: [avatar + username del admin]
- Fecha y hora: DD/MM/YYYY HH:MM
- Acción realizada: "Activó el servicio" / "Desactivó el servicio"

Si nunca ha sido modificado: "Sin cambios registrados aún"

─── CARD INFORMATIVA: EMAILS CONFIGURADOS ───

Lista de tipos de email gestionados por este servicio:
- 📧 OTP de verificación (registro)
- 👋 Email de bienvenida
- 🚫 Notificación de ban de cuenta
- 🔒 Recuperación de contraseña (endpoint pendiente)

─── ACCIONES (footer de la página) ───

Botón "Guardar cambios" (verde, activo si hay cambios pendientes)
Botón "Descartar cambios" (outline, activo si hay cambios pendientes)

Estados:
- Cargando estado inicial (skeleton)
- Estado normal sin cambios (botones deshabilitados)
- Cambio pendiente (botones habilitados)
- Loading al guardar
- Éxito: toast "Configuración guardada"
- Error: banner rojo

Dark Mode Ready.
Accesibilidad AA.
---

#### I-02 — Configuración — General (Futuro)
**Tipo:** Desktop  
**Objetivo:** Configuraciones expandibles del sistema  
**Posibles secciones (P2-P3):**
- Límites de mensajes por usuario
- Configuración de JWT (tiempo de expiración)
- Mantenimiento programado
- Feature flags avanzados

**Prioridad:** P2

---

### SECCIÓN J: LOGS Y AUDITORÍA

---

#### J-01 — Log de Actividad Admin
**Tipo:** Desktop  
**Objetivo:** Ver registro de acciones administrativas  
**Componentes:**
- Filtros: por acción / por admin / por rango de fechas
- Tabla: Timestamp / Admin / Acción / Entidad afectada / IP
- Paginación
- Exportar (P2)

**Nota:** Esta funcionalidad **no está implementada en el API**. Requiere crear tabla `audit_log` y middleware de auditoría.  
**Prioridad:** P1

**Promp**
Diseña la pantalla web "Log de Actividad Administrativa" del panel admin de ZunoChat.

Objetivo:
Mostrar el registro de todas las acciones realizadas por administradores
(bans, cambios de rol, cambios de configuración, etc.)

Nota técnica: Esta funcionalidad NO está implementada en el API actual.
Requiere crear tabla audit_log y middleware de auditoría en el backend.
Diseñar con datos de ejemplo para prototipo.

Mantener el Design System del panel admin.

─── HEADER DE PÁGINA ───

Título: "Log de Actividad"
Subtítulo: "Registro de todas las acciones administrativas del sistema."

Banner informativo amarillo (mientras el endpoint no esté implementado):
"⚠️ Esta sección muestra datos de ejemplo. El sistema de auditoría está pendiente de implementación en el backend."

─── BARRA DE FILTROS ───

Fila horizontal:

1. Rango de fechas: [Desde] [Hasta] (date pickers)
2. Filtro por Acción:
   Dropdown: Todas / Ban usuario / Activar usuario / Cambiar rol / Config email / Eliminar usuario
3. Filtro por Admin:
   Dropdown o buscador: Todos los admins / [lista de admins]
4. Botón "Aplicar filtros"
5. Botón "Limpiar"

─── TABLA DE LOGS ───

Columnas:
Timestamp | Admin | Acción | Usuario afectado | Detalle | IP

Por fila:
- Timestamp: DD/MM/YYYY HH:MM:SS (monospace)
- Admin: avatar pequeño (24px) + username
- Acción: badge con color semántico:
    - Ban: badge rojo "BAN"
    - Activar: badge verde "ACTIVAR"
    - Cambiar rol: badge azul "ROL"
    - Config: badge naranja "CONFIG"
    - Eliminar: badge rojo oscuro "ELIMINAR"
- Usuario afectado: username con link a G-02
- Detalle: texto corto (ej: "Baneado por spam" / "Rol cambiado de USER a ADMIN")
- IP: dirección IP (monospace, color secundario)

Paginación: igual que G-01

─── ESTADO SIN DATOS ───

Ilustración de lista vacía
Texto: "No hay registros de actividad para los filtros seleccionados."

─── EXPORTAR ─── (P2)

Botón "Exportar CSV" en el header de la tabla (deshabilitado con badge "Próximamente")

Dark Mode Ready.
Accesibilidad AA.
Responsive: en tablet, colapsar columnas IP y Detalle.

---

#### J-02 — Log de Emails Enviados
**Tipo:** Desktop  
**Objetivo:** Ver historial de emails transaccionales  
**Componentes:**
- Filtros: por tipo (OTP/WELCOME/BAN/RESET) / por email destino / por rango de fechas
- Tabla: Timestamp / Tipo / Destinatario / Estado (enviado/fallido)

**Nota:** Requiere implementación en API.  
**Prioridad:** P2

---

### SECCIÓN K: MÉTRICAS

---

#### K-01 — Métricas de Uso
**Tipo:** Desktop  
**Objetivo:** Estadísticas de uso de la plataforma  
**Componentes:**
- Total mensajes (por día/semana/mes)
- Usuarios activos (DAU/WAU/MAU)
- Conversaciones iniciadas
- Tipos de mensajes (TEXT vs FILE vs PAYLOAD)
- Gráficos de tendencias

**Nota:** Requiere implementación en API.  
**Prioridad:** P2

**Promp**
Diseña la pantalla web "Métricas de Uso" del panel admin de ZunoChat.
Objetivo:
Mostrar estadísticas detalladas de uso de la plataforma para análisis de crecimiento y comportamiento, complementando el Dashboard general (F-01) con vistas más profundas y filtrables.
Nota técnica: Esta funcionalidad requiere endpoints de analytics no implementados en el API actual. Diseñar con datos de ejemplo realistas para prototipo.
Mantener el Design System del panel admin (sidebar igual que F-01).
─── HEADER DE PÁGINA ───
Título: "Métricas de Uso"
Subtítulo: "Analiza el comportamiento y crecimiento de la plataforma."
Banner informativo amarillo: "⚠️ Esta sección muestra datos de ejemplo. Los endpoints de analytics están pendientes de implementación."
Selector de rango de fechas (esquina superior derecha):
Botones tipo tabs: "7 días" / "30 días" / "90 días" / "Personalizado" (con date pickers)
─── FILA 1: KPI CARDS DE USUARIOS ACTIVOS ───
Grid de 3 tarjetas:
Card 1: DAU (Daily Active Users)

Número grande
Sparkline de los últimos 7 días
Variación: "+5% vs período anterior"

Card 2: WAU (Weekly Active Users)

Número grande
Sparkline
Variación

Card 3: MAU (Monthly Active Users)

Número grande
Sparkline
Variación
Ratio DAU/MAU como indicador de "stickiness" (ej: "Stickiness: 23%")

─── FILA 2: GRÁFICO DE MENSAJES ───
Gráfico grande de líneas (ancho completo):
Título: "Mensajes enviados en el tiempo"
Eje X: fechas según rango seleccionado
Eje Y: cantidad de mensajes
Múltiples líneas/series con leyenda:

Total mensajes
Mensajes TEXT
Mensajes IMAGE/FILE
Mensajes PAYLOAD (sumados)
Toggle de series (click en leyenda oculta/muestra línea)
Tooltip al hover con desglose por tipo

─── FILA 3: DISTRIBUCIÓN POR TIPO + CONVERSACIONES ───
Panel izquierdo (40%): "Distribución por tipo de mensaje"

Gráfico de dona/pie:

TEXT (verde primario)
IMAGE (azul)
FILE (naranja)
PAYLOAD/SALES (amarillo)
PAYLOAD/SYSTEM (gris)
PAYLOAD/SURVEY (celeste)
PAYLOAD/CARD (violeta)


Leyenda con porcentajes y cantidades absolutas

Panel derecho (60%): "Conversaciones"

Gráfico de barras: "Conversaciones nuevas por día"
KPI inline arriba del gráfico:

Total conversaciones activas
Promedio de mensajes por conversación
Conversaciones sin actividad en 7+ días (inactivas)



─── FILA 4: TABLA DE USUARIOS MÁS ACTIVOS ───
Título: "Top 10 usuarios más activos"
Tabla:
Avatar+Username | Mensajes enviados | Conversaciones | Última actividad
Ordenable por columna
Link a G-02 (Detalle de Usuario) en cada fila
─── FILA 5: HORARIOS DE ACTIVIDAD ───
Heatmap (mapa de calor):
Título: "Actividad por hora y día de la semana"
Eje X: horas del día (0-23)
Eje Y: días de la semana (Lun-Dom)
Intensidad de color según volumen de mensajes (escala de verde claro a verde oscuro #22C55E)
Tooltip con valor exacto al hover
─── ACCIONES ───
Botón "Exportar reporte" (PDF/CSV) — deshabilitado con badge "Próximamente"
Botón "Actualizar datos" (ícono refresh)
Estados:

Loading inicial (skeleton en todas las cards, gráficos y tabla)
Datos cargados
Sin datos para el rango seleccionado: ilustración + "No hay datos disponibles para este período"
Error de carga: banner rojo + reintentar

Preparar componentes reutilizables:

MetricCard (con sparkline)
LineChartMultiSeries
DonutChart
HeatmapCalendar
TopUsersTable
DateRangeSelector

Responsive: en tablet, apilar paneles verticalmente; heatmap con scroll horizontal.
Dark Mode Ready.
Accesibilidad AA (no depender solo del color para el heatmap — incluir valores numéricos).


---

## PANTALLAS FALTANTES VS COMPETENCIA

### Comparativa con WhatsApp / Telegram / Discord / Slack

| Funcionalidad | WhatsApp | Telegram | Discord | Slack | ZunoChat actual | Prioridad |
|---|---|---|---|---|---|---|
| Chats grupales | ✅ | ✅ | ✅ | ✅ | ❌ No implementado | P2 |
| Llamadas de voz | ✅ | ✅ | ✅ | ✅ | ❌ | P3 |
| Videollamadas | ✅ | ✅ | ✅ | ✅ | ❌ | P3 |
| Mensajes de voz | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Estados/Stories | ✅ | ✅ | ❌ | ❌ | ❌ | P3 |
| Reacciones a mensajes | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Responder mensajes (reply) | ✅ | ✅ | ✅ | ✅ | ❌ | P1 |
| Reenviar mensajes | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Eliminar mensajes | ✅ | ✅ | ✅ | ✅ | ❌ API pendiente | P1 |
| Editar mensajes | ❌ (reciente) | ✅ | ✅ | ✅ | ❌ | P2 |
| Mensajes temporales | ✅ | ✅ | ❌ | ❌ | ❌ | P3 |
| Buscar en mensajes | ✅ | ✅ | ✅ | ✅ | ❌ | P1 |
| Archivar conversaciones | ✅ | ✅ | ❌ | ✅ | ❌ | P2 |
| Silenciar conversaciones | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Bloquear usuarios | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Contactos / Directorio | ✅ | ✅ | ✅ | ✅ | ❌ Solo búsqueda | P1 |
| Notificaciones push | ✅ | ✅ | ✅ | ✅ | ❌ API pendiente | P1 |
| Gestor de archivos | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Links con preview | ✅ | ✅ | ✅ | ✅ | ❌ | P2 |
| Stickers / GIFs | ✅ | ✅ | ✅ | ✅ | ❌ | P3 |
| Multi-dispositivo | ✅ | ✅ | ✅ | ✅ | ⚠️ WS registry listo | P2 |
| Tema oscuro | ✅ | ✅ | ✅ | ✅ | ❌ | P1 |
| Recovery de cuenta | ✅ | ✅ | ✅ | ✅ | ⚠️ Email listo, falta endpoint | P1 |

### Pantallas faltantes identificadas (sin precedente en API actual)

Las siguientes pantallas requieren **nuevos endpoints** en el backend:

1. **Gestión de contactos** — lista de usuarios con quienes se ha hablado
2. **Chats grupales** — crear grupo, info de grupo, agregar/eliminar miembros, roles
3. **Búsqueda en mensajes** — buscar texto dentro de una conversación
4. **Papelera / mensajes eliminados** — soft delete con recuperación
5. **Sesiones activas** — ver y cerrar sesiones en otros dispositivos
6. **Notificaciones push** — preferencias y tokens de dispositivo (FCM/APNs)
7. **Bloqueo de usuarios** — tabla `blocked_users` pendiente
8. **Centro de ayuda / FAQ** — pantalla estática

---

## USER FLOWS

---

### FLOW 1 — Onboarding Completo (Usuario Nuevo)

```
Splash Screen
    ↓ Tap "Crear cuenta"
Pantalla de Registro
    ↓ Completar DNI, username, email, password
    ↓ Tap "Registrarse"
    → API: POST /api/auth/register
    ↓ Éxito → OTP enviado al email
Pantalla Verificar OTP
    ↓ Ingresar 6 dígitos
    ↓ Tap "Verificar"
    → API: POST /api/auth/verify-otp
    ↓ JWT guardado en dispositivo
    ↓ Email de bienvenida enviado automáticamente
Lista de Conversaciones (vacía)
    ↓ Empty state: "¡Bienvenido! Busca a alguien para comenzar"
    ↓ Tap FAB
Buscar Usuario
    ↓ Escribir username (mín. 2 chars)
    → API: GET /api/users/search?q=xxx
    ↓ Tap en resultado
    → API: POST /api/conversations
Chat Individual (vacío)
    ↓ Escribir primer mensaje
    → WebSocket: /app/chat.send (o REST POST /api/messages)
    ↓ Mensaje enviado ✓
    ↓ Receptor recibe notificación en tiempo real
    ↓ Receptor lee → ✓✓ azul
```

---

### FLOW 2 — Login y Uso Cotidiano

```
Splash Screen
    ↓ Tap "Iniciar sesión"
Pantalla de Login
    ↓ Ingresar username/email + contraseña
    → API: POST /api/auth/login
    ↓ JWT guardado
Lista de Conversaciones
    ↓ Se carga inbox paginado
    → API: GET /api/conversations?page=0&size=20
    ↓ WebSocket conectado → eventos en tiempo real
    ↓ Ver badge de no leídos
    ↓ Tap en conversación con mensajes nuevos
Chat Individual
    → API: GET /api/messages?conversationId=X
    → WebSocket: PATCH automático /api/messages/read
    ↓ Mensajes propios cambian a ✓✓ azul
    ↓ Escribir respuesta → "Escribiendo..." en el otro lado
    → WebSocket: /app/chat.typing
    ↓ Enviar → entrega en tiempo real
    ↓ Receptor escribe → "Escribiendo..." aparece
    → WebSocket suscrito a /topic/typing.{id}
```

---

### FLOW 3 — Administrador Baneo de Usuario

```
Login Admin (Web)
    → POST /api/auth/login (rol ADMIN)
    ↓ JWT con permissions: [usuarios:bannear, usuarios:ver, ...]
Dashboard
    ↓ Tap "Gestión de usuarios"
Listado de Usuarios
    → GET /api/admin/usuarios
    ↓ Filtrar por estado ACTIVE
    ↓ Buscar usuario problemático
    ↓ Tap acción "Banear"
Modal: Confirmar Ban
    ↓ Ingresar motivo (opcional)
    ↓ Confirmar
    → PATCH /api/admin/usuarios/{id}/ban
    ↓ Email enviado al usuario: "Tu cuenta ha sido suspendida"
    ↓ Si usuario estaba online → próximo request devuelve 403
    ↓ Toast de éxito en panel admin
Listado actualizado
    ↓ Usuario aparece con badge "BANNED"
```

---

### FLOW 4 — SUPERADMIN Configuración de Email

```
Login Admin (Web)
    → POST /api/auth/login (rol SUPERADMIN)
Dashboard
    ↓ Card de estado del sistema muestra "Email: ACTIVO"
    ↓ Navegar a Configuración → Email
Config Email
    → GET /api/config/email
    ↓ Muestra: enabled=true, updatedAt, updatedBy
    ↓ Desactivar toggle
    ↓ Modal de confirmación: "¿Estás seguro? No se enviarán más emails."
    ↓ Confirmar
    → PUT /api/config/email { "enabled": false }
    ↓ Toast: "Servicio de correo desactivado"
    ↓ Card del dashboard se actualiza → "Email: INACTIVO"
```

---

### FLOW 5 — Envío de Mensaje con Archivo

```
Chat Individual
    ↓ Tap ícono de adjunto
Bottom Sheet Adjunto
    ↓ Seleccionar "Documento"
Selector de Archivos (nativo)
    ↓ Elegir archivo (máx. 3, 5MB c/u)
    ↓ [Pendiente API] Upload al storage (S3/Cloudinary)
    ↓ Obtener URL del archivo
Chat Individual
    → POST /api/messages { type: "FILE", fileUrls: ["https://..."] }
    ↓ Burbuja de archivo aparece con ícono y nombre
    → WebSocket: receptor recibe evento con fileUrls
    ↓ Receptor ve burbuja de archivo con botón descargar
```

---

## ARQUITECTURA DE NAVEGACIÓN

### Mobile App — Estructura de Navegación

```
┌─────────────────────────────────────────────────────┐
│              AUTH STACK (no autenticado)             │
│   Splash → Registro → Verificar OTP                 │
│         ↘ Login → Recuperar Contraseña              │
└────────────────────────┬────────────────────────────┘
                         ↓ (JWT guardado)
┌─────────────────────────────────────────────────────┐
│              MAIN TAB NAVIGATOR                     │
│                                                     │
│   [💬 Chats]    [👤 Perfil]    [⚙️ Config]         │
│       │              │               │             │
│   ┌───┴───┐    ┌──────┴──────┐  ┌───┴───┐         │
│   │ Inbox │    │  Mi Perfil  │  │Config │         │
│   │  List │    │  Cambiar PW │  │Notifs │         │
│   └───┬───┘    └─────────────┘  └───────┘         │
│       │                                             │
│   ┌───┴──────────────────────┐                     │
│   │  CHAT STACK              │                     │
│   │  Nueva Conversación      │                     │
│   │  → Chat Individual       │                     │
│   │     → Visor de Imagen    │                     │
│   │     → Perfil del otro    │                     │
│   └──────────────────────────┘                     │
└─────────────────────────────────────────────────────┘
```

### Web Admin — Estructura de Navegación

```
┌─────────────────────────────────────────────────────┐
│              LOGIN ADMIN                             │
└────────────────────────┬────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│   SIDEBAR NAVIGATION                                │
│                                                     │
│   📊 Dashboard                                      │
│   👥 Usuarios                                       │
│      ├── Listado                                    │
│      ├── [Detalle de usuario] (modal/drawer)        │
│      └── [Banear / Cambiar rol] (modales)           │
│   🔐 Roles y Permisos                               │
│      └── Tabla de matriz                            │
│   ⚙️  Configuración                                 │
│      ├── Email                                      │
│      └── General (P2)                               │
│   📋 Logs y Auditoría                               │
│      ├── Actividad admin                            │
│      └── Emails (P2)                                │
│   📈 Métricas (P2)                                  │
│   ─────────────────                                 │
│   Mi cuenta admin                                   │
│   Cerrar sesión                                     │
└─────────────────────────────────────────────────────┘
```

---

## PRIORIDAD DE DESARROLLO

### P0 — Críticas (MVP mínimo funcional)

| ID | Pantalla | Plataforma | Bloqueante para |
|---|---|---|---|
| A-01 | Splash / Onboarding | Mobile | Todo el flujo |
| A-02 | Registro | Mobile | Nuevos usuarios |
| A-03 | Verificación OTP | Mobile | Activación de cuenta |
| A-04 | Login | Mobile | Acceso al sistema |
| B-01 | Lista de Conversaciones | Mobile | Flujo principal |
| B-02 | Nueva Conversación / Búsqueda | Mobile | Iniciar chats |
| B-03 | Chat Individual | Mobile | Mensajería |
| B-04 | Burbujas especiales (todos los tipos) | Mobile | Mensajería |
| E-01 | Login Admin | Web | Panel admin |
| F-01 | Dashboard | Web | Visibilidad del sistema |
| G-01 | Listado de Usuarios | Web | Gestión usuarios |
| G-03 | Modal Banear Usuario | Web | Moderación |
| I-01 | Config Email | Web | Control del sistema |

### P1 — Importantes (necesarias para producto completo)

| ID | Pantalla | Plataforma | Depende de |
|---|---|---|---|
| A-05 | Recuperar Contraseña (solicitud) | Mobile | Endpoint API pendiente |
| A-06 | Recuperar Contraseña (nueva PW) | Mobile | Endpoint API pendiente |
| B-05 | Visor de Imagen | Mobile | Mensajes con imágenes |
| B-06 | Selector de Adjunto | Mobile | Envío de archivos |
| C-01 | Mi Perfil | Mobile | Editar datos |
| C-02 | Cambiar Contraseña | Mobile | Endpoint API pendiente |
| D-01 | Configuración General | Mobile | Notificaciones push |
| D-02 | Notificaciones | Mobile | Firebase/APNs pendiente |
| G-02 | Detalle de Usuario | Web | Info completa |
| G-04 | Modal Cambiar Rol | Web | Gestión de roles |
| H-01 | Tabla de Roles y Permisos | Web | Transparencia |
| J-01 | Log de Actividad Admin | Web | Auditoría — API pendiente |

### P2 — Deseables (mejoran significativamente UX)

| ID | Pantalla | Notas |
|---|---|---|
| C-03 | Perfil de otro usuario | Requiere API |
| Reacciones a mensajes | Requiere nuevos endpoints |
| Responder mensaje (reply) | Requiere campo `replyToMessageId` en API |
| Archivar / silenciar conversación | Requiere tabla de preferencias en DB |
| Bloqueo de usuarios | Requiere tabla `blocked_users` |
| Búsqueda en mensajes | Requiere endpoint de búsqueda full-text |
| Mensajes de voz | Requiere storage y nuevo `MessageType` |
| Reenviar mensajes | Lógica de copia de mensaje |
| J-02 | Log de Emails | Requiere persistencia en API |
| K-01 | Métricas de Uso | Requiere endpoints de analytics |
| Multi-dispositivo mejorado | WS session registry ya implementado |
| Tema oscuro | Solo frontend |
| I-02 | Configuración General | Expandir `app_config` |

### P3 — Futuras (roadmap largo plazo)

| Funcionalidad | Notas |
|---|---|
| Chats grupales | Rediseño del modelo de datos |
| Llamadas de voz/video | WebRTC — infraestructura nueva |
| Estados / Stories | Nuevo módulo completo |
| Stickers y GIFs | Integración con servicios externos |
| Mensajes temporales | TTL en mensajes |
| Editar mensajes | Campo `editedAt` + historial |
| Papelera / recuperación | Soft delete extendido |
| Videollamadas | WebRTC + TURN servers |
| Integraciones / Webhooks | Nuevo módulo |
| Bots / API pública | Nuevo módulo |
| Pantallas grandes (Tablet/iPad) | Layouts split-view |

---

## COMPONENTES DE DISEÑO REUTILIZABLES

> Para ser definidos como Design System en Figma antes de comenzar con pantallas individuales.

### Átomos
- `Avatar` — circular, con indicador de presencia (punto verde/gris/animado)
- `Badge` — contador numérico (unread count)
- `StatusBadge` — ACTIVE / BANNED / ONLINE / OFFLINE / TYPING
- `RoleBadge` — USER / ADMIN / SUPERADMIN
- `PasswordStrengthIndicator` — barra de fortaleza
- `OtpInput` — 6 campos individuales para código OTP
- `MessageTick` — ✓ / ✓✓ gris / ✓✓ azul

### Moléculas
- `ConversationListItem` — avatar + nombre + preview + timestamp + badge
- `MessageBubble` — burbuja con tipo TEXT / IMAGE / FILE / PAYLOAD
- `TypingIndicator` — "..." animado
- `UserSearchResultItem` — avatar + username
- `AdminUserTableRow` — fila de usuario con acciones
- `KPICard` — número grande + etiqueta + variación
- `SystemStatusCard` — servicio + estado + toggle

### Organismos
- `ChatInput` — barra completa de entrada de mensaje
- `ChatHeader` — header de conversación con presencia
- `ConversationList` — lista completa con estados
- `UserManagementTable` — tabla con filtros y paginación
- `PermissionsMatrix` — tabla de roles × permisos

### Plantillas
- `AuthLayout` — centrado, branding, sin nav
- `ChatLayout` — header + lista scrolleable + footer fijo
- `InboxLayout` — header + tab nav + lista
- `AdminLayout` — sidebar + main content + breadcrumbs

---

## NOTAS PARA FIGMA MAKE AI

Al usar este documento como entrada para Figma Make AI, incluir las siguientes instrucciones adicionales:

1. **Paleta de color:** Proponer una paleta moderna similar a WhatsApp (verde/blanco) o Telegram (azul/blanco). El nombre "Zuno" sugiere algo fresco/minimalista.

2. **Tipografía:** Sans-serif legible. Recomendado: Inter o SF Pro para mobile.

3. **Densidad:** Alta densidad de información en inbox (como WhatsApp). Burbujas con tipografía clara y contraste WCAG AA mínimo.

4. **Tiempo real visual:** Los cambios de estado (typing, presencia, read receipts) deben tener micro-animaciones suaves (200-300ms).

5. **Web Admin:** Material Design 3 o shadcn/ui como referencia visual. Sidebar colapsable para pantallas medianas.

6. **Accesibilidad:** Soporte mínimo de tamaño de fuente dinámica en mobile. Contraste mínimo AA.

7. **Edge cases críticos a diseñar:** mensajes muy largos, nombres de usuario muy largos, 99+ mensajes no leídos, estado de error en envío de mensaje, chat vacío.

---

*Documento generado automáticamente mediante análisis del código fuente de `zunochat-api`. Revisar con el equipo de desarrollo antes de comenzar el diseño en Figma.*