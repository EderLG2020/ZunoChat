package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Paso 2 del alta con Google: el usuario ya se autenticó con Google
 * (POST /api/auth/google devolvió needsUsername=true) y ahora elige su
 * username para terminar de crear la cuenta.
 */
public record CompleteGoogleRegistrationRequest(

        @NotBlank(message = "El token de registro es obligatorio")
        String registrationToken,

        /** Mismas reglas que RegisterRequest#username */
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 30, message = "El username debe tener entre 4 y 30 caracteres")
        @Pattern(
            regexp = "^(?![._])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$",
            message = "Username inválido: solo letras, números, punto y guión bajo. No puede empezar ni terminar con punto o guión bajo, ni tenerlos consecutivos"
        )
        String username

) {}
