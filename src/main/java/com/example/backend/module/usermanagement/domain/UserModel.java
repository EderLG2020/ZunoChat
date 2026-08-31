package com.example.backend.module.usermanagement.domain;

import com.example.backend.common.enums.AuthProvider;
import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.ThemePreference;
import com.example.backend.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad principal de usuario.
 * Incluye campos para autenticación, estado, auditoría y verificación OTP.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email",    columnNames = "email"),
        @UniqueConstraint(name = "uk_users_dni",      columnNames = "dni"),
        @UniqueConstraint(name = "uk_users_google_id", columnNames = "google_id")
    },
    indexes = {
        // Permite que la búsqueda de usuarios (prefijo) use un índice en vez de
        // escanear toda la tabla calculando LOWER(username) fila por fila.
        @Index(name = "idx_users_username_lower", columnList = "username_lower")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Datos de identidad ───────────────────────────────────────────────────

    /** Obligatorio para cuentas LOCAL. Null en cuentas creadas vía Google (ver authProvider). */
    @Column(length = 20)
    private String dni;

    /** Nombre de usuario único, visible públicamente */
    @Column(nullable = false, length = 50)
    private String username;

    /** Copia en minúsculas de username, mantenida automáticamente — soporta búsqueda indexada case-insensitive */
    @Column(name = "username_lower", nullable = false, length = 50)
    private String usernameLower;

    @Column(nullable = false, length = 120)
    private String email;

    /** Opcional — el usuario lo carga desde Configuración; null hasta entonces. */
    @Column(length = 20)
    private String phone;

    /** Contraseña encriptada con BCrypt. Null en cuentas creadas vía Google (ver authProvider). */
    private String password;

    /** URL de la foto de perfil. Nula hasta que el usuario suba una (o la que trae Google). */
    @Column(length = 500)
    private String avatar;

    // ─── Seguridad / Roles ───────────────────────────────────────────────────

    /**
     * Cómo se autentica: LOCAL (usuario/password + OTP) o GOOGLE (authorization
     * code flow). columnDefinition con DEFAULT: sin esto, el ALTER TABLE que
     * agrega la columna NOT NULL falla en filas ya existentes (mismo motivo que
     * themePreference más abajo).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20, columnDefinition = "varchar(20) default 'LOCAL'")
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /** "sub" del id_token de Google. Único, null en cuentas LOCAL. */
    @Column(name = "google_id", length = 50)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    /**
     * Preferencia de tema (claro/oscuro), sincronizada entre dispositivos.
     * columnDefinition con DEFAULT: sin esto, el ALTER TABLE que agrega la
     * columna NOT NULL falla en filas ya existentes (no hay valor que asignarles).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'LIGHT'")
    @Builder.Default
    private ThemePreference themePreference = ThemePreference.LIGHT;

    // ─── Verificación OTP ────────────────────────────────────────────────────

    /** Código OTP de 6 dígitos (se borra tras verificar) */
    @Column(length = 6)
    private String otpCode;

    /** Fecha límite para usar el OTP */
    private LocalDateTime otpExpiration;

    // ─── Auditoría ───────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Usuario que realizó el último ban/cambio de estado (para trazabilidad) */
    private Long updatedBy;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @PrePersist
    @PreUpdate
    private void syncUsernameLower() {
        this.usernameLower = this.username == null ? null : this.username.toLowerCase();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isBanned() {
        return this.status == UserStatus.BANNED;
    }

    public boolean isPendingVerification() {
        return this.status == UserStatus.PENDING_VERIFICATION;
    }
}
