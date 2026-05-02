package com.example.backend.module.messagemanagement.persistence;

import com.example.backend.module.messagemanagement.domain.ConversationModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}