package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Authorization code obtenido en el frontend con
 * google.accounts.oauth2.initCodeClient (ux_mode: 'popup').
 */
public record GoogleAuthRequest(

        @NotBlank(message = "El código de autorización es obligatorio")
        String code

) {}
