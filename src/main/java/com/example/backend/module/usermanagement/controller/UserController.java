package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.usermanagement.application.BlockService;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.BlockedUserResponse;
import com.example.backend.module.usermanagement.dto.UpdatePhoneRequest;
import com.example.backend.module.usermanagement.dto.UpdateThemeRequest;
import com.example.backend.module.usermanagement.dto.UserProfileResponse;
import com.example.backend.module.usermanagement.dto.UserSearchResponse;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import jakarta.validation.Valid;
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

    // ── Perfil ───────────────────────────────────────────────────────────────

    /** Perfil público de cualquier usuario activo — lo que se ve al tocar un contacto en el chat. */
    @GetMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> profile(@PathVariable Long id) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, UserProfileResponse.from(user)));
    }

    /** Guarda (o borra, si viene vacío) el teléfono del usuario autenticado. */
    @PatchMapping("/me/phone")
    public ResponseEntity<ApiResponse<Void>> updatePhone(@Valid @RequestBody UpdatePhoneRequest request) {
        UserModel user = userRepository.findById(JwtUtil.currentUserId())
                .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));

        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.setPhone(phone);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC));
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

    // ── Preferencias ────────────────────────────────────────────────────────

    /** Persiste la preferencia de tema (claro/oscuro) del usuario autenticado. */
    @PatchMapping("/me/theme")
    public ResponseEntity<ApiResponse<Void>> updateTheme(@Valid @RequestBody UpdateThemeRequest request) {
        UserModel user = userRepository.findById(JwtUtil.currentUserId())
                .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));

        user.setThemePreference(request.theme());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_THEME_UPDATED));
    }
}