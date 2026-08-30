package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.enums.ConversationType;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.domain.GroupMemberModel;
import com.example.backend.module.messagemanagement.dto.ConversationResponse;
import com.example.backend.module.messagemanagement.dto.CreateConversationRequest;
import com.example.backend.module.messagemanagement.dto.CreateGroupRequest;
import com.example.backend.module.messagemanagement.dto.GroupMemberResponse;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.GroupMemberRepository;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para conversaciones.
 *
 * Invariante (solo DIRECT): user1Id siempre es el menor → garantiza unicidad sin duplicados.
 * Las conversaciones GROUP no usan user1Id/user2Id (quedan null) — su membresía
 * vive en GroupMemberModel (ver esa clase).
 */
@Service
public class ConversationService {

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private GroupMemberRepository  groupMemberRepository;
    @Autowired private UserRepository         userRepository;

    // ─── Listado ──────────────────────────────────────────────────────────────

    /**
     * Lista todas las conversaciones del usuario autenticado — DIRECT y GROUP
     * mezcladas y ordenadas por última actividad DESC (como WhatsApp).
     *
     * findAllByUserId ya excluye naturalmente las GROUP (user1Id/user2Id son
     * null ahí, nunca igualan a :userId), así que se complementa con las
     * membresías de grupo del usuario. El merge + orden final se hace en
     * memoria: el total de conversaciones por usuario es chico (decenas, no
     * miles), así que no vale la pena una consulta SQL combinada solo para
     * mantener el conteo de la Page exacto.
     */
    @Transactional(readOnly = true)
    public Page<ConversationResponse> listConversations(Long userId, int page, int size) {
        int fetchLimit = (page + 1) * size;

        List<ConversationResponse> merged = new ArrayList<>();

        conversationRepository.findAllByUserId(userId, PageRequest.of(0, fetchLimit))
                .forEach(c -> merged.add(toDirectResponse(c, userId)));

        List<GroupMemberModel> myMemberships = groupMemberRepository.findByUserId(userId);
        if (!myMemberships.isEmpty()) {
            List<Long> groupIds = myMemberships.stream().map(GroupMemberModel::getConversationId).toList();
            Map<Long, ConversationModel> groupsById = conversationRepository.findAllById(groupIds).stream()
                    .collect(Collectors.toMap(ConversationModel::getId, c -> c));

            for (GroupMemberModel membership : myMemberships) {
                ConversationModel group = groupsById.get(membership.getConversationId());
                if (group == null) continue; // no debería pasar, pero no tumbar el listado por una fila huérfana
                merged.add(toGroupResponse(group, membersOf(group.getId()), membership));
            }
        }

        merged.sort(Comparator.comparing(
                ConversationResponse::lastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int from = Math.min(page * size, merged.size());
        int to   = Math.min(from + size, merged.size());

        return new PageImpl<>(merged.subList(from, to), PageRequest.of(page, size), merged.size());
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
                .map(c -> toDirectResponse(c, currentUserId))
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
                    .user1Avatar(user1.getAvatar())
                    .user2Avatar(user2.getAvatar())
                    .status(ConversationStatus.OFFLINE)
                    .build();

            // saveAndFlush (no save): el INSERT debe ejecutarse YA, dentro de
            // este try, para que la violación del UNIQUE se lance acá mismo
            // en vez de en el flush automático al final de la transacción,
            // donde ya no habría catch posible.
            return toDirectResponse(conversationRepository.saveAndFlush(conv), currentUserId);
        } catch (DataIntegrityViolationException e) {
            return conversationRepository.findByParticipants(u1, u2)
                    .map(c -> toDirectResponse(c, currentUserId))
                    .orElseThrow(() -> e);
        }
    }

    // ─── Crear grupo ──────────────────────────────────────────────────────────

    /**
     * Crea un grupo con el usuario autenticado como creador + los miembros
     * indicados. memberIds no necesita (ni debe asumirse que) incluya al
     * creador — se agrega automáticamente y se ignoran duplicados.
     */
    @Transactional
    public ConversationResponse createGroup(Long creatorId, CreateGroupRequest req) {
        // LinkedHashSet: preserva el orden de selección y de paso deduplica
        // sin depender de que el frontend no repita ids.
        Set<Long> memberIds = new LinkedHashSet<>(req.memberIds());
        memberIds.remove(creatorId);

        if (memberIds.size() < 2)
            throw new AppException(AppCode.CONV_GROUP_MIN_MEMBERS);

        List<UserModel> creatorAndMembers = new ArrayList<>();
        UserModel creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
        creatorAndMembers.add(creator);
        for (Long id : memberIds) {
            creatorAndMembers.add(userRepository.findById(id)
                    .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND)));
        }

        ConversationModel group = conversationRepository.save(ConversationModel.builder()
                .type(ConversationType.GROUP)
                .groupName(req.name())
                .createdBy(creatorId)
                .status(ConversationStatus.OFFLINE)
                .build());

        List<GroupMemberModel> members = creatorAndMembers.stream()
                .map(u -> GroupMemberModel.builder()
                        .conversationId(group.getId())
                        .userId(u.getId())
                        .username(u.getUsername())
                        .avatar(u.getAvatar())
                        .build())
                .toList();
        groupMemberRepository.saveAll(members);

        GroupMemberModel myMembership = members.stream()
                .filter(m -> m.getUserId().equals(creatorId))
                .findFirst().orElseThrow();

        return toGroupResponse(group, members, myMembership);
    }

    // ─── Silenciar / reactivar ────────────────────────────────────────────────

    @Transactional
    public ConversationResponse setMuted(Long currentUserId, Long conversationId, boolean muted) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        if (conv.getType() == ConversationType.GROUP) {
            GroupMemberModel membership = groupMemberRepository
                    .findByConversationIdAndUserId(conversationId, currentUserId)
                    .orElseThrow(() -> new AppException(AppCode.AUTH_FORBIDDEN));
            membership.setMuted(muted);
            groupMemberRepository.save(membership);
            return toGroupResponse(conv, membersOf(conv.getId()), membership);
        }

        if (!conv.getUser1Id().equals(currentUserId) && !conv.getUser2Id().equals(currentUserId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        if (conv.getUser1Id().equals(currentUserId)) conv.setMutedUser1(muted);
        else conv.setMutedUser2(muted);

        return toDirectResponse(conversationRepository.save(conv), currentUserId);
    }

    // ─── Chat temporal ────────────────────────────────────────────────────────

    /**
     * Prende/apaga el modo "chat temporal" — a diferencia de mute, es un
     * ajuste compartido de la conversación (no por lado): cualquier
     * participante lo puede cambiar y afecta a ambos/todos por igual.
     */
    @Transactional
    public ConversationResponse setEphemeral(Long currentUserId, Long conversationId, boolean enabled) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        if (conv.getType() == ConversationType.GROUP) {
            GroupMemberModel membership = groupMemberRepository
                    .findByConversationIdAndUserId(conversationId, currentUserId)
                    .orElseThrow(() -> new AppException(AppCode.AUTH_FORBIDDEN));
            conv.setEphemeralEnabled(enabled);
            return toGroupResponse(conversationRepository.save(conv), membersOf(conv.getId()), membership);
        }

        if (!conv.getUser1Id().equals(currentUserId) && !conv.getUser2Id().equals(currentUserId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        conv.setEphemeralEnabled(enabled);
        return toDirectResponse(conversationRepository.save(conv), currentUserId);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private List<GroupMemberModel> membersOf(Long conversationId) {
        return groupMemberRepository.findByConversationId(conversationId);
    }

    private ConversationResponse toDirectResponse(ConversationModel c, Long currentUserId) {
        boolean isUser1 = c.getUser1Id().equals(currentUserId);

        Long   otherId       = isUser1 ? c.getUser2Id()       : c.getUser1Id();
        String otherUsername = isUser1 ? c.getUser2Username()  : c.getUser1Username();
        String otherAvatar   = isUser1 ? c.getUser2Avatar()    : c.getUser1Avatar();
        int    unread        = isUser1 ? c.getUnreadCountUser1(): c.getUnreadCountUser2();
        boolean muted        = isUser1 ? c.isMutedUser1()      : c.isMutedUser2();
        boolean isMine       = currentUserId.equals(c.getLastMessageSenderId());

        return new ConversationResponse(
                c.getId(),
                ConversationType.DIRECT,
                otherId,
                otherUsername,
                otherAvatar,
                null, null, null,
                c.getLastMessagePreview(),
                isMine,
                c.getLastMessageAt(),
                c.getStatus(),
                unread,
                muted,
                c.isEphemeralEnabled()
        );
    }

    private ConversationResponse toGroupResponse(ConversationModel c, List<GroupMemberModel> members, GroupMemberModel myMembership) {
        boolean isMine = myMembership.getUserId().equals(c.getLastMessageSenderId());

        return new ConversationResponse(
                c.getId(),
                ConversationType.GROUP,
                null, null, null,
                c.getGroupName(),
                c.getGroupAvatar(),
                members.stream().map(GroupMemberResponse::from).toList(),
                c.getLastMessagePreview(),
                isMine,
                c.getLastMessageAt(),
                ConversationStatus.OFFLINE,
                myMembership.getUnreadCount(),
                myMembership.isMuted(),
                c.isEphemeralEnabled()
        );
    }
}