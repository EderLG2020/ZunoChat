package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para inicio de sesión.
 * Se acepta username o email como identificador.
 */
public record LoginRequest(

        @NotBlank(message = "El identificador (username o email) es obligatorio")
        String identifier,   // puede ser username o email

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {}
