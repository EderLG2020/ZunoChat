package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;

import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.messagemanagement.application.ConversationService;
import com.example.backend.module.messagemanagement.dto.ConversationResponse;
import com.example.backend.module.messagemanagement.dto.CreateConversationRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET  /api/conversations          → lista de conversaciones del usuario
 * POST /api/conversations          → crear (o recuperar) una conversación
 */
@RestController
@RequestMapping("/api/conversations")
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
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = JwtUtil.extractUserId(token);
        Page<ConversationResponse> result = conversationService.listConversations(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    /**
     * Crea una conversación con otro usuario.
     * Si ya existe entre ambos, la retorna sin duplicar.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> create(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateConversationRequest req
    ) {
        Long userId = JwtUtil.extractUserId(token);
        ConversationResponse result = conversationService.createOrGet(userId, req);
        return ResponseEntity
                .status(AppCode.OK_CREATED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_CREATED, result));
    }
}