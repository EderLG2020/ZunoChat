package com.example.backend.module.usermanagement.controller;

import com.example.backend.module.usermanagement.application.AuthService;
import com.example.backend.module.usermanagement.dto.AuthResponse;
import com.example.backend.module.usermanagement.dto.LoginRequest;
import com.example.backend.module.usermanagement.dto.RegisterRequest;
import com.example.backend.module.usermanagement.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest req) {
        String message = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Verificar OTP enviado al correo.
     * Si es válido, activa la cuenta y devuelve el JWT.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(authService.verifyOtp(req));
    }

    /**
     * Login con username o email + contraseña.
     * Devuelve JWT con rol y permisos granulares.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
