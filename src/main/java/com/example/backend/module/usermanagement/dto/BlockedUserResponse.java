package com.example.backend.module.usermanagement.dto;

import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long id,
        String username,
        LocalDateTime blockedAt
) {}
