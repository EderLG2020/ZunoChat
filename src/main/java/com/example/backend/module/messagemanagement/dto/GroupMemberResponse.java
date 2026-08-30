package com.example.backend.module.messagemanagement.dto;

import com.example.backend.module.messagemanagement.domain.GroupMemberModel;

public record GroupMemberResponse(
        Long userId,
        String username,
        String avatar
) {
    public static GroupMemberResponse from(GroupMemberModel m) {
        return new GroupMemberResponse(m.getUserId(), m.getUsername(), m.getAvatar());
    }
}
