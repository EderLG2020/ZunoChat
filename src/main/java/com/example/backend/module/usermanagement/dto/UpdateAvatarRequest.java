package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.Size;

/**
 * La URL viene de POST /api/uploads (ver UploadController) — este endpoint
 * solo persiste la URL ya subida, no procesa archivos.
 * Null o vacío para borrar el avatar (volver al placeholder por iniciales).
 */
public record UpdateAvatarRequest(

        @Size(max = 500, message = "La URL del avatar no puede superar los 500 caracteres")
        String avatarUrl

) {}
