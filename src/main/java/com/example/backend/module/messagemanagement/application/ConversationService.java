package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.dto.ConversationResponse;
import com.example.backend.module.messagemanagement.dto.CreateConversationRequest;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio para conversaciones.
 *
 * Invariante: user1Id siempre es el menor → garantiza unicidad sin duplicados.
 */
@Service
public class ConversationService {

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private UserRepository         userRepository;

    // ─── Listado ──────────────────────────────────────────────────────────────

    /**
     * Lista todas las conversaciones del usuario autenticado.
     * Orden: última actividad DESC (como WhatsApp).
     * Sin JOIN: todos los campos vienen de la tabla conversations.
     */
    @Transactional(readOnly = true)
    public Page<ConversationResponse> listConversations(Long userId, int page, int size) {
        Page<ConversationModel> conversations =
                conversationRepository.findAllByUserId(userId, PageRequest.of(page, size));

        return conversations.map(c -> toResponse(c, userId));
    }

    // ─── Crear conversación ───────────────────────────────────────────────────

    /**
     * Crea una conversación entre el usuario autenticado y targetUserId.
     * Si ya existe, la retorna sin duplicar.
     */
    @Transactional
    public ConversationResponse createOrGet(Long currentUserId, CreateConversationRequest req) {
        Long targetId = req.targetUserId();

        if (currentUserId.equals(targetId))
            throw new AppException(AppCode.CONV_SELF_CONVERSATION);

        // Normalizar: user1 siempre es el menor
        Long u1 = Math.min(currentUserId, targetId);
        Long u2 = Math.max(currentUserId, targetId);

        // Si ya existe, retornarla
        return conversationRepository.findByParticipants(u1, u2)
                .map(c -> toResponse(c, currentUserId))
                .orElseGet(() -> createNew(u1, u2, currentUserId));
    }

    /**
     * Crea la conversación entre u1 y u2. Dos requests simultáneos (ambos
     * lados abriendo el chat a la vez) pueden pasar el `findByParticipants`
     * de arriba viendo "no existe" y competir por el mismo INSERT — el
     * UNIQUE(user1_id,user2_id) de BD garantiza que solo uno gane; el que
     * pierde recibe DataIntegrityViolationException en vez de un 500, y acá
     * simplemente se le devuelve la conversación que sí se creó.
     */
    private ConversationResponse createNew(Long u1, Long u2, Long currentUserId) {
        try {
            UserModel user1 = userRepository.findById(u1)
                    .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
            UserModel user2 = userRepository.findById(u2)
                    .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));

            ConversationModel conv = ConversationModel.builder()
                    .user1Id(u1)
                    .user2Id(u2)
                    .user1Username(user1.getUsername())
                    .user2Username(user2.getUsername())
                    .user1Avatar(null) // se actualizará cuando el user suba avatar
                    .user2Avatar(null)
                    .status(ConversationStatus.OFFLINE)
                    .build();

            // saveAndFlush (no save): el INSERT debe ejecutarse YA, dentro de
            // este try, para que la violación del UNIQUE se lance acá mismo
            // en vez de en el flush automático al final de la transacción,
            // donde ya no habría catch posible.
            return toResponse(conversationRepository.saveAndFlush(conv), currentUserId);
        } catch (DataIntegrityViolationException e) {
            return conversationRepository.findByParticipants(u1, u2)
                    .map(c -> toResponse(c, currentUserId))
                    .orElseThrow(() -> e);
        }
    }

    // ─── Silenciar / reactivar ────────────────────────────────────────────────

    @Transactional
    public ConversationResponse setMuted(Long currentUserId, Long conversationId, boolean muted) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));
        if (!conv.getUser1Id().equals(currentUserId) && !conv.getUser2Id().equals(currentUserId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        if (conv.getUser1Id().equals(currentUserId)) conv.setMutedUser1(muted);
        else conv.setMutedUser2(muted);

        return toResponse(conversationRepository.save(conv), currentUserId);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private ConversationResponse toResponse(ConversationModel c, Long currentUserId) {
        boolean isUser1 = c.getUser1Id().equals(currentUserId);

        Long   otherId       = isUser1 ? c.getUser2Id()       : c.getUser1Id();
        String otherUsername = isUser1 ? c.getUser2Username()  : c.getUser1Username();
        String otherAvatar   = isUser1 ? c.getUser2Avatar()    : c.getUser1Avatar();
        int    unread        = isUser1 ? c.getUnreadCountUser1(): c.getUnreadCountUser2();
        boolean muted        = isUser1 ? c.isMutedUser1()      : c.isMutedUser2();
        boolean isMine       = currentUserId.equals(c.getLastMessageSenderId());

        return new ConversationResponse(
                c.getId(),
                otherId,
                otherUsername,
                otherAvatar,
                c.getLastMessagePreview(),
                isMine,
                c.getLastMessageAt(),
                c.getStatus(),
                unread,
                muted
        );
    }
}