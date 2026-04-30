package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.usermanagement.application.AuthService;
import com.example.backend.module.usermanagement.dto.AuthResponse;
import com.example.backend.module.usermanagement.dto.LoginRequest;
import com.example.backend.module.usermanagement.dto.RegisterRequest;
import com.example.backend.module.usermanagement.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos de autenticación.
 *
 * POST /auth/register     → registra usuario (devuelve OTP en dev)
 * POST /auth/verify-otp   → verifica OTP y activa la cuenta → devuelve JWT
 * POST /auth/login        → login → devuelve JWT
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

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
}