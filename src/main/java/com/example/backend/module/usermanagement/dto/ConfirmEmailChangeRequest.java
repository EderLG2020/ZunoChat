package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Paso 2: valida el OTP enviado al email nuevo y aplica el cambio. */
public record ConfirmEmailChangeRequest(

        @NotBlank(message = "El código OTP es obligatorio")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código OTP debe ser de 6 dígitos numéricos")
        String otpCode

) {}
