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

    // ─── Auth ─────────────────────────────────────────────────────────────────

    AUTH_BAD_CREDENTIALS    (HttpStatus.UNAUTHORIZED,          "Credenciales incorrectas"),
    AUTH_TOKEN_EXPIRED      (HttpStatus.UNAUTHORIZED,          "El token JWT ha expirado"),
    AUTH_TOKEN_INVALID      (HttpStatus.UNAUTHORIZED,          "El token JWT es inválido o está malformado"),
    AUTH_TOKEN_MISSING      (HttpStatus.UNAUTHORIZED,          "Se requiere autenticación"),
    AUTH_FORBIDDEN          (HttpStatus.FORBIDDEN,             "No tienes permisos para realizar esta acción"),

    // ─── Usuario ──────────────────────────────────────────────────────────────

    USER_NOT_FOUND          (HttpStatus.NOT_FOUND,             "Usuario no encontrado"),
    USER_BANNED             (HttpStatus.FORBIDDEN,             "Tu cuenta ha sido suspendida. Contacta al soporte"),
    USER_INACTIVE           (HttpStatus.FORBIDDEN,             "Tu cuenta está inactiva"),
    USER_ALREADY_ACTIVE     (HttpStatus.BAD_REQUEST,           "La cuenta ya está verificada y activa"),
    USER_DNI_EXISTS         (HttpStatus.CONFLICT,              "El DNI ya está registrado"),
    USER_USERNAME_EXISTS    (HttpStatus.CONFLICT,              "El nombre de usuario ya está en uso"),
    USER_EMAIL_EXISTS       (HttpStatus.CONFLICT,              "El correo electrónico ya está registrado"),

    // ─── OTP ──────────────────────────────────────────────────────────────────

    OTP_INVALID             (HttpStatus.BAD_REQUEST,           "El código OTP es incorrecto"),
    OTP_EXPIRED             (HttpStatus.BAD_REQUEST,           "El código OTP ha expirado. Solicita uno nuevo"),
    OTP_PENDING_REQUIRED    (HttpStatus.FORBIDDEN,             "Debes verificar tu correo antes de iniciar sesión"),

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
