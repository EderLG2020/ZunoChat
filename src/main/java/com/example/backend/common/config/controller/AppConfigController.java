package com.example.backend.common.config.controller;

import com.example.backend.common.config.domain.AppConfigModel;
import com.example.backend.common.config.domain.AppConfigServiceDomain;
import com.example.backend.common.config.dto.EmailConfigRequest;
import com.example.backend.common.config.dto.EmailConfigResponse;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de configuración del sistema (solo SUPERADMIN).
 *
 * GET  /config/email        → consulta estado actual del servicio de correo
 * PUT  /config/email        → activa o desactiva el servicio de correo
 */
@RestController
@RequestMapping("/config")
public class AppConfigController {

    @Autowired
    private AppConfigServiceDomain appConfigService;

    /**
     * Consulta si el envío de correos está habilitado.
     */
    @GetMapping("/email")
    @PreAuthorize("hasAuthority('config:ver')")
    public ResponseEntity<ApiResponse<EmailConfigResponse>> getEmailConfig() {

        AppConfigModel cfg = appConfigService.getEmailConfig();
        EmailConfigResponse response = new EmailConfigResponse(
                "true".equalsIgnoreCase(cfg.getValue()),
                cfg.getUpdatedAt(),
                cfg.getUpdatedBy()
        );

        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC, "Configuración de correo obtenida", response));
    }

    /**
     * Activa o desactiva el servicio de correo en tiempo de ejecución.
     * Solo SUPERADMIN puede ejecutar esta acción.
     *
     * Body: { "enabled": true | falsé }
     */
    @PutMapping("/email")
    @PreAuthorize("hasAuthority('config:editar')")
    public ResponseEntity<ApiResponse<EmailConfigResponse>> setEmailEnabled(
            @Valid @RequestBody EmailConfigRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extraemos eld del admin autenticado desde el principal si está disponible
        Long adminId = null;
        if (userDetails instanceof com.example.backend.module.usermanagement.domain.UserModel u) {
            adminId = u.getId();
        }

        appConfigService.setEmailEnabled(req.enabled(), adminId);

        AppConfigModel cfg = appConfigService.getEmailConfig();
        EmailConfigResponse response = new EmailConfigResponse(
                "true".equalsIgnoreCase(cfg.getValue()),
                cfg.getUpdatedAt(),
                cfg.getUpdatedBy()
        );

        String msg = req.enabled()
                ? "Servicio de correo activado correctamente"
                : "Servicio de correo desactivado correctamente";

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, msg, response));
    }
}