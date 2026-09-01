package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Paso 1 de cambiar el email: confirma identidad con la contraseña actual y dispara un OTP al email nuevo. */
public record RequestEmailChangeRequest(

        @NotBlank(message = "Debes ingresar tu contraseña actual")
        String currentPassword,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String newEmail

) {}
