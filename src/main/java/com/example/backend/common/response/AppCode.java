package com.example.backend.common.response;

import org.springframework.http.HttpStatus;

/**
 * Catálogo centralizado de códigos del sistema.
 *
 * Cada entrada define:
 *  - code        → identificador único legible por el frontend (ej: "OTP_EXPIRED")
 *  - httpStatus  → código HTTP asociado
 *  - message     → mensaje por defecto (puede sobreescribirse al lanzar)
 *
 * Convención de prefijos:
 *  AUTH_*    → autenticación / sesión
 *  USER_*    → gestión de usuarios
 *  OTP_*     → verificación OTP
 *  VALID_*   → validaciones de entrada
 *  SYS_*     → errores internos del sistema
 *  OK_*      → respuestas de éxito
 */
public enum AppCode {

    // ─── Éxito ────────────────────────────────────────────────────────────────

    OK_GENERIC              (HttpStatus.OK,                    "Operación exitosa"),
    OK_CREATED              (HttpStatus.CREATED,               "Recurso creado exitosamente"),
    OK_REGISTER             (HttpStatus.CREATED,               "Registro exitoso. Revisa tu correo para el código OTP"),
    OK_OTP_VERIFIED         (HttpStatus.OK,                    "Cuenta verificada correctamente"),
    OK_LOGIN                (HttpStatus.OK,                    "Sesión iniciada correctamente"),
    OK_USER_BANNED          (HttpStatus.OK,                    "Usuario baneado correctamente"),
    OK_USER_ACTIVATED       (HttpStatus.OK,                    "Usuario activado correctamente"),
    OK_ROLE_ASSIGNED        (HttpStatus.OK,                    "Rol asignado correctamente"),
    OK_USER_DELETED         (HttpStatus.OK,                    "Usuario eliminado correctamente"),
    OK_USERS_LISTED         (HttpStatus.OK,                    "Usuarios obtenidos correctamente"),
    OK_PASSWORD_RESET_SENT  (HttpStatus.OK,                    "Si el correo existe, enviamos un código para restablecer la contraseña"),
    OK_PASSWORD_RESET       (HttpStatus.OK,                    "Contraseña restablecida correctamente"),
    OK_OTP_RESENT           (HttpStatus.OK,                    "Código reenviado. Revisa tu correo"),
    OK_TOKEN_REFRESHED      (HttpStatus.OK,                    "Sesión renovada"),
    OK_MESSAGE_DELETED      (HttpStatus.OK,                    "Mensaje eliminado"),
    OK_MESSAGE_EDITED       (HttpStatus.OK,                    "Mensaje editado"),
    OK_USER_BLOCKED         (HttpStatus.OK,                    "Usuario bloqueado"),
    OK_USER_UNBLOCKED       (HttpStatus.OK,                    "Usuario desbloqueado"),
    OK_CONVERSATION_MUTED   (HttpStatus.OK,                    "Conversación silenciada"),
    OK_CONVERSATION_UNMUTED (HttpStatus.OK,                    "Conversación reactivada"),
    OK_FILES_UPLOADED       (HttpStatus.CREATED,               "Archivo(s) subido(s) correctamente"),
    OK_THEME_UPDATED        (HttpStatus.OK,                    "Preferencia de tema actualizada"),
    OK_GOOGLE_AUTH           (HttpStatus.OK,                   "Autenticado con Google correctamente"),
    OK_USERNAME_UPDATED     (HttpStatus.OK,                    "Nombre de usuario actualizado"),
    OK_PASSWORD_UPDATED     (HttpStatus.OK,                    "Contraseña actualizada correctamente"),
    OK_AVATAR_UPDATED       (HttpStatus.OK,                    "Foto de perfil actualizada"),
    OK_EMAIL_CHANGE_REQUESTED(HttpStatus.OK,                   "Enviamos un código de verificación a tu correo nuevo"),
    OK_EMAIL_UPDATED        (HttpStatus.OK,                    "Correo electrónico actualizado correctamente"),
    OK_GROUP_MEMBERS_ADDED  (HttpStatus.OK,                    "Miembros agregados al grupo"),
    OK_GROUP_MEMBER_REMOVED (HttpStatus.OK,                    "Miembro eliminado del grupo"),
    OK_GROUP_LEFT           (HttpStatus.OK,                    "Saliste del grupo"),
    OK_GROUP_ROLE_UPDATED   (HttpStatus.OK,                    "Rol del miembro actualizado"),
    OK_GROUP_OWNERSHIP_TRANSFERRED(HttpStatus.OK,              "Propiedad del grupo transferida"),
    OK_AUDIT_LOG_LISTED     (HttpStatus.OK,                    "Historial obtenido correctamente"),

