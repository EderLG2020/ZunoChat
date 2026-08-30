package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.messagemanagement.application.MessageService;
import com.example.backend.module.messagemanagement.dto.EditMessageRequest;
import com.example.backend.module.messagemanagement.dto.MarkReadRequest;
import com.example.backend.module.messagemanagement.dto.MessageCursorPage;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.realtime.messaging.IMessageProducer;
import com.example.backend.module.messagemanagement.realtime.messaging.MessageEventFactory;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptBroadcastEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * GET    /api/messages?conversationId={id}&beforeId={cursor}  → lista mensajes por cursor (más reciente primero)
 * POST   /api/messages                                        → enviar mensaje
 * PATCH  /api/messages/read                                   → marcar como visto (READ)
 * PATCH  /api/messages/{id}                                   → editar (solo texto, ventana de 15 min)
 * DELETE /api/messages/{id}                                   → borrar (soft delete)
 */
@RestController
@RequestMapping("/api/messages")
@Validated
public class MessageController {

    @Autowired private MessageService        messageService;
    @Autowired private IMessageProducer      messageProducer;
    @Autowired private MessageEventFactory   messageEventFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<MessageCursorPage>> list(
            @RequestParam Long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(50) int size
    ) {
        Long userId = JwtUtil.currentUserId();
        MessageCursorPage result = messageService.listMessages(conversationId, userId, beforeId, size);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> send(@Valid @RequestBody SendMessageRequest req) {
        Long userId = JwtUtil.currentUserId();
        MessageResponse result = messageService.sendMessage(userId, req);

        messageProducer.publishMessage(messageEventFactory.from(result, userId, JwtUtil.currentUsername()));

        return ResponseEntity
                .status(AppCode.OK_CREATED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_CREATED, result));
    }

    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@Valid @RequestBody MarkReadRequest req) {
        Long userId = JwtUtil.currentUserId();
        int updated = messageService.markAsRead(req.conversationId(), userId);

        // ✅ Publicar read receipt por WS para que el emisor vea el "visto"
        if (updated > 0) {
            String username = JwtUtil.currentUsername();
            messageProducer.publishReadReceipt(new ReadReceiptBroadcastEvent(
                    req.conversationId(), userId, username,
                    updated, LocalDateTime.now()
            ));
        }

        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC,
                        updated + " mensaje(s) marcados como leídos")
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> edit(@PathVariable Long id, @Valid @RequestBody EditMessageRequest req) {
        Long userId = JwtUtil.currentUserId();
        MessageResponse result = messageService.editMessage(id, userId, req.textContent());

        messageProducer.publishMessageUpdate(messageEventFactory.from(result, userId, JwtUtil.currentUsername()));

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_MESSAGE_EDITED, result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(@PathVariable Long id) {
        Long userId = JwtUtil.currentUserId();
        MessageResponse result = messageService.deleteMessage(id, userId);

        messageProducer.publishMessageUpdate(messageEventFactory.from(result, userId, JwtUtil.currentUsername()));

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_MESSAGE_DELETED, result));
    }

}
