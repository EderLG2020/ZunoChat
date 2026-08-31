package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.ConversationType;
import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.domain.MessageModel;
import com.example.backend.module.messagemanagement.dto.MessageCursorPage;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.GroupMemberRepository;
import com.example.backend.module.messagemanagement.persistence.MessageRepository;
import com.example.backend.module.messagemanagement.realtime.session.IWebSocketSessionRegistry;
import com.example.backend.module.streak.application.StreakService;
import com.example.backend.module.usermanagement.application.BlockedPairCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MessageService {

    /** Ventana para poder editar un mensaje de texto después de enviado. */
    private static final long EDIT_WINDOW_MINUTES = 15;

    /** SendMessageRequest.textContent ya tiene @Size(max=4000) — este límite cubre el payload (Object, sin validación de tamaño por Bean Validation). */
    private static final int MAX_PAYLOAD_BYTES = 8_000;

    /** Vida de un mensaje enviado con la conversación en modo "chat temporal" — ver EphemeralMessageSweeper. */
    public static final long EPHEMERAL_TTL_HOURS = 24;

    @Autowired private MessageRepository           messageRepository;
    @Autowired private ConversationRepository      conversationRepository;
    @Autowired private GroupMemberRepository       groupMemberRepository;
    @Autowired private IWebSocketSessionRegistry   sessionRegistry;
    @Autowired private BlockedPairCache            blockedPairCache;
    @Autowired private ObjectMapper                objectMapper;
    @Autowired private StreakService               streakService;

    // ─── Listar mensajes ─────────────────────────────────────────────────────

    /**
     * beforeId = null → los `size` mensajes más recientes de la conversación.
     * beforeId != null → los `size` mensajes inmediatamente anteriores a ese id.
     * Pide size+1 filas para saber si hay más sin necesitar un COUNT(*) aparte.
     */
    @Transactional(readOnly = true)
    public MessageCursorPage listMessages(Long conversationId, Long currentUserId,
                                          Long beforeId, int size) {
        getConversationAndVerifyParticipant(conversationId, currentUserId);

        List<MessageModel> rows = messageRepository.findPageByConversationId(
                conversationId, beforeId, PageRequest.of(0, size + 1));

        boolean hasMore = rows.size() > size;
        List<MessageModel> page = hasMore ? rows.subList(0, size) : rows;
        Long nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getId();

        return new MessageCursorPage(page.stream().map(this::toResponse).toList(), hasMore, nextCursor);
    }

    // ─── Enviar mensaje (REST + WS comparten este método) ────────────────────

    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest req) {
        ConversationModel conv = getConversationAndVerifyParticipant(req.conversationId(), senderId);
        boolean isGroup = conv.getType() == ConversationType.GROUP;

        // GROUP no tiene un único receptor — ni concepto de bloqueo 1:1 (el
        // bloqueo entre dos usuarios no expulsa a nadie de un grupo compartido).
        Long receiverId = null;
        if (!isGroup) {
            receiverId = conv.getUser1Id().equals(senderId) ? conv.getUser2Id() : conv.getUser1Id();
            if (blockedPairCache.isBlocked(senderId, receiverId))
                throw new AppException(AppCode.USER_BLOCKED_CONTACT);
        }

        validateMessage(req);

        // Reintento idempotente: si el cliente ya mandó este clientMessageId
        // antes (timeout de red, doble tap reintentando el POST), se devuelve
        // el mensaje que ya se creó y transmitió la primera vez, sin duplicar.
        if (req.clientMessageId() != null) {
            MessageModel existing = messageRepository.findByClientMessageId(req.clientMessageId()).orElse(null);
            if (existing != null) {
                if (!existing.getSenderId().equals(senderId) || !existing.getConversationId().equals(req.conversationId()))
                    throw new AppException(AppCode.MSG_CLIENT_ID_CONFLICT);
                return toResponse(existing);
            }
        }

        // Se resuelve el status ANTES de construir la entidad para no pagar
        // un segundo INSERT/UPDATE cuando el receptor ya está conectado (el
        // caso más común) — antes se guardaba como SENT y, si correspondía,
        // se volvía a guardar como DELIVERED en un segundo round-trip a BD.
        // GROUP no trackea DELIVERED/READ por mensaje (no hay un único
        // receptor) — solo el contador de no leídos por miembro.
        MessageStatus initialStatus = (!isGroup && sessionRegistry.hasActiveSessions(receiverId))
                ? MessageStatus.DELIVERED : MessageStatus.SENT;

        MessageModel msg = MessageModel.builder()
                .conversationId(req.conversationId())
                .senderId(senderId)
                .receiverId(receiverId)
                .type(req.type())
                .textContent(req.textContent())
                .payload(req.payload())
                .payloadType(req.payloadType())
                .fileUrls(req.fileUrls())
                .status(initialStatus)
                .clientMessageId(req.clientMessageId())
                .expiresAt(conv.isEphemeralEnabled() ? LocalDateTime.now().plusHours(EPHEMERAL_TTL_HOURS) : null)
                .build();

        MessageModel saved;
        try {
            // saveAndFlush (no save): si dos requests con el mismo
            // clientMessageId chocan en una carrera real, el UNIQUE de BD
            // debe lanzar la excepción ACÁ, dentro del try — no en el flush
            // automático al final de la transacción, donde ya no hay catch posible.
            saved = messageRepository.saveAndFlush(msg);
        } catch (DataIntegrityViolationException e) {
            if (req.clientMessageId() == null) throw e; // no fue por el UNIQUE de clientMessageId
            saved = messageRepository.findByClientMessageId(req.clientMessageId()).orElseThrow(() -> e);
        }

        updateConversationPreview(conv, isGroup, senderId, req, saved.getSentAt());

        // GROUP no tiene racha (ver StreakService#recordInteraction) — solo
        // aplica a DIRECT, donde sí hay un único "otro" con quien contarla.
        if (!isGroup) {
            streakService.recordInteraction(req.conversationId(), senderId, receiverId);
        }

        return toResponse(saved);
    }

    // ─── Marcar como leído ────────────────────────────────────────────────────

    @Transactional
    public int markAsRead(Long conversationId, Long currentUserId) {
        ConversationModel conv = getConversationAndVerifyParticipant(conversationId, currentUserId);

        if (conv.getType() == ConversationType.GROUP) {
            // GROUP no trackea READ por mensaje (ver sendMessage) — solo
            // resetea el contador de no leídos de este miembro. Sin
            // mensajes actualizados, no hay read receipt que emitir por WS.
            groupMemberRepository.resetUnread(conversationId, currentUserId);
            return 0;
        }

        int updated = messageRepository.markAsRead(conversationId, currentUserId, MessageStatus.READ);

        if (conv.getUser1Id().equals(currentUserId)) {
            conversationRepository.resetUnreadUser1(conversationId);
        } else {
            conversationRepository.resetUnreadUser2(conversationId);
        }

        return updated;
    }

    // ─── Borrar mensaje (soft delete) ─────────────────────────────────────────

    @Transactional
    public MessageResponse deleteMessage(Long messageId, Long currentUserId) {
        MessageModel msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(AppCode.MSG_NOT_FOUND));

        if (!msg.getSenderId().equals(currentUserId))
            throw new AppException(AppCode.MSG_NOT_OWNER);
        if (msg.isDeleted())
            throw new AppException(AppCode.MSG_ALREADY_DELETED);

        msg.setDeleted(true);
        msg.setTextContent(null);
        msg.setPayload(null);
        msg.setFileUrls(null);
        MessageModel saved = messageRepository.save(msg);

        return toResponse(saved);
    }

    // ─── Editar mensaje ────────────────────────────────────────────────────────

    @Transactional
    public MessageResponse editMessage(Long messageId, Long currentUserId, String newText) {
        MessageModel msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(AppCode.MSG_NOT_FOUND));

        if (!msg.getSenderId().equals(currentUserId))
            throw new AppException(AppCode.MSG_NOT_OWNER);
        if (msg.isDeleted())
            throw new AppException(AppCode.MSG_ALREADY_DELETED);
        if (msg.getType() != MessageType.TEXT)
            throw new AppException(AppCode.MSG_EDIT_NOT_TEXT);
        if (msg.getSentAt().plus(EDIT_WINDOW_MINUTES, ChronoUnit.MINUTES).isBefore(LocalDateTime.now()))
            throw new AppException(AppCode.MSG_EDIT_WINDOW_EXPIRED);
        if (newText == null || newText.isBlank())
            throw new AppException(AppCode.MSG_TEXT_REQUIRED);

        msg.setTextContent(newText);
        msg.setEditedAt(LocalDateTime.now());
        MessageModel saved = messageRepository.save(msg);

        return toResponse(saved);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private ConversationModel getConversationAndVerifyParticipant(Long convId, Long userId) {
        ConversationModel conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        if (conv.getType() == ConversationType.GROUP) {
            if (!groupMemberRepository.existsByConversationIdAndUserId(convId, userId))
                throw new AppException(AppCode.AUTH_FORBIDDEN);
            return conv;
        }

        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);
        return conv;
    }

    private void validateMessage(SendMessageRequest req) {
        if (req.type() == MessageType.TEXT && (req.textContent() == null || req.textContent().isBlank()))
            throw new AppException(AppCode.MSG_TEXT_REQUIRED);
        if (req.type() == MessageType.PAYLOAD && req.payload() == null)
            throw new AppException(AppCode.MSG_PAYLOAD_REQUIRED);
        if (req.type() == MessageType.FILE && (req.fileUrls() == null || req.fileUrls().isEmpty()))
            throw new AppException(AppCode.MSG_FILE_REQUIRED);
        if (req.fileUrls() != null && req.fileUrls().size() > 3)
            throw new AppException(AppCode.MSG_FILE_LIMIT);
        if (req.payload() != null && payloadSizeBytes(req.payload()) > MAX_PAYLOAD_BYTES)
            throw new AppException(AppCode.MSG_PAYLOAD_TOO_LARGE);
    }

    private int payloadSizeBytes(Object payload) {
        try {
            return objectMapper.writeValueAsBytes(payload).length;
        } catch (JsonProcessingException e) {
            // Un payload que ni siquiera serializa fallará más adelante al
            // persistir/serializar la respuesta — no es este método el que
            // debe decidir ese error, así que se deja pasar el chequeo de tamaño.
            return 0;
        }
    }

    private void updateConversationPreview(ConversationModel conv, boolean isGroup, Long senderId,
                                           SendMessageRequest req, LocalDateTime sentAt) {
        String preview = buildPreview(req);

        if (isGroup) {
            conversationRepository.applyNewMessageGroup(conv.getId(), preview, senderId, sentAt);
            groupMemberRepository.incrementUnreadForOthers(conv.getId(), senderId);
            return;
        }

        boolean senderIsUser1 = conv.getUser1Id().equals(senderId);
        if (senderIsUser1) {
            conversationRepository.applyNewMessageFromUser1(conv.getId(), preview, senderId, sentAt);
        } else {
            conversationRepository.applyNewMessageFromUser2(conv.getId(), preview, senderId, sentAt);
        }
    }

    private String buildPreview(SendMessageRequest req) {
        String raw = switch (req.type()) {
            case TEXT    -> req.textContent() != null ? req.textContent() : "";
            case IMAGE   -> "📷 Imagen";
            case AUDIO   -> "🎤 Audio";
            case FILE    -> "📎 Archivo adjunto";
            case PAYLOAD -> switch (req.payloadType() != null ? req.payloadType() : PayloadType.SYSTEM) {
                case SALES  -> "🛒 Oferta";
                case SYSTEM -> "⚙️ Notificación";
                case SURVEY -> "📋 Encuesta";
                case CARD   -> "🃏 Tarjeta";
            };
        };
        return raw.length() > 50 ? raw.substring(0, 47) + "..." : raw;
    }

    /** Package-private (no private): EphemeralMessageSweeper también la usa para mapear tras el soft-delete automático. */
    MessageResponse toResponse(MessageModel m) {
        return new MessageResponse(
                m.getId(), m.getConversationId(),
                m.getSenderId(), m.getReceiverId(),
                m.getType(), m.getTextContent(),
                m.getPayload(), m.getPayloadType(),
                m.getFileUrls(), m.getStatus(),
                m.getSentAt(), m.getReadAt(),
                m.isDeleted(), m.getEditedAt(),
                m.getExpiresAt()
        );
    }
}