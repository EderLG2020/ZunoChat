package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.usermanagement.dto.UserSearchResponse;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> search(
            @RequestParam String q,
            @AuthenticationPrincipal String username) {

        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(
                    ApiResponse.ok(AppCode.OK_GENERIC, "OK", List.of())
            );
        }

        var me = userRepository.findByUsername(username)
                .orElseThrow();

        List<UserSearchResponse> results = userRepository
                .searchByUsername(
                        q.trim(),
                        me.getId(),
                        UserStatus.ACTIVE,
                        PageRequest.of(0, 10)
                )
                .stream()
                .map(UserSearchResponse::from)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC, "OK", results)
        );
    }
}