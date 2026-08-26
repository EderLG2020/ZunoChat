package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.security.ratelimit.IRateLimiter;
import com.example.backend.module.usermanagement.application.AuthService;
import com.example.backend.module.usermanagement.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * POST /api/auth/register     → registra usuario
 * POST /api/auth/verify-otp   → verifica OTP → devuelve JWT
 * POST /api/auth/login        → login → devuelve JWT
 *
 * La búsqueda de usuarios vive en UserController (GET /api/users/search).
 *
 * Los endpoints sensibles a fuerza bruta (login, verificación/reenvío de
 * OTP, recuperación de contraseña) están protegidos con IRateLimiter — por
 * IP + identificador de cuenta, ventana fija. No parsea X-Forwarded-For:
 * detrás de un reverse proxy que no preserve la IP real, este límite pasa a
 * aplicarse por proxy en vez de por cliente — sigue frenando fuerza bruta
 * masiva, pero es una limitación conocida a resolver si se despliega así.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private IRateLimiter rateLimiter;

    private void enforceRateLimit(HttpServletRequest request, String scope, String identifier,
                                  int maxAttempts, Duration window) {
        String key = scope + ":" + request.getRemoteAddr() + ":" + identifier;
        if (!rateLimiter.tryConsume(key, maxAttempts, window)) {
            throw new AppException(AppCode.AUTH_RATE_LIMITED);
        }
    }

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
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req,
                                                                HttpServletRequest request) {
        // 5 intentos / 10 min por cuenta — el OTP son 6 dígitos (1M combinaciones)
        // y expira a los 10 min; sin este límite, un ataque automatizado podía
        // agotar el espacio de búsqueda dentro de la propia ventana de validez.
        enforceRateLimit(request, "otp-verify", req.email(), 5, Duration.ofMinutes(10));
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
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req,
                                                            HttpServletRequest request) {
        enforceRateLimit(request, "login", req.identifier(), 10, Duration.ofMinutes(5));
        AuthResponse auth = authService.login(req);
        return ResponseEntity
                .status(AppCode.OK_LOGIN.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_LOGIN, auth));
    }

    /**
     * Reenvía el OTP a una cuenta PENDING_VERIFICATION que no llegó a
     * verificarse a tiempo (o nunca recibió el correo).
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest req,
                                                        HttpServletRequest request) {
        enforceRateLimit(request, "otp-resend", req.email(), 5, Duration.ofMinutes(10));
        authService.resendOtp(req);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_OTP_RESENT));
    }

    /**
     * Paso 1 de recuperar contraseña: envía un OTP si el correo corresponde
     * a una cuenta activa. Respuesta idéntica exista o no la cuenta.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                                             HttpServletRequest request) {
        enforceRateLimit(request, "forgot-password", req.email(), 5, Duration.ofMinutes(10));
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_PASSWORD_RESET_SENT));
    }

    /** Paso 2 de recuperar contraseña: valida el OTP y cambia la contraseña. */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req,
                                                            HttpServletRequest request) {
        // Mismo límite que verify-otp: reset-password también valida un OTP
        // de 6 dígitos y es igual de vulnerable a fuerza bruta si no se acota.
        enforceRateLimit(request, "reset-password", req.email(), 5, Duration.ofMinutes(10));
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_PASSWORD_RESET));
    }

    /**
     * Renueva un JWT antes (o poco después) de que expire, sin obligar a
     * loguearse de nuevo. Lee el token del header manualmente porque
     * JwtFilter deja pasar esta ruta sin procesarlo (ver JwtFilter).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(AppCode.AUTH_TOKEN_MISSING.getHttpStatus())
                    .body(ApiResponse.error(AppCode.AUTH_TOKEN_MISSING));

        AuthResponse auth = authService.refresh(authHeader.substring(7));
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_TOKEN_REFRESHED, auth));
    }
}