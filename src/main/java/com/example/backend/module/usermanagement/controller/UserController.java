package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.usermanagement.application.BlockService;
import com.example.backend.module.usermanagement.dto.BlockedUserResponse;
import com.example.backend.module.usermanagement.dto.UserSearchResponse;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final BlockService blockService;

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
                        q.trim().toLowerCase(),
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

    // ── Bloqueo de usuarios ─────────────────────────────────────────────────

    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> listBlocked() {
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, blockService.listBlocked(JwtUtil.currentUserId())));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> block(@PathVariable Long id) {
        blockService.block(JwtUtil.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_BLOCKED));
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> unblock(@PathVariable Long id) {
        blockService.unblock(JwtUtil.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_UNBLOCKED));
    }
}