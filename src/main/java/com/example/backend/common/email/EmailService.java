package com.example.backend.common.email;

import com.example.backend.common.config.domain.AppConfigServiceDomain;
import com.example.backend.common.email.template.EmailTemplates;
import com.example.backend.common.enums.EmailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Servicio centralizado de envío de correos electrónicos con Resend
 * (https://resend.com/docs/api-reference/emails/send-email).
 *
 * ─── Comportamiento según perfil y configuración ────────────────────────────
 *
 *  DEV  + email.enabled=true  → envía correo real con Resend
 *  DEV  + email.enabled=false → NO envía; el OTP va en la respuesta API
 *  PROD + email.enabled=true  → envía correo real con Resend
 *  PROD + email.enabled=false → NO envía; NO expone datos en la respuesta API
 *
 * ─── Uso desde otros servicios ──────────────────────────────────────────────
 *
 *   emailService.sendOtp(email, username, otpCode);
 *   emailService.sendWelcome(email, username, bannerUrl);
 *   emailService.sendAccountStatusChanged(email, username, newStatus, reason);
 *   emailService.sendPasswordResetConfirm(email, username);
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    @Autowired
    private AppConfigServiceDomain appConfigService;

    @Autowired
    private Environment environment;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String resendFromEmail;

    @Value("${resend.from.name}")
    private String resendFromName;

    private final RestClient restClient = RestClient.create();

    // ─── Métodos públicos por tipo de correo ─────────────────────────────────
    //
    // Todos son @Async: el caller (AuthService, etc.) no espera la respuesta
    // de Resend para devolver su propia respuesta HTTP. Antes, cada registro o
    // verificación de OTP quedaba bloqueado en el hilo del request hasta que
    // el proveedor de correo contestaba.

    @Async("emailExecutor")
    public void sendOtp(String toEmail, String username, String otpCode) {
        if (!shouldSend()) {
            log.info("[EmailService] email.enabled=false — OTP para {} no enviado. Código: {}",
                    toEmail, isDev() ? otpCode : "***");
            return;
        }
        send(toEmail, username,
                "Tu código de verificación — ZunoChat",
                EmailTemplates.otpVerification(username, otpCode),
                EmailType.OTP_VERIFICATION);
    }

    @Async("emailExecutor")
    public void sendPasswordResetOtp(String toEmail, String username, String otpCode) {
        if (!shouldSend()) {
            log.info("[EmailService] email.enabled=false — OTP de reset para {} no enviado. Código: {}",
                    toEmail, isDev() ? otpCode : "***");
            return;
        }
        send(toEmail, username,
                "Restablece tu contraseña — ZunoChat",
                EmailTemplates.passwordResetOtp(username, otpCode),
                EmailType.OTP_VERIFICATION);
    }

    @Async("emailExecutor")
    public void sendWelcome(String toEmail, String username, String bannerUrl) {
        if (!shouldSend()) {
            log.info("[EmailService] email.enabled=false — Bienvenida para {} no enviada.", toEmail);
            return;
        }
        String banner = (bannerUrl != null && !bannerUrl.isBlank())
                ? bannerUrl : "https://zunochat.com/assets/email-banner.png";
        send(toEmail, username,
                "¡Bienvenido a ZunoChat, %s! 🎉".formatted(username),
                EmailTemplates.welcome(username, banner),
                EmailType.WELCOME);
    }

    @Async("emailExecutor")
    public void sendAccountStatusChanged(String toEmail, String username,
                                         String newStatus, String reason) {
        if (!shouldSend()) {
            log.info("[EmailService] email.enabled=false — Cambio de estado para {} no notificado.", toEmail);
            return;
        }
        String subject = switch (newStatus.toUpperCase()) {
            case "ACTIVE"   -> "Tu cuenta ha sido activada — ZunoChat";
            case "BANNED"   -> "Tu cuenta ha sido suspendida — ZunoChat";
            case "INACTIVE" -> "Tu cuenta está inactiva — ZunoChat";
            default         -> "Actualización en tu cuenta — ZunoChat";
        };
        send(toEmail, username, subject,
                EmailTemplates.accountStatusChanged(username, newStatus, reason),
                EmailType.ACCOUNT_STATUS_CHANGED);
    }

    @Async("emailExecutor")
    public void sendPasswordResetConfirm(String toEmail, String username) {
        if (!shouldSend()) {
            log.info("[EmailService] email.enabled=false — Reset para {} no enviado.", toEmail);
            return;
        }
        send(toEmail, username,
                "Tu contraseña fue restablecida — ZunoChat",
                EmailTemplates.passwordResetConfirm(username),
                EmailType.PASSWORD_RESET_CONFIRM);
    }

    // ─── Núcleo de envío ──────────────────────────────────────────────────────

    private void send(String toEmail, String toName, String subject, String html, EmailType type) {
        try {
            Map<String, Object> body = Map.of(
                    "from", "%s <%s>".formatted(resendFromName, resendFromEmail),
                    "to", List.of(toEmail),
                    "subject", subject,
                    "html", html
            );

            Map<String, Object> result = restClient.post()
                    .uri(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("[EmailService] Correo [{}] enviado a {} — id: {}",
                    type, toEmail, result != null ? result.get("id") : null);

        } catch (RestClientException e) {
            log.warn("[EmailService] Error enviando correo [{}] a {}: {}", type, toEmail, e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean shouldSend() {
        return appConfigService.isEmailEnabled();
    }

    public boolean isDev() {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(profile)) return true;
        }
        return true; // sin perfil explícito se asume dev
    }
}
