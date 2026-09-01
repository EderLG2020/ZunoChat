package com.example.backend.module.messagemanagement.domain;

import com.example.backend.common.enums.GroupRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tabla: group_members
 *
 * Membresía de un usuario en una conversación GROUP — el equivalente a las
 * columnas user1Id/user2Id de ConversationModel, pero para N participantes.
 * Guarda unread_count y muted por miembro, igual que ConversationModel lo
 * hace por lado (unreadCountUser1/2) para conversaciones DIRECT.
 *
 * Desnormaliza username/avatar (mismo criterio que ConversationModel) para
 * poder listar los miembros de un grupo sin JOIN contra users.
 */
@Entity
@Table(
        name = "group_members",
        indexes = {
                @Index(name = "idx_group_members_conversation", columnList = "conversation_id"),
                @Index(name = "idx_group_members_user",         columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_group_members_conv_user", columnNames = {"conversation_id", "user_id"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupMemberModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** OWNER (único por grupo, el creador salvo transferencia), ADMIN o MEMBER — ver GroupRole. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupRole role = GroupRole.MEMBER;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private Integer unreadCount = 0;

    @Column(name = "muted", nullable = false)
    @Builder.Default
    private boolean muted = false;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}
