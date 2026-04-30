package com.example.backend.module.usermanagement.application;

import com.example.backend.common.config.domain.AppConfigServiceDomain;
import com.example.backend.common.email.EmailService;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.service.JwtService;
import com.example.backend.common.service.OtpService;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.AuthResponse;
import com.example.backend.module.usermanagement.dto.LoginRequest;
import com.example.backend.module.usermanagement.dto.RegisterRequest;
import com.example.backend.module.usermanagement.dto.VerifyOtpRequest;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio para autenticación:
 * - Registro con envío de OTP
 * - Verificación OTP → activa la cuenta y envía bienvenida
 * - Login → devuelve JWT con rol y permisos
 */
@Service
public class AuthService {

    @Autowired private UserRepository   userRepository;
    @Autowired private JwtService       jwtService;
    @Autowired private OtpService       otpService;
    @Autowired private PasswordEncoder  passwordEncoder;
    @Autowired private EmailService     emailService;
    @Autowired private AppConfigServiceDomain appConfigService;

    // ─── Registro ────────────────────────────────────────────────────────────

    /**
     * Paso 1: Crea el usuario con estado PENDING_VERIFICATION y genera el OTP.
     *
     * Comportamiento según email.enabled y perfil:
     *
     *  DEV  + email.enabled=true  → intenta enviar correo; el OTP también va
     *                               en la respuesta (útil mientras no hay dominio
     *                               verificado en Brevo).
     *  DEV  + email.enabled=false → OTP en la respuesta, sin intento de correo.
     *  PROD + email.enabled=true  → envía correo; la respuesta NO expone el OTP.
     *  PROD + email.enabled=false → sin correo; la respuesta NO expone el OTP.
     */
    public String register(RegisterRequest req) {

        // Validar unicidad
        if (userRepository.existsByDni(req.dni()))
            throw new AppException(AppCode.USER_DNI_EXISTS);

        if (userRepository.existsByUsername(req.username()))
            throw new AppException(AppCode.USER_USERNAME_EXISTS);

        if (userRepository.existsByEmail(req.email()))
            throw new AppException(AppCode.USER_EMAIL_EXISTS);

        // Generar OTP
        String otpCode   = otpService.generateOtp();
        var    otpExpiry = otpService.generateExpiration();

        // Crear usuario
        UserModel user = UserModel.builder()
                .dni(req.dni())
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .status(UserStatus.PENDING_VERIFICATION)
                .otpCode(otpCode)
                .otpExpiration(otpExpiry)
                .build();

        userRepository.save(user);

        boolean emailEnabled = appConfigService.isEmailEnabled();
        boolean isDev        = emailService.isDev();

        if (emailEnabled) {
            // Intenta enviar (el EmailService captura cualquier error sin lanzar)
            emailService.sendOtp(req.email(), req.username(), otpCode);
        }

        if (isDev) {
            // En dev siempre devolvemos el OTP en la respuesta para facilitar pruebas,
            // independientemente de si el correo se envió o no.
            return "Usuario registrado. Código OTP: " + otpCode + " (válido 10 min)";
        }

        // En PROD nunca se expone el OTP en la respuesta
        return "Registro exitoso. Te enviamos un código OTP a " + req.email() + " (válido 10 min).";
    }

    // ─── Verificar OTP ───────────────────────────────────────────────────────

    /**
     * Paso 2: Valida el OTP y activa la cuenta.
     * Tras activar, intenta enviar el correo de bienvenida (error no bloquea).
     */
    public AuthResponse verifyOtp(VerifyOtpRequest req) {

        UserModel user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));

        if (user.isActive())
            throw new AppException(AppCode.USER_ALREADY_ACTIVE);

        if (!otpService.isValid(user.getOtpCode(), req.otpCode(), user.getOtpExpiration()))
            throw new AppException(AppCode.OTP_INVALID);

        // Activar cuenta y limpiar OTP
        user.setStatus(UserStatus.ACTIVE);
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        // Correo de bienvenida — el EmailService absorbe cualquier error
        emailService.sendWelcome(user.getEmail(), user.getUsername(), null);

        // Generar JWT
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getUsername(), user.getEmail(),
                user.getRole().name(), user.getRole().getPermissions());
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    /**
     * Login con username o email + contraseña.
     */
    public AuthResponse login(LoginRequest req) {

        UserModel user = userRepository
                .findByUsernameOrEmail(req.identifier(), req.identifier())
                .orElseThrow(() -> new AppException(AppCode.AUTH_BAD_CREDENTIALS));

        if (user.isBanned())
            throw new AppException(AppCode.USER_BANNED);

        if (user.isPendingVerification())
            throw new AppException(AppCode.OTP_PENDING_REQUIRED);

        if (user.getStatus() == UserStatus.INACTIVE)
            throw new AppException(AppCode.USER_INACTIVE);

        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new AppException(AppCode.AUTH_BAD_CREDENTIALS);

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getUsername(), user.getEmail(),
                user.getRole().name(), user.getRole().getPermissions());
    }
}