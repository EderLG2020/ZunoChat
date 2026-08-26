package com.example.backend.common.email;

import brevo.ApiClient;
import brevo.ApiException;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.CreateSmtpEmail;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import com.example.backend.common.config.domain.AppConfigServiceDomain;
import com.example.backend.common.email.template.EmailTemplates;
import com.example.backend.common.enums.EmailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio centralizado de envío de correos electrónicos con Brevo.
 *
 * ─── Comportamiento según perfil y configuración ────────────────────────────
 *
 *  DEV  + email.enabled=true  → envía correo real con Brevo
 *  DEV  + email.enabled=false → NO envía; el OTP va en la respuesta API
 *  PROD + email.enabled=true  → envía correo real con Brevo
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

    @Autowired
    private AppConfigServiceDomain appConfigService;

    @Autowired
    private Environment environment;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.from.email}")
    private String brevoFromEmail;

    @Value("${brevo.from.name}")
    private String brevoFromName;

    // ─── Métodos públicos por tipo de correo ─────────────────────────────────
    //
    // Todos son @Async: el caller (AuthService, etc.) no espera la respuesta
    // de Brevo para devolver su propia respuesta HTTP. Antes, cada registro o
    // verificación de OTP quedaba bloqueado en el hilo del request hasta que
    // Brevo contestaba.

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
            ApiClient client = Configuration.getDefaultApiClient();
            ApiKeyAuth apiKey = (ApiKeyAuth) client.getAuthentication("api-key");
            apiKey.setApiKey(brevoApiKey);

            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(brevoFromEmail);
            sender.setName(brevoFromName);

            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            recipient.setName(toName);

            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(sender);
            email.setTo(List.of(recipient));
            email.setSubject(subject);
            email.setHtmlContent(html);

            TransactionalEmailsApi api = new TransactionalEmailsApi();
            CreateSmtpEmail result = api.sendTransacEmail(email);

            log.info("[EmailService] Correo [{}] enviado a {} — messageId: {}",
                    type, toEmail, result.getMessageId());

        } catch (ApiException e) {
            log.warn("[EmailService] Error enviando correo [{}] a {}: {} — body: {}",
                    type, toEmail, e.getMessage(), e.getResponseBody());
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