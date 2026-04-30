package com.example.backend.common.enums;

import java.util.List;

/**
 * Roles del sistema con sus permisos granulares.
 * Los permisos se incluyen directamente en el JWT como claim "permissions".
*/
public enum Role {

    USER(List.of(
            "profile:ver",
            "profile:editar",
            "chat:enviar",
            "chat:ver"
    )),

    ADMIN(List.of(
            "profile:ver",
            "profile:editar",
            "chat:enviar",
            "chat:ver",
            "dashboard:ver",
            "dashboard:editar",
            "usuarios:ver",
            "usuarios:bannear",
            "usuarios:activar"
    )),

    SUPERADMIN(List.of(
            "profile:ver",
            "profile:editar",
            "chat:enviar",
            "chat:ver",
            "dashboard:ver",
            "dashboard:editar",
            "usuarios:ver",
            "usuarios:bannear",
            "usuarios:activar",
            "usuarios:eliminar",
            "roles:asignar",
            "sistema:configurar",
            "superadmin:panel"
    ));

    private final List<String> permissions;

    Role(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
