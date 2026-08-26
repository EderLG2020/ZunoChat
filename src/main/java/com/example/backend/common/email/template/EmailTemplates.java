package com.example.backend.common.email.template;

/**
 * Plantillas HTML para cada tipo de correo.
 *
 * Diseño: fondo oscuro (#0f0f0f), tarjeta centrada, tipografía Inter,
 * branding ZunoChat con acento verde-azulado (#00c9a7).
 *
 * Cada método devuelve el HTML completo listo para enviarse.
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    // ─── Estilos base compartidos ─────────────────────────────────────────────

    private static String baseWrapper(String content) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>ZunoChat</title>
            </head>
            <body style="margin:0;padding:0;background:#0f0f0f;font-family:'Inter',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0f0f0f;padding:40px 0;">
                <tr><td align="center">
                  <table width="580" cellpadding="0" cellspacing="0"
                         style="background:#1a1a1a;border-radius:16px;overflow:hidden;
                                border:1px solid #2a2a2a;max-width:580px;width:100%%;">

                    <!-- HEADER -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#00c9a7 0%%,#0077ff 100%%);
                                 padding:32px 40px;text-align:center;">
                        <span style="font-size:28px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">
                          Zuno<span style="opacity:0.75;">Chat</span>
                        </span>
                      </td>
                    </tr>

                    <!-- BODY -->
                    <tr><td style="padding:40px;">
                      %s
                    </td></tr>

                    <!-- FOOTER -->
                    <tr>
                      <td style="padding:24px 40px;border-top:1px solid #2a2a2a;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#555;">
                          © 2026 ZunoChat · Todos los derechos reservados<br/>
                          <span style="color:#444;">Este correo fue enviado automáticamente, no respondas a este mensaje.</span>
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(content);
    }

    private static String primaryButton(String label, String url) {
        return """
            <a href="%s"
               style="display:inline-block;margin-top:24px;padding:14px 36px;
                      background:linear-gradient(135deg,#00c9a7,#0077ff);
                      color:#fff;text-decoration:none;border-radius:8px;
                      font-weight:700;font-size:15px;letter-spacing:0.3px;">
              %s
            </a>
            """.formatted(url, label);
    }

    private static String heading(String text) {
        return "<h2 style=\"margin:0 0 12px;font-size:22px;font-weight:700;color:#f0f0f0;\">%s</h2>"
                .formatted(text);
    }

    private static String paragraph(String text) {
        return "<p style=\"margin:0 0 16px;font-size:15px;line-height:1.65;color:#aaa;\">%s</p>"
                .formatted(text);
    }

    private static String divider() {
        return "<hr style=\"border:none;border-top:1px solid #2a2a2a;margin:24px 0;\"/>";
    }

    // ─── OTP_VERIFICATION ────────────────────────────────────────────────────

    /**
     * Correo con el código OTP de 6 dígitos.
     *
     * @param username  nombre del usuario
     * @param otpCode   código OTP a mostrar
     */
    public static String otpVerification(String username, String otpCode) {
        String body = heading("Verifica tu cuenta") +
                paragraph("Hola <strong style=\"color:#f0f0f0;\">%s</strong>, gracias por registrarte en ZunoChat."
                        .formatted(username)) +
                paragraph("Usa el siguiente código para activar tu cuenta. Válido por <strong style=\"color:#f0f0f0;\">10 minutos</strong>.") +
                // OTP block
                """
                <div style="margin:24px 0;padding:20px;background:#0f0f0f;border-radius:12px;
                            border:1px dashed #00c9a7;text-align:center;">
                  <span style="font-size:40px;font-weight:800;letter-spacing:12px;color:#00c9a7;">%s</span>
                </div>
                """.formatted(otpCode) +
                divider() +
                paragraph("Si no creaste una cuenta en ZunoChat, puedes ignorar este correo.");

        return baseWrapper(body);
    }

    // ─── PASSWORD_RESET_OTP ──────────────────────────────────────────────────

    /**
     * Correo con el código OTP de 6 dígitos para restablecer la contraseña
     * (distinto de otpVerification: ese es para activar la cuenta al registrarse).
     */
    public static String passwordResetOtp(String username, String otpCode) {
        String body = heading("Restablece tu contraseña") +
                paragraph("Hola <strong style=\"color:#f0f0f0;\">%s</strong>, recibimos una solicitud para restablecer la contraseña de tu cuenta."
                        .formatted(username)) +
                paragraph("Usa el siguiente código para continuar. Válido por <strong style=\"color:#f0f0f0;\">10 minutos</strong>.") +
                """
                <div style="margin:24px 0;padding:20px;background:#0f0f0f;border-radius:12px;
                            border:1px dashed #00c9a7;text-align:center;">
                  <span style="font-size:40px;font-weight:800;letter-spacing:12px;color:#00c9a7;">%s</span>
                </div>
                """.formatted(otpCode) +
                divider() +
                paragraph("Si no solicitaste este cambio, ignora este correo — tu contraseña actual sigue siendo válida.");

        return baseWrapper(body);
    }

    // ─── WELCOME ─────────────────────────────────────────────────────────────

    /**
     * Correo de bienvenida tras verificar la cuenta (con imagen de banner).
     *
     * @param username  nombre del usuario
     * @param bannerUrl URL pública de la imagen de bienvenida
     */
    public static String welcome(String username, String bannerUrl) {
        String body = // Banner image
                """
                <div style="margin:-40px -40px 32px;overflow:hidden;border-radius:0;">
                  <img src="%s" alt="Bienvenido a ZunoChat"
                       style="width:100%%;max-height:200px;object-fit:cover;display:block;"/>
                </div>
                """.formatted(bannerUrl) +
                        heading("¡Bienvenido a ZunoChat! 🎉") +
                        paragraph("Hola <strong style=\"color:#f0f0f0;\">%s</strong>, tu cuenta ya está activa y lista para usar."
                                .formatted(username)) +
                        paragraph("ZunoChat es tu espacio de comunicación en tiempo real. Conecta con tu equipo, crea salas y colabora sin límites.") +

                        // Feature pills
                        """
                        <table cellpadding="0" cellspacing="0" width="100%%" style="margin:24px 0;">
                          <tr>
                            <td width="33%%" style="padding:4px;">
                              <div style="background:#0f0f0f;border-radius:8px;padding:16px;text-align:center;border:1px solid #2a2a2a;">
                                <div style="font-size:24px;">💬</div>
                                <div style="font-size:12px;color:#aaa;margin-top:6px;">Mensajería en tiempo real</div>
                              </div>
                            </td>
                            <td width="33%%" style="padding:4px;">
                              <div style="background:#0f0f0f;border-radius:8px;padding:16px;text-align:center;border:1px solid #2a2a2a;">
                                <div style="font-size:24px;">🔒</div>
                                <div style="font-size:12px;color:#aaa;margin-top:6px;">Cifrado de extremo a extremo</div>
                              </div>
                            </td>
                            <td width="33%%" style="padding:4px;">
                              <div style="background:#0f0f0f;border-radius:8px;padding:16px;text-align:center;border:1px solid #2a2a2a;">
                                <div style="font-size:24px;">⚡</div>
                                <div style="font-size:12px;color:#aaa;margin-top:6px;">Ultra rápido y confiable</div>
                              </div>
                            </td>
                          </tr>
                        </table>
                        """ +
                        primaryButton("Ir a ZunoChat", "https://zunochat.com/app") +
                        divider() +
                        paragraph("¿Tienes dudas? Escríbenos a <a href=\"mailto:soporte@zunochat.com\" style=\"color:#00c9a7;\">soporte@zunochat.com</a>");

        return baseWrapper(body);
    }

    // ─── ACCOUNT_STATUS_CHANGED ──────────────────────────────────────────────

    /**
     * Notificación de cambio de estado de la cuenta.
     *
     * @param username   nombre del usuario
     * @param newStatus  nuevo estado (ACTIVE, BANNED, INACTIVE…)
     * @param reason     motivo del cambio (puede ser null)
     */
    public static String accountStatusChanged(String username, String newStatus, String reason) {

        String emoji     = switch (newStatus.toUpperCase()) {
            case "ACTIVE"   -> "✅";
            case "BANNED"   -> "🚫";
            case "INACTIVE" -> "⏸️";
            default         -> "ℹ️";
        };

        String statusLabel = switch (newStatus.toUpperCase()) {
            case "ACTIVE"   -> "<span style=\"color:#00c9a7;font-weight:700;\">Activa</span>";
            case "BANNED"   -> "<span style=\"color:#ff4d4d;font-weight:700;\">Suspendida</span>";
            case "INACTIVE" -> "<span style=\"color:#f0a500;font-weight:700;\">Inactiva</span>";
            default         -> "<span style=\"color:#aaa;font-weight:700;\">%s</span>".formatted(newStatus);
        };

        String reasonBlock = (reason != null && !reason.isBlank())
                ? """
              <div style="margin:16px 0;padding:16px;background:#0f0f0f;border-left:4px solid #f0a500;border-radius:4px;">
                <p style="margin:0;font-size:14px;color:#aaa;">
                  <strong style="color:#f0f0f0;">Motivo:</strong> %s
                </p>
              </div>
              """.formatted(reason)
                : "";

        String body = heading("Cambio de estado en tu cuenta %s".formatted(emoji)) +
                paragraph("Hola <strong style=\"color:#f0f0f0;\">%s</strong>, queremos informarte que el estado de tu cuenta ha cambiado."
                        .formatted(username)) +
                "<p style=\"margin:0 0 16px;font-size:15px;color:#aaa;\">Estado actual: %s</p>".formatted(statusLabel) +
                reasonBlock +
                paragraph("Si crees que esto es un error o necesitas más información, contáctanos.") +
                primaryButton("Contactar soporte", "mailto:soporte@zunochat.com") +
                divider() +
                paragraph("Equipo de ZunoChat");

        return baseWrapper(body);
    }

    // ─── PASSWORD_RESET_CONFIRM ──────────────────────────────────────────────

    /**
     * Confirmación de que la contraseña fue restablecida exitosamente.
     *
     * @param username  nombre del usuario
     */
    public static String passwordResetConfirm(String username) {
        String body = heading("Tu contraseña fue restablecida 🔑") +
                paragraph("Hola <strong style=\"color:#f0f0f0;\">%s</strong>, confirmamos que la contraseña de tu cuenta fue cambiada exitosamente."
                        .formatted(username)) +
                """
                <div style="margin:24px 0;padding:20px;background:#0f0f0f;border-radius:12px;
                            border:1px solid #2a2a2a;display:flex;align-items:center;">
                  <span style="font-size:32px;margin-right:16px;">🛡️</span>
                  <p style="margin:0;font-size:14px;color:#aaa;line-height:1.6;">
                    Si <strong style="color:#f0f0f0;">no realizaste este cambio</strong>, bloquea tu cuenta
                    de inmediato y contáctanos para proteger tu información.
                  </p>
                </div>
                """ +
                primaryButton("Contactar soporte urgente", "mailto:soporte@zunochat.com") +
                divider() +
                paragraph("Si fuiste tú quien realizó este cambio, puedes ignorar este mensaje.");

        return baseWrapper(body);
    }
}