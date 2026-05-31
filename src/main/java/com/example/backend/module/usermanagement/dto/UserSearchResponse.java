package com.example.backend.module.usermanagement.dto;

import com.example.backend.module.usermanagement.domain.UserModel;
import lombok.Builder;

@Builder
public record UserSearchResponse(
        Long   id,
        String username,
        String avatar
) {
    public static UserSearchResponse from(UserModel u) {
        return UserSearchResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .avatar(null)
                .build();
    }
}