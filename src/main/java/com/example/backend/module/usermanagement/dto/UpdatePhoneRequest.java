package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.Size;

public record UpdatePhoneRequest(

        /** Null o vacío para borrar el teléfono guardado. */
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone

) {}
