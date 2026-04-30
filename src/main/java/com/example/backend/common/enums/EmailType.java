package com.example.backend.common.enums;

/**
 * Catálogo de tipos de correo del sistema.
 *
 * Cada valor define el propósito del correo y determina
 * qué plantilla y qué datos se utilizarán al enviarlo.
 *
 * Tipos con solo texto:
 *   OTP_VERIFICATION, ACCOUNT_STATUS_CHANGED
 *
 * Tipos con texto + imagen:
 *   WELCOME, PASSWORD_RESET_CONFIRM
 */
public enum EmailType {

    /** Bienvenida al registrarse (con imagen de banner) */
    WELCOME,

    /** Envío del código OTP para verificar cuenta */
    OTP_VERIFICATION,

    /** Notificación cuando el estado de la cuenta cambia (ban, activación, etc.) */
    ACCOUNT_STATUS_CHANGED,

    /** Confirmación de que la contraseña fue restablecida */
    PASSWORD_RESET_CONFIRM
}