package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cambio de contraseña autenticado (a diferencia de ResetPasswordRequest, que no requiere sesión y valida un OTP). */
public record UpdatePasswordRequest(

        @NotBlank(message = "Debes ingresar tu contraseña actual")
        String currentPassword,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^\\-])[A-Za-z\\d@$!%*?&_#^\\-]{8,64}$",
            message = "La contraseña debe tener entre 8 y 64 caracteres, incluir al menos: 1 mayúscula, 1 minúscula, 1 número y 1 caracter especial (@$!%*?&_#^-). Sin espacios."
        )
        String newPassword

) {}
