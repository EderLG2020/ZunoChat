package com.example.backend.module.usermanagement.dto;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.domain.UserModel;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String dni,
        String username,
        String email,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(UserModel u) {
        return new AdminUserResponse(u.getId(), u.getDni(), u.getUsername(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }
}
