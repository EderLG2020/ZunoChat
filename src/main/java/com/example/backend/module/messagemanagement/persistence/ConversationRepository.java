package com.example.backend.module.messagemanagement.persistence;

import com.example.backend.module.messagemanagement.domain.ConversationModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<ConversationModel, Long> {

    /**
     * Lista todas las conversaciones de un usuario, ordenadas por último mensaje.
     * Un usuario puede ser user1 o user2 → condición OR con índices separados.
     */
    @Query("""
        SELECT c FROM ConversationModel c
        WHERE c.user1Id = :userId OR c.user2Id = :userId
        ORDER BY c.lastMessageAt DESC NULLS LAST
        """)
    Page<ConversationModel> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Busca la conversación única entre dos usuarios.
     * Siempre busca con min/max para mantener la invariante user1Id < user2Id.
     */
    @Query("""
        SELECT c FROM ConversationModel c
        WHERE c.user1Id = :u1 AND c.user2Id = :u2
        """)
    Optional<ConversationModel> findByParticipants(@Param("u1") Long u1, @Param("u2") Long u2);

    /**
     * Verifica si ya existe una conversación entre dos usuarios.
     */
    @Query("""
        SELECT COUNT(c) > 0 FROM ConversationModel c
        WHERE c.user1Id = :u1 AND c.user2Id = :u2
        """)
    boolean existsByParticipants(@Param("u1") Long u1, @Param("u2") Long u2);

    /**
     * Aplica un mensaje nuevo enviado por user1 (preview + incremento de no
     * leídos de user2) en un único UPDATE atómico — evita el "lost update"
     * de leer el contador, sumarle 1 en memoria y guardarlo de vuelta, que
     * pierde incrementos bajo envíos concurrentes a la misma conversación.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ConversationModel c
        SET c.lastMessagePreview = :preview,
            c.lastMessageSenderId = :senderId,
            c.lastMessageAt = :sentAt,
            c.unreadCountUser2 = c.unreadCountUser2 + 1
        WHERE c.id = :id
        """)
    void applyNewMessageFromUser1(@Param("id") Long id, @Param("preview") String preview,
                                   @Param("senderId") Long senderId, @Param("sentAt") LocalDateTime sentAt);

    /** Misma operación que {@link #applyNewMessageFromUser1}, cuando el emisor es user2. */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ConversationModel c
        SET c.lastMessagePreview = :preview,
            c.lastMessageSenderId = :senderId,
            c.lastMessageAt = :sentAt,
            c.unreadCountUser1 = c.unreadCountUser1 + 1
        WHERE c.id = :id
        """)
    void applyNewMessageFromUser2(@Param("id") Long id, @Param("preview") String preview,
                                   @Param("senderId") Long senderId, @Param("sentAt") LocalDateTime sentAt);

    /** Igual que applyNewMessageFromUser1/2, pero para GROUP: una sola fila, sin lado — el
     *  incremento de no leídos por miembro lo hace GroupMemberRepository#incrementUnreadForOthers. */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ConversationModel c
        SET c.lastMessagePreview = :preview,
            c.lastMessageSenderId = :senderId,
            c.lastMessageAt = :sentAt
        WHERE c.id = :id
        """)
    void applyNewMessageGroup(@Param("id") Long id, @Param("preview") String preview,
                              @Param("senderId") Long senderId, @Param("sentAt") LocalDateTime sentAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationModel c SET c.unreadCountUser1 = 0 WHERE c.id = :id")
    void resetUnreadUser1(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationModel c SET c.unreadCountUser2 = 0 WHERE c.id = :id")
    void resetUnreadUser2(@Param("id") Long id);

    // ─── Sincronización de datos desnormalizados (ver UserProfileService) ─────
    // Separadas por lado porque user1Username/user2Username son columnas
    // distintas — no hay forma de actualizar "la que corresponda" en un único
    // UPDATE sin CASE, y user1Id/user2Id nunca es el mismo valor a la vez.

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationModel c SET c.user1Username = :username WHERE c.user1Id = :userId")
    void updateUser1Username(@Param("userId") Long userId, @Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationModel c SET c.user2Username = :username WHERE c.user2Id = :userId")
    void updateUser2Username(@Param("userId") Long userId, @Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationModel c SET c.user1Avatar = :avatar WHERE c.user1Id = :userId")
    void updateUser1Avatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationModel c SET c.user2Avatar = :avatar WHERE c.user2Id = :userId")
    void updateUser2Avatar(@Param("userId") Long userId, @Param("avatar") String avatar);
}