package com.example.backend.module.messagemanagement.persistence;

import com.example.backend.module.messagemanagement.domain.GroupMemberModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMemberModel, Long> {

    List<GroupMemberModel> findByConversationId(Long conversationId);

    Optional<GroupMemberModel> findByConversationIdAndUserId(Long conversationId, Long userId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /** Todas las membresías de grupo del usuario — usadas para listar sus grupos junto a sus DMs. */
    List<GroupMemberModel> findByUserId(Long userId);

    /** Incrementa el contador de no leídos de todos los miembros salvo quien envió el mensaje. */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE GroupMemberModel g
        SET g.unreadCount = g.unreadCount + 1
        WHERE g.conversationId = :conversationId AND g.userId <> :senderId
        """)
    void incrementUnreadForOthers(@Param("conversationId") Long conversationId, @Param("senderId") Long senderId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE GroupMemberModel g
        SET g.unreadCount = 0
        WHERE g.conversationId = :conversationId AND g.userId = :userId
        """)
    void resetUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
