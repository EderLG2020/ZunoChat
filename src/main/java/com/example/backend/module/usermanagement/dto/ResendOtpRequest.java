package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email

) {}
