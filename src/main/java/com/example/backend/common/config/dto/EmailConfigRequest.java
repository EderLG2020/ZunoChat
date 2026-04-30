package com.example.backend.common.config.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la petición para activar/desactivar el servicio de correo.
 */
public record EmailConfigRequest(

        @NotNull(message = "El campo 'enabled' es obligatorio")
        Boolean enabled
) {}