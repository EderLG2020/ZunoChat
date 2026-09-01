package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.enums.ConversationType;
import com.example.backend.common.enums.GroupRole;
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
                        .role(u.getId().equals(creatorId) ? GroupRole.OWNER : GroupRole.MEMBER)
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

    // ─── Gestión de miembros (roles: OWNER > ADMIN > MEMBER — ver GroupRole) ──

    /** OWNER y ADMIN pueden agregar miembros. Ids ya presentes en el grupo se ignoran (no rompe el batch entero). */
    @Transactional
    public ConversationResponse addMembers(Long actorId, Long conversationId, List<Long> memberIds) {
        ConversationModel group = loadGroup(conversationId);
        GroupMemberModel actorMembership = requireMembership(conversationId, actorId);
        requireAtLeast(actorMembership, GroupRole.ADMIN);

        List<Long> toAdd = memberIds.stream()
                .distinct()
                .filter(id -> !groupMemberRepository.existsByConversationIdAndUserId(conversationId, id))
                .toList();

        List<GroupMemberModel> newMembers = new ArrayList<>();
        for (Long id : toAdd) {
            var user = userRepository.findById(id).orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
            newMembers.add(GroupMemberModel.builder()
                    .conversationId(conversationId)
                    .userId(user.getId())
                    .role(GroupRole.MEMBER)
                    .username(user.getUsername())
                    .avatar(user.getAvatar())
                    .build());
        }
        groupMemberRepository.saveAll(newMembers);

        return toGroupResponse(group, membersOf(conversationId), actorMembership);
    }

    /**
     * Quita a otro miembro del grupo. OWNER puede quitar ADMIN/MEMBER; ADMIN
     * solo puede quitar MEMBER; nadie puede quitar al OWNER por acá (tiene
     * que transferir la propiedad primero — ver transferOwnership).
     * Para salir uno mismo del grupo, ver leaveGroup — acá se rechaza
     * explícitamente para no confundir "me saco a mí mismo" con "salgo".
     */
    @Transactional
    public ConversationResponse removeMember(Long actorId, Long conversationId, Long targetUserId) {
        if (actorId.equals(targetUserId))
            throw new AppException(AppCode.GROUP_CANNOT_SELF_TARGET, "Usa el endpoint de salir del grupo para removerte a ti mismo");

        ConversationModel group = loadGroup(conversationId);
        GroupMemberModel actorMembership = requireMembership(conversationId, actorId);
        GroupMemberModel targetMembership = requireMembership(conversationId, targetUserId);

        if (targetMembership.getRole() == GroupRole.OWNER)
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK, "El propietario no puede ser removido — debe transferir la propiedad o salir");

        requireOutranks(actorMembership, targetMembership);

        groupMemberRepository.delete(targetMembership);
        return toGroupResponse(group, membersOf(conversationId), actorMembership);
    }

    /** El usuario autenticado sale del grupo. El OWNER no puede salir sin transferir la propiedad antes. */
    @Transactional
    public void leaveGroup(Long userId, Long conversationId) {
        loadGroup(conversationId);
        GroupMemberModel membership = requireMembership(conversationId, userId);

        if (membership.getRole() == GroupRole.OWNER)
            throw new AppException(AppCode.GROUP_OWNER_MUST_TRANSFER);

        groupMemberRepository.delete(membership);
    }

    /** Solo el OWNER puede promover a ADMIN o degradar a MEMBER. No se puede asignar OWNER acá — ver transferOwnership. */
    @Transactional
    public ConversationResponse updateMemberRole(Long actorId, Long conversationId, Long targetUserId, GroupRole newRole) {
        if (newRole == GroupRole.OWNER)
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK, "La propiedad se transfiere, no se asigna como rol");
        if (actorId.equals(targetUserId))
            throw new AppException(AppCode.GROUP_CANNOT_SELF_TARGET);

        ConversationModel group = loadGroup(conversationId);
        GroupMemberModel actorMembership = requireMembership(conversationId, actorId);
        if (actorMembership.getRole() != GroupRole.OWNER)
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK);

        GroupMemberModel targetMembership = requireMembership(conversationId, targetUserId);
        if (targetMembership.getRole() == GroupRole.OWNER)
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK);

        targetMembership.setRole(newRole);
        groupMemberRepository.save(targetMembership);

        return toGroupResponse(group, membersOf(conversationId), actorMembership);
    }

    /** Solo el OWNER actual puede transferir. El OWNER anterior queda como ADMIN (no se lo expulsa). */
    @Transactional
    public ConversationResponse transferOwnership(Long actorId, Long conversationId, Long newOwnerUserId) {
        if (actorId.equals(newOwnerUserId))
            throw new AppException(AppCode.GROUP_CANNOT_SELF_TARGET);

        ConversationModel group = loadGroup(conversationId);
        GroupMemberModel actorMembership = requireMembership(conversationId, actorId);
        if (actorMembership.getRole() != GroupRole.OWNER)
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK);

        GroupMemberModel newOwnerMembership = groupMemberRepository
                .findByConversationIdAndUserId(conversationId, newOwnerUserId)
                .orElseThrow(() -> new AppException(AppCode.GROUP_TARGET_NOT_OWNER));

        actorMembership.setRole(GroupRole.ADMIN);
        newOwnerMembership.setRole(GroupRole.OWNER);
        groupMemberRepository.save(actorMembership);
        groupMemberRepository.save(newOwnerMembership);

        group.setCreatedBy(newOwnerUserId);
        conversationRepository.save(group);

        return toGroupResponse(group, membersOf(conversationId), newOwnerMembership);
    }

    // ─── Helpers de membresía/rango ───────────────────────────────────────────

    private ConversationModel loadGroup(Long conversationId) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));
        if (conv.getType() != ConversationType.GROUP)
            throw new AppException(AppCode.CONV_NOT_GROUP);
        return conv;
    }

    private GroupMemberModel requireMembership(Long conversationId, Long userId) {
        return groupMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AppException(AppCode.GROUP_NOT_MEMBER));
    }

    /** OWNER=2, ADMIN=1, MEMBER=0 — enum ordinal en orden de declaración (ver GroupRole), invertido: menor ordinal = más rango. */
    private int rank(GroupRole role) {
        return switch (role) {
            case OWNER -> 2;
            case ADMIN -> 1;
            case MEMBER -> 0;
        };
    }

    private void requireAtLeast(GroupMemberModel membership, GroupRole minimum) {
        if (rank(membership.getRole()) < rank(minimum))
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK);
    }

    private void requireOutranks(GroupMemberModel actor, GroupMemberModel target) {
        if (rank(actor.getRole()) <= rank(target.getRole()))
            throw new AppException(AppCode.GROUP_INSUFFICIENT_RANK);
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