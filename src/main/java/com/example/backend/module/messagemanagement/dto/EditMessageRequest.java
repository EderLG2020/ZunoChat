package com.example.backend.module.messagemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(

        @NotBlank(message = "El contenido de texto es obligatorio")
        @Size(max = 4000, message = "El mensaje no puede superar los 4000 caracteres")
        String textContent

) {}
