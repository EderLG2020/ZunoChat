package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "El código OTP es obligatorio")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código OTP debe ser de 6 dígitos numéricos")
        String otpCode,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^\\-])[A-Za-z\\d@$!%*?&_#^\\-]{8,64}$",
            message = "La contraseña debe tener entre 8 y 64 caracteres, incluir al menos: 1 mayúscula, 1 minúscula, 1 número y 1 caracter especial (@$!%*?&_#^-). Sin espacios."
        )
        String newPassword

) {}
