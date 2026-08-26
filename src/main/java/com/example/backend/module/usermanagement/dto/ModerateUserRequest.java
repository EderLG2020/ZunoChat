package com.example.backend.module.usermanagement.dto;

/** Body opcional para ban/activar — reason solo se usa para el correo de notificación. */
public record ModerateUserRequest(String reason) {}
