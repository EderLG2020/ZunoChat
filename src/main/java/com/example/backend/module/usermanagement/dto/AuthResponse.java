package com.example.backend.module.usermanagement.dto;

import java.util.List;

/**
 * Respuesta devuelta tras un login o registro exitoso.
 * Incluye el token JWT y los datos públicos del usuario.
 */
public record AuthResponse(

        String token,
        String tokenType,        // "Bearer"
        String username,
        String email,
        String role,
        List<String> permissions, // ej: ["dashboard:editar", "usuarios:ver"]
        String themePreference    // "LIGHT" | "DARK"

) {
    public static AuthResponse of(String token, String username, String email,
                                  String role, List<String> permissions, String themePreference) {
        return new AuthResponse(token, "Bearer", username, email, role, permissions, themePreference);
    }
}
