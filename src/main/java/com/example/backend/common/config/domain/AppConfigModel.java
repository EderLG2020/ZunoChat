package com.example.backend.common.config.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tabla de configuración dinámica del sistema.
 *
 * Almacena pares clave-valor que pueden cambiar en tiempo de ejecución
 * sin necesidad de reiniciar el servidor.
 *
 * Filas iniciales (creadas por el DDL / data.sql):
 *   key = "email.enabled"  →  value = "true"  (dev)
 *   key = "email.enabled"  →  value = "false" (prod)
 */
@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfigModel {

    @Id
    @Column(name = "config_key", nullable = false, length = 100)
    private String key;

    @Column(name = "config_value", nullable = false, length = 500)
    private String value;

    /** Descripción legible del parámetro (solo referencia interna) */
    @Column(length = 255)
    private String description;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Quién modificó el valor (id del admin) */
    private Long updatedBy;
}