package com.example.backend.module.usermanagement.dto;

import com.example.backend.module.usermanagement.domain.UserModel;
import lombok.Builder;

/** Perfil público de un usuario — lo que ve cualquier otro usuario al abrirlo desde un chat. */
@Builder
public record UserProfileResponse(
        Long   id,
        String username,
        String email,
        String phone,
        String avatar
) {
    public static UserProfileResponse from(UserModel u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatar(u.getAvatar())
                .build();
    }
}
