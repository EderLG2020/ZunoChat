package com.example.backend.common.config.dto;

import java.time.LocalDateTime;

/**
 * Respuesta que muestra el estado actual de la configuración de correo.
 */
public record EmailConfigResponse(
        boolean enabled,
        LocalDateTime updatedAt,
        Long updatedBy
) {}