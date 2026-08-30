package com.example.backend.module.streak.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tabla: streaks
 *
 * Una fila por conversación DIRECT con seguimiento de racha activo — no se
 * crea hasta que alguno de los dos participantes la solicita desde
 * Configuración (ver StreakService#requestActivation). Por defecto ninguna
 * conversación cuenta racha: es opt-in mutuo, no automático.
 *
 * Invariante: userAId siempre es el menor (mismo criterio que
 * ConversationModel#user1Id) — no indica "quién empezó", solo evita filas
 * duplicadas para el mismo par de usuarios.
 *
 * lastMessageDateA/B guardan, por separado, el último día (UTC) en que cada
 * lado escribió al menos un mensaje. Un día solo cuenta para la racha
 * (currentCount) cuando AMBOS coinciden en la fecha de hoy — ver
 * StreakCalculator, que es quien decide cuándo mover lastInteractionDate.
 */
@Entity
@Table(
        name = "streaks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_streak_conversation", columnNames = "conversation_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StreakModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    @Column(name = "current_count", nullable = false)
    @Builder.Default
    private int currentCount = 0;

    @Column(name = "longest_count", nullable = false)
    @Builder.Default
    private int longestCount = 0;

    /** Último día (UTC) contado para la racha — el día en que AMBOS ya habían escrito. */
    @Column(name = "last_interaction_date")
    private LocalDate lastInteractionDate;

    @Column(name = "last_message_date_a")
    private LocalDate lastMessageDateA;

    @Column(name = "last_message_date_b")
    private LocalDate lastMessageDateB;

    /**
     * Opt-in mutuo: activar desde Configuración dispara una solicitud (ver
     * requestStatus) y solo pasa a true cuando el otro también acepta.
     * Desactivar, en cambio, es unilateral e inmediato para cualquiera.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 20)
    @Builder.Default
    private StreakRequestStatus requestStatus = StreakRequestStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StreakStatus status = StreakStatus.INACTIVE;

    /**
     * Lock optimista de respaldo — la concurrencia real ante mensajes casi
     * simultáneos la resuelve el PESSIMISTIC_WRITE de
     * StreakRepository#findByConversationIdForUpdate, usado dentro de la
     * transacción de StreakService#recordInteraction.
     */
    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
