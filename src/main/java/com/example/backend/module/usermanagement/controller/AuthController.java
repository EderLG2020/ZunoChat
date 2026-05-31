package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.usermanagement.application.AuthService;
import com.example.backend.module.usermanagement.dto.*;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * POST /api/auth/register     → registra usuario
 * POST /api/auth/verify-otp   → verifica OTP → devuelve JWT
 * POST /api/auth/login        → login → devuelve JWT
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthService authService;
    private final UserRepository userRepository;

    /**
     * Registro de nuevo usuario (solo rol USER).
     * Campos requeridos: dni, username, email, password
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest req) {
        String otp = authService.register(req);
        return ResponseEntity
                .status(AppCode.OK_REGISTER.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_REGISTER, otp));
    }

    /**
     * Verificar OTP enviado al correo.
     * Si es válido, activa la cuenta y devuelve el JWT.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        AuthResponse auth = authService.verifyOtp(req);
        return ResponseEntity
                .status(AppCode.OK_OTP_VERIFIED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_OTP_VERIFIED, auth));
    }

    /**
     * Login con username o email + contraseña.
     * Devuelve JWT con rol y permisos granulares.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse auth = authService.login(req);
        return ResponseEntity
                .status(AppCode.OK_LOGIN.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_LOGIN, auth));
    }

    // ── Búsqueda de usuarios ──────────────────────────────────────────────────
    @GetMapping("/api/users/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> search(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails principal) {

        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, "OK", List.of()));
        }

        var me = userRepository.findByUsername(principal.getUsername()).orElseThrow();

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

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, "OK", results));
    }
}