package com.example.backend.module.usermanagement.application;

import com.example.backend.common.email.EmailService;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.service.JwtService;
import com.example.backend.common.service.OtpService;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.GroupMemberRepository;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.AuthResponse;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambios de perfil que requieren más que un simple `save()` de UserModel:
 * username y avatar están desnormalizados en ConversationModel (DIRECT) y
 * GroupMemberModel (GROUP) — ver ADR-007 — así que cambiarlos acá también
 * dispara el UPDATE en cascada sobre esas tablas. Password y email exigen
 * reconfirmar la contraseña actual por ser campos de seguridad de la cuenta.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    // ─── Username ────────────────────────────────────────────────────────────

    /**
     * El JWT lleva el username como "sub" (ver JwtService) — JwtFilter y
     * /api/auth/refresh resuelven al usuario haciendo findByUsername(sub).
     * Si cambiáramos el username sin devolver un token nuevo, el JWT que el
     * cliente sigue usando dejaría de resolver a nadie: JwtFilter lo trataría
     * como cuenta DELETED (ver InMemoryUserStatusCache) y /refresh fallaría
     * con USER_NOT_FOUND — el usuario quedaría bloqueado hasta loguearse de
     * nuevo a mano. Por eso este método devuelve un AuthResponse con token
     * nuevo, igual que login/register/verifyOtp.
     */
    @Transactional
    public AuthResponse changeUsername(Long userId, String newUsername) {
        UserModel user = loadUser(userId);

        if (!user.getUsername().equals(newUsername)) {
            if (userRepository.existsByUsername(newUsername))
                throw new AppException(AppCode.USER_USERNAME_EXISTS);

            user.setUsername(newUsername); // syncUsernameLower() corre en @PreUpdate
            userRepository.save(user);

            conversationRepository.updateUser1Username(userId, newUsername);
            conversationRepository.updateUser2Username(userId, newUsername);
            groupMemberRepository.updateUsernameForUser(userId, newUsername);
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getUsername(), user.getEmail(),
                user.getRole().name(), user.getRole().getPermissions(), user.getThemePreference().name());
    }

    // ─── Avatar ──────────────────────────────────────────────────────────────

    @Transactional
    public void changeAvatar(Long userId, String avatarUrl) {
        String normalized = (avatarUrl == null || avatarUrl.isBlank()) ? null : avatarUrl.trim();

        UserModel user = loadUser(userId);
        user.setAvatar(normalized);
        userRepository.save(user);

        conversationRepository.updateUser1Avatar(userId, normalized);
        conversationRepository.updateUser2Avatar(userId, normalized);
        groupMemberRepository.updateAvatarForUser(userId, normalized);
    }

    // ─── Contraseña ──────────────────────────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserModel user = loadUser(userId);

        if (user.getPassword() == null)
            throw new AppException(AppCode.USER_NO_PASSWORD_SET);

        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new AppException(AppCode.USER_CURRENT_PASSWORD_INVALID);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendPasswordResetConfirm(user.getEmail(), user.getUsername());
    }

    // ─── Email (dos pasos: solicitar + confirmar con OTP al correo nuevo) ────

    /**
     * Paso 1: valida la contraseña actual y el correo nuevo, genera un OTP y
     * lo envía al correo NUEVO (prueba de propiedad) — el correo actual no
     * cambia hasta confirmEmailChange().
     */
    @Transactional
    public void requestEmailChange(Long userId, String currentPassword, String newEmail) {
        UserModel user = loadUser(userId);

        if (user.getPassword() == null)
            throw new AppException(AppCode.USER_NO_PASSWORD_SET);

        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new AppException(AppCode.USER_CURRENT_PASSWORD_INVALID);

        if (user.getEmail().equalsIgnoreCase(newEmail))
            throw new AppException(AppCode.USER_EMAIL_SAME);

        if (userRepository.existsByEmail(newEmail))
            throw new AppException(AppCode.USER_EMAIL_EXISTS);

        String otpCode = otpService.generateOtp();
        user.setPendingEmail(newEmail);
        user.setOtpCode(otpCode);
        user.setOtpExpiration(otpService.generateExpiration());
        userRepository.save(user);

        emailService.sendEmailChangeOtp(newEmail, user.getUsername(), otpCode);
    }

    /** Paso 2: valida el OTP (enviado al correo nuevo) y aplica el cambio. */
    @Transactional
    public void confirmEmailChange(Long userId, String otpCode) {
        UserModel user = loadUser(userId);

        if (user.getPendingEmail() == null)
            throw new AppException(AppCode.USER_EMAIL_CHANGE_NOT_REQUESTED);

        if (!otpService.isValid(user.getOtpCode(), otpCode, user.getOtpExpiration()))
            throw new AppException(AppCode.OTP_INVALID);

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UserModel loadUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
    }
}
