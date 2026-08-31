package com.example.backend.common.config.domain;

import com.example.backend.common.config.persistence.AppConfigRepository;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Servicio para leer y modificar la configuración dinámica del sistema.
 *
 * Al arrancar la aplicación garantiza que la fila "email.enabled" exista
 * en BD con el valor definido en el .properties del perfil activo.
 */
@Service
public class AppConfigServiceDomain {

    private static final String EMAIL_ENABLED_KEY = "email.enabled";

    @Autowired
    private AppConfigRepository configRepository;

    /** Valor inicial según el perfil (true en dev, false en prod) */
    @Value("${email.enabled:false}")
    private boolean emailEnabledDefault;

    // ─── Inicialización ──────────────────────────────────────────────────────

    /**
     * Al arrancar, inserta la fila de email.enabled si no existe.
     * Si ya existe en BD, la deja como está (permite que un admin
     * la haya cambiado antes del reinicio).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDefaults() {
        configRepository.findById(EMAIL_ENABLED_KEY).ifPresentOrElse(
                existing -> { /* ya existe, no sobreescribir */ },
                () -> configRepository.save(AppConfigModel.builder()
                        .key(EMAIL_ENABLED_KEY)
                        .value(String.valueOf(emailEnabledDefault))
                        .description("Activa/desactiva el envío de correos mediante Resend")
                        .build())
        );
    }

    // ─── Consultas ───────────────────────────────────────────────────────────

    /**
     * Devuelve true si el envío de correos está habilitado en BD.
     */
    public boolean isEmailEnabled() {
        return configRepository.findById(EMAIL_ENABLED_KEY)
                .map(cfg -> "true".equalsIgnoreCase(cfg.getValue()))
                .orElse(emailEnabledDefault);
    }

    // ─── Mutaciones ──────────────────────────────────────────────────────────

    /**
     * Activa o desactiva el servicio de correo en tiempo de ejecución.
     *
     * @param enabled   nuevo valor
     * @param adminId   id del admin que realiza el cambio (trazabilidad)
     */
    public void setEmailEnabled(boolean enabled, Long adminId) {
        AppConfigModel cfg = configRepository.findById(EMAIL_ENABLED_KEY)
                .orElseThrow(() -> new AppException(AppCode.SYS_INTERNAL_ERROR,
                        "Fila de configuración 'email.enabled' no encontrada"));

        cfg.setValue(String.valueOf(enabled));
        cfg.setUpdatedBy(adminId);
        configRepository.save(cfg);
    }

    /**
     * Devuelve la entidad completa para mostrarla en el endpoint de consulta.
     */
    public AppConfigModel getEmailConfig() {
        return configRepository.findById(EMAIL_ENABLED_KEY)
                .orElseThrow(() -> new AppException(AppCode.SYS_INTERNAL_ERROR,
                        "Configuración de email no encontrada"));
    }
}