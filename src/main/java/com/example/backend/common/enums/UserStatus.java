package com.example.backend.common.enums;

/**
 * Estados posibles de un usuario en el sistema.
 */
public enum UserStatus {

    /** Registro iniciado, pendiente de verificar OTP */
    PENDING_VERIFICATION,

    /** Usuario activo y verificado */
    ACTIVE,

    /** Baneado por un admin/superadmin */
    BANNED,

    /** Desactivado voluntariamente o por inactividad */
    INACTIVE,

    /** Eliminado lógicamente (soft delete) */
    DELETED
}
