package com.example.backend.module.usermanagement.application;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.service.JwtService;
import com.example.backend.common.service.OtpService;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.AuthResponse;
import com.example.backend.module.usermanagement.dto.LoginRequest;
import com.example.backend.module.usermanagement.dto.RegisterRequest;
import com.example.backend.module.usermanagement.dto.VerifyOtpRequest;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de negocio para autenticación:
 * - Registro con envío de OTP
 * - Verificación OTP → activa la cuenta
 * - Login → devuelve JWT con rol y permisos
 */
@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private OtpService otpService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ─── Registro ────────────────────────────────────────────────────────────

    /**
     * Paso 1: Crear el usuario con estado PENDING_VERIFICATION y generar OTP.
     *
     * El OTP se devuelve en la respuesta para que el frontend lo muestre/envíe.
     * En producción aquí se llamaría al servicio de email para enviarlo.
     */
    public String register(RegisterRequest req) {

        // Validar unicidad
        if (userRepository.existsByDni(req.dni()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");

        if (userRepository.existsByUsername(req.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");

        if (userRepository.existsByEmail(req.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");

        // Generar OTP
        String otpCode   = otpService.generateOtp();
        var    otpExpiry = otpService.generateExpiration();

        // Crear usuario
        UserModel user = UserModel.builder()
                .dni(req.dni())
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))   // ← BCrypt
                .status(UserStatus.PENDING_VERIFICATION)
                .otpCode(otpCode)
                .otpExpiration(otpExpiry)
                .build();

        userRepository.save(user);

        // TODO: enviar otpCode al correo req.email() con JavaMailSender / SendGrid
        // emailService.sendOtp(req.email(), otpCode);

        // En dev se devuelve el OTP directamente (en prod NO hacer esto)
        return "Usuario registrado. Código OTP: " + otpCode + " (válido 10 min)";
    }

    // ─── Verificar OTP ───────────────────────────────────────────────────────

    /**
     * Paso 2: Validar el OTP e activar la cuenta.
     */
    public AuthResponse verifyOtp(VerifyOtpRequest req) {

        UserModel user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (user.isActive())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta ya está verificada");

        if (!otpService.isValid(user.getOtpCode(), req.otpCode(), user.getOtpExpiration()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código OTP inválido o expirado");

        // Activar cuenta y limpiar OTP
        user.setStatus(UserStatus.ACTIVE);
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        // Verificar estado
        if (user.isBanned())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta ha sido suspendida");

        if (user.isPendingVerification())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes verificar tu correo antes de iniciar sesión");

        if (user.getStatus() == com.example.backend.common.enums.UserStatus.INACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva");

        // Verificar contraseña
        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getUsername(), user.getEmail(),
                user.getRole().name(), user.getRole().getPermissions());
    }
}
