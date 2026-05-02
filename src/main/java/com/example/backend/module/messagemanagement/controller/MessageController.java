package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.messagemanagement.application.MessageService;
import com.example.backend.module.messagemanagement.dto.MarkReadRequest;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET   /api/messages?conversationId={id}  → lista mensajes (inversa, paginada)
 * POST  /api/messages                      → enviar mensaje
 * PATCH /api/messages/read                 → marcar como visto (READ)
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired private MessageService messageService;

    /**
     * El usuario autenticado debe ser participante de la conversación.
     *
     * Query params:
     *   conversationId (obligatorio)
     *   page (default 0)
     *   size (default 30)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> list(
            @RequestHeader("Authorization") String token,
            @RequestParam Long conversationId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Long userId = JwtUtil.extractUserId(token);
        Page<MessageResponse> result = messageService.listMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    /**
     * Envía un mensaje dentro de una conversación existente.
     * El receptor se infiere desde la conversación → no hace falta especificarlo.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SendMessageRequest req
    ) {
        Long userId = JwtUtil.extractUserId(token);
        MessageResponse result = messageService.sendMessage(userId, req);
        return ResponseEntity
                .status(AppCode.OK_CREATED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_CREATED, result));
    }

    /**
     * Marca todos los mensajes recibidos en la conversación como READ (visto).
     * Equivale a abrir el chat en WhatsApp → doble check azul.
     */
    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody MarkReadRequest req
    ) {
        Long userId = JwtUtil.extractUserId(token);
        int updated = messageService.markAsRead(req.conversationId(), userId);
        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC,
                        updated + " mensaje(s) marcados como leídos")
        );
    }
}