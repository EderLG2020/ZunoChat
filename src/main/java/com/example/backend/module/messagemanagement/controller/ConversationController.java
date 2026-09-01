package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;

import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.messagemanagement.application.ConversationService;
import com.example.backend.module.messagemanagement.dto.AddGroupMembersRequest;
import com.example.backend.module.messagemanagement.dto.ConversationResponse;
import com.example.backend.module.messagemanagement.dto.CreateConversationRequest;
import com.example.backend.module.messagemanagement.dto.CreateGroupRequest;
import com.example.backend.module.messagemanagement.dto.MuteConversationRequest;
import com.example.backend.module.messagemanagement.dto.SetEphemeralRequest;
import com.example.backend.module.messagemanagement.dto.TransferGroupOwnershipRequest;
import com.example.backend.module.messagemanagement.dto.UpdateGroupMemberRoleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * GET  /api/conversations          → lista de conversaciones del usuario
 * POST /api/conversations          → crear (o recuperar) una conversación
 */
@RestController
@RequestMapping("/api/conversations")
@Validated
public class ConversationController {

    @Autowired private ConversationService conversationService;

    /**
     * Lista todas las conversaciones del usuario autenticado.
     * El userId se extrae del JWT → no hace falta pasarlo en la URL.
     *
     * Query params:
     *   page (default 0)
     *   size (default 20)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        Long userId = JwtUtil.currentUserId();
        Page<ConversationResponse> result = conversationService.listConversations(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    /**
     * Crea una conversación con otro usuario.
     * Si ya existe entre ambos, la retorna sin duplicar.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> create(@Valid @RequestBody CreateConversationRequest req) {
        Long userId = JwtUtil.currentUserId();
        ConversationResponse result = conversationService.createOrGet(userId, req);
        return ResponseEntity
                .status(AppCode.OK_CREATED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_CREATED, result));
    }

    /**
     * Crea un grupo con el usuario autenticado como creador.
     * memberIds no incluye al creador — se agrega automáticamente.
     */
    @PostMapping("/group")
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroup(@Valid @RequestBody CreateGroupRequest req) {
        Long userId = JwtUtil.currentUserId();
        ConversationResponse result = conversationService.createGroup(userId, req);
        return ResponseEntity
                .status(AppCode.OK_CREATED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_CREATED, result));
    }

    /** Silencia/reactiva la conversación solo para el usuario autenticado. */
    @PatchMapping("/{id}/mute")
    public ResponseEntity<ApiResponse<ConversationResponse>> mute(@PathVariable Long id, @RequestBody MuteConversationRequest req) {
        Long userId = JwtUtil.currentUserId();
        ConversationResponse result = conversationService.setMuted(userId, id, req.muted());
        AppCode code = req.muted() ? AppCode.OK_CONVERSATION_MUTED : AppCode.OK_CONVERSATION_UNMUTED;
        return ResponseEntity.ok(ApiResponse.ok(code, result));
    }

    /** Prende/apaga el chat temporal — afecta a la conversación entera, no solo al usuario autenticado. */
    @PatchMapping("/{id}/ephemeral")
    public ResponseEntity<ApiResponse<ConversationResponse>> setEphemeral(@PathVariable Long id, @RequestBody SetEphemeralRequest req) {
        Long userId = JwtUtil.currentUserId();
        ConversationResponse result = conversationService.setEphemeral(userId, id, req.enabled());
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    // ── Miembros de grupo (roles: OWNER > ADMIN > MEMBER) ──────────────────────

    /** OWNER y ADMIN del grupo pueden agregar miembros. */
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<ConversationResponse>> addMembers(@PathVariable Long id, @Valid @RequestBody AddGroupMembersRequest req) {
        ConversationResponse result = conversationService.addMembers(JwtUtil.currentUserId(), id, req.memberIds());
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GROUP_MEMBERS_ADDED, result));
    }

    /** Quita a otro miembro (OWNER puede quitar ADMIN/MEMBER, ADMIN solo MEMBER). Para salir uno mismo, ver /leave. */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        ConversationResponse result = conversationService.removeMember(JwtUtil.currentUserId(), id, userId);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GROUP_MEMBER_REMOVED, result));
    }

    /** El usuario autenticado sale del grupo. El OWNER debe transferir la propiedad antes (ver /transfer-ownership). */
    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(@PathVariable Long id) {
        conversationService.leaveGroup(JwtUtil.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GROUP_LEFT));
    }

    /** Solo el OWNER puede promover a ADMIN o degradar a MEMBER. */
    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<ApiResponse<ConversationResponse>> updateMemberRole(
            @PathVariable Long id, @PathVariable Long userId, @Valid @RequestBody UpdateGroupMemberRoleRequest req) {
        ConversationResponse result = conversationService.updateMemberRole(JwtUtil.currentUserId(), id, userId, req.role());
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GROUP_ROLE_UPDATED, result));
    }

    /** Solo el OWNER actual puede transferir la propiedad — queda como ADMIN tras transferirla. */
    @PostMapping("/{id}/transfer-ownership")
    public ResponseEntity<ApiResponse<ConversationResponse>> transferOwnership(
            @PathVariable Long id, @Valid @RequestBody TransferGroupOwnershipRequest req) {
        ConversationResponse result = conversationService.transferOwnership(JwtUtil.currentUserId(), id, req.newOwnerUserId());
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GROUP_OWNERSHIP_TRANSFERRED, result));
    }
}