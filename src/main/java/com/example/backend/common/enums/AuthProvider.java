package com.example.backend.common.enums;

/**
 * Cómo se autentica el usuario.
 */
public enum AuthProvider {

    /** Usuario/email + contraseña, con verificación por OTP */
    LOCAL,

    /** Autenticado vía Google OAuth (authorization code flow) */
    GOOGLE
}
