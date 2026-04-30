package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para verificar el código OTP enviado al correo durante el registro.
 */
public record VerifyOtpRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "El código OTP es obligatorio")
        @Pattern(
            regexp = "^[0-9]{6}$",
            message = "El código OTP debe ser de 6 dígitos numéricos"
        )
        String otpCode

) {}
