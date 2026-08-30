package com.example.backend.module.usermanagement.dto;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.domain.UserModel;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String dni,
        String username,
        String email,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
    /**
     * El DNI completo (PII) solo se expone a SUPERADMIN — un ADMIN normal ve
     * los últimos 4 dígitos enmascarados (****5678), suficiente para
     * confirmar identidad en el panel sin exponer el documento completo a
     * cualquier cuenta con rol ADMIN.
     */
    public static AdminUserResponse from(UserModel u, Role actorRole) {
        String dni = actorRole == Role.SUPERADMIN ? u.getDni() : maskDni(u.getDni());
        return new AdminUserResponse(u.getId(), dni, u.getUsername(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }

    private static String maskDni(String dni) {
        if (dni == null || dni.length() <= 4) return "****";
        return "*".repeat(dni.length() - 4) + dni.substring(dni.length() - 4);
    }
}
