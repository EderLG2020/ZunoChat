package com.example.backend.module.usermanagement.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tabla: blocked_users
 *
 * Fila = "blockerId bloqueó a blockedId". Dirigido (a diferencia de
 * conversations, donde user1/user2 no importa el orden) — si A bloquea a B,
 * B no queda bloqueado hacia A a menos que también lo haga explícitamente.
 * MessageService revisa ambas direcciones antes de dejar enviar un mensaje.
 */
@Entity
@Table(
        name = "blocked_users",
        indexes = {
                @Index(name = "idx_blocked_blocker", columnList = "blocker_id"),
                @Index(name = "idx_blocked_blocked", columnList = "blocked_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_blocked_pair", columnNames = {"blocker_id", "blocked_id"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlockedUserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
