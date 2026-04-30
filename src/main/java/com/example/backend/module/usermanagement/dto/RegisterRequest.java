package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.*;

/**
 * DTO para el registro de nuevos usuarios.
 * Valida formato de DNI, username, email y contraseña con regex.
 */
public record RegisterRequest(

        /**
         * DNI peruano: 8 dígitos numéricos
         */
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(
            regexp = "^[0-9]{8}$",
            message = "El DNI debe tener exactamente 8 dígitos numéricos"
        )
        String dni,

        /**
         * Username: 4-30 chars, solo letras, números, guión bajo y punto.
         * No puede empezar/terminar con punto o guión bajo.
         */
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 30, message = "El username debe tener entre 4 y 30 caracteres")
        @Pattern(
            regexp = "^(?![._])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$",
            message = "Username inválido: solo letras, números, punto y guión bajo. No puede empezar ni terminar con punto o guión bajo, ni tenerlos consecutivos"
        )
        String username,

        /**
         * Correo electrónico estándar
         */
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String email,

        /**
         * Contraseña segura:
         * - Mínimo 8 caracteres
         * - Al menos 1 mayúscula
         * - Al menos 1 minúscula
         * - Al menos 1 dígito
         * - Al menos 1 caracter especial (@$!%*?&_#^-)
         * - Sin espacios
         */
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^\\-])[A-Za-z\\d@$!%*?&_#^\\-]{8,64}$",
            message = "La contraseña debe tener entre 8 y 64 caracteres, incluir al menos: 1 mayúscula, 1 minúscula, 1 número y 1 caracter especial (@$!%*?&_#^-). Sin espacios."
        )
        String password

) {}
