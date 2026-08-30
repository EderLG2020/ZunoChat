package com.example.backend.module.messagemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(

        @NotBlank(message = "El nombre del grupo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        /** IDs de los demás miembros (sin contar al creador, que se agrega automáticamente). */
        @NotNull(message = "Selecciona los miembros del grupo")
        @Size(min = 2, message = "Un grupo necesita al menos 2 miembros además de ti")
        List<Long> memberIds

) {}