    // ─── Auth ─────────────────────────────────────────────────────────────────

    AUTH_BAD_CREDENTIALS    (HttpStatus.UNAUTHORIZED,          "Credenciales incorrectas"),
    AUTH_TOKEN_EXPIRED      (HttpStatus.UNAUTHORIZED,          "El token JWT ha expirado"),
    AUTH_TOKEN_INVALID      (HttpStatus.UNAUTHORIZED,          "El token JWT es inválido o está malformado"),
    AUTH_TOKEN_MISSING      (HttpStatus.UNAUTHORIZED,          "Se requiere autenticación"),
    AUTH_FORBIDDEN          (HttpStatus.FORBIDDEN,             "No tienes permisos para realizar esta acción"),
    AUTH_REFRESH_EXPIRED    (HttpStatus.UNAUTHORIZED,          "La sesión expiró hace demasiado tiempo. Vuelve a iniciar sesión"),
    AUTH_RATE_LIMITED       (HttpStatus.TOO_MANY_REQUESTS,     "Demasiados intentos. Espera unos minutos antes de volver a intentar"),
    GOOGLE_AUTH_INVALID_CODE      (HttpStatus.UNAUTHORIZED,    "No se pudo validar la autenticación con Google. Intenta de nuevo"),
    GOOGLE_AUTH_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN,       "Tu cuenta de Google no tiene el correo verificado"),
    GOOGLE_REGISTRATION_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "La sesión de registro con Google expiró o es inválida. Vuelve a intentarlo"),

    // ─── Usuario ──────────────────────────────────────────────────────────────

    USER_NOT_FOUND          (HttpStatus.NOT_FOUND,             "Usuario no encontrado"),
    USER_BANNED             (HttpStatus.FORBIDDEN,             "Tu cuenta ha sido suspendida. Contacta al soporte"),
    USER_INACTIVE           (HttpStatus.FORBIDDEN,             "Tu cuenta está inactiva"),
    USER_ALREADY_ACTIVE     (HttpStatus.BAD_REQUEST,           "La cuenta ya está verificada y activa"),
    USER_DNI_EXISTS         (HttpStatus.CONFLICT,              "El DNI ya está registrado"),
    USER_USERNAME_EXISTS    (HttpStatus.CONFLICT,              "El nombre de usuario ya está en uso"),
    USER_EMAIL_EXISTS       (HttpStatus.CONFLICT,              "El correo electrónico ya está registrado"),
    USER_SELF_ACTION        (HttpStatus.BAD_REQUEST,           "No puedes realizar esta acción sobre tu propia cuenta"),
    USER_ROLE_INVALID       (HttpStatus.BAD_REQUEST,           "Rol inválido"),
    USER_INSUFFICIENT_RANK  (HttpStatus.FORBIDDEN,             "No tienes rango suficiente para moderar a este usuario"),
    USER_ALREADY_BLOCKED    (HttpStatus.CONFLICT,              "Ya bloqueaste a este usuario"),
    USER_NOT_BLOCKED        (HttpStatus.BAD_REQUEST,           "No has bloqueado a este usuario"),
    USER_BLOCKED_CONTACT    (HttpStatus.FORBIDDEN,             "No puedes enviar mensajes a este usuario"),
    USER_CURRENT_PASSWORD_INVALID(HttpStatus.BAD_REQUEST,      "La contraseña actual es incorrecta"),
    USER_NO_PASSWORD_SET    (HttpStatus.BAD_REQUEST,           "Tu cuenta no tiene contraseña (inició sesión con Google) — no hay contraseña que cambiar"),
    USER_EMAIL_SAME         (HttpStatus.BAD_REQUEST,           "Ese ya es tu correo actual"),
    USER_EMAIL_CHANGE_NOT_REQUESTED(HttpStatus.BAD_REQUEST,    "No hay un cambio de correo en curso. Solicítalo de nuevo"),

    // ─── OTP ──────────────────────────────────────────────────────────────────

    OTP_INVALID             (HttpStatus.BAD_REQUEST,           "El código OTP es incorrecto"),
    OTP_EXPIRED              (HttpStatus.BAD_REQUEST,           "El código OTP ha expirado. Solicita uno nuevo"),
    OTP_PENDING_REQUIRED    (HttpStatus.FORBIDDEN,             "Debes verificar tu correo antes de iniciar sesión"),
    OTP_RESEND_TOO_SOON     (HttpStatus.TOO_MANY_REQUESTS,     "Espera un momento antes de solicitar otro código"),

    // ─── Conversaciones ───────────────────────────────────────────────────────
    CONV_NOT_FOUND          (HttpStatus.NOT_FOUND,             "Conversación no encontrada"),
    CONV_SELF_CONVERSATION  (HttpStatus.BAD_REQUEST,           "No puedes iniciar una conversación contigo mismo"),
    CONV_GROUP_MIN_MEMBERS  (HttpStatus.BAD_REQUEST,           "Un grupo necesita al menos 2 miembros además de ti"),
    CONV_NOT_GROUP          (HttpStatus.BAD_REQUEST,           "Esta operación solo aplica a conversaciones de grupo"),
    GROUP_NOT_MEMBER        (HttpStatus.FORBIDDEN,             "No eres miembro de este grupo"),
    GROUP_ALREADY_MEMBER    (HttpStatus.CONFLICT,              "Ese usuario ya es miembro del grupo"),
    GROUP_INSUFFICIENT_RANK (HttpStatus.FORBIDDEN,             "No tienes rango suficiente dentro del grupo para esta acción"),
    GROUP_OWNER_MUST_TRANSFER(HttpStatus.BAD_REQUEST,          "Debes transferir la propiedad del grupo antes de salir"),
    GROUP_TARGET_NOT_OWNER  (HttpStatus.BAD_REQUEST,           "Solo puedes transferir la propiedad a otro miembro del grupo"),
    GROUP_CANNOT_SELF_TARGET(HttpStatus.BAD_REQUEST,           "No puedes realizar esta acción sobre ti mismo"),

    // ─── Racha (streak) ───────────────────────────────────────────────────────
    STREAK_NOT_DIRECT        (HttpStatus.BAD_REQUEST,          "La racha solo aplica a conversaciones directas"),
    STREAK_NO_PENDING_REQUEST(HttpStatus.BAD_REQUEST,          "No hay una solicitud de racha pendiente para esta conversación"),
    STREAK_OWN_REQUEST       (HttpStatus.BAD_REQUEST,          "No puedes responder tu propia solicitud de racha"),

    // ─── Mensajes ─────────────────────────────────────────────────────────────
    MSG_TEXT_REQUIRED       (HttpStatus.BAD_REQUEST,           "El contenido de texto es obligatorio para mensajes de tipo TEXT"),
    MSG_PAYLOAD_REQUIRED    (HttpStatus.BAD_REQUEST,           "El payload es obligatorio para mensajes de tipo PAYLOAD"),
    MSG_FILE_REQUIRED       (HttpStatus.BAD_REQUEST,           "Debes adjuntar al menos un archivo"),
    MSG_FILE_LIMIT          (HttpStatus.BAD_REQUEST,           "Se permiten máximo 3 archivos por mensaje"),
    MSG_PAYLOAD_TOO_LARGE   (HttpStatus.BAD_REQUEST,           "El payload es demasiado grande"),
    MSG_CLIENT_ID_CONFLICT  (HttpStatus.CONFLICT,              "Ese clientMessageId ya se usó para otro mensaje"),
    MSG_NOT_FOUND           (HttpStatus.NOT_FOUND,             "Mensaje no encontrado"),
    MSG_NOT_OWNER           (HttpStatus.FORBIDDEN,             "Solo puedes modificar tus propios mensajes"),
    MSG_ALREADY_DELETED     (HttpStatus.BAD_REQUEST,           "Este mensaje ya fue eliminado"),
    MSG_EDIT_NOT_TEXT       (HttpStatus.BAD_REQUEST,           "Solo se pueden editar mensajes de texto"),
    MSG_EDIT_WINDOW_EXPIRED (HttpStatus.BAD_REQUEST,           "Ya pasó el tiempo permitido para editar este mensaje"),

    // ─── Archivos ─────────────────────────────────────────────────────────────
    UPLOAD_FILE_EMPTY       (HttpStatus.BAD_REQUEST,           "El archivo está vacío"),
    UPLOAD_FILE_TOO_LARGE   (HttpStatus.BAD_REQUEST,           "El archivo supera el tamaño máximo permitido"),
    UPLOAD_TOO_MANY_FILES   (HttpStatus.BAD_REQUEST,           "Se permiten máximo 3 archivos por solicitud"),

    // ─── Validación ───────────────────────────────────────────────────────────

    VALID_FIELDS            (HttpStatus.BAD_REQUEST,           "Hay errores en los campos enviados"),

    // ─── Sistema ──────────────────────────────────────────────────────────────

    SYS_INTERNAL_ERROR      (HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");

    // ─── ─────────────────────────────────────────────────────────────────────

    private final HttpStatus httpStatus;
    private final String     message;

    AppCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message    = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public int        getHttpStatusValue() { return httpStatus.value(); }
    public String     getMessage()    { return message; }
    public String     getCode()       { return this.name(); }
}
