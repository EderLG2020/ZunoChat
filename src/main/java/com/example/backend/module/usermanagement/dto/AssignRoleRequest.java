package com.example.backend.module.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(

        @NotBlank(message = "El rol es obligatorio")
        String role // USER | ADMIN | SUPERADMIN

) {}
