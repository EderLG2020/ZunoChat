package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.messagemanagement.application.MessageService;
import com.example.backend.module.messagemanagement.dto.MarkReadRequest;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.realtime.messaging.IMessageProducer;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptRabbitEvent;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * GET   /api/messages?conversationId={id}  → lista mensajes (inversa, paginada)
 * POST  /api/messages                      → enviar mensaje
 * PATCH /api/messages/read                 → marcar como visto (READ)
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired private MessageService         messageService;
    @Autowired private IMessageProducer       messageProducer;       // ✅ AÑADIDO
    @Autowired private ConversationRepository conversationRepository; // ✅ AÑADIDO

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

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SendMessageRequest req
    ) {
        Long userId = JwtUtil.extractUserId(token);
        MessageResponse result = messageService.sendMessage(userId, req);

        String senderUsername   = SecurityContextHolder.getContext().getAuthentication().getName();
        String receiverUsername = conversationRepository.findById(result.conversationId())
                .map(conv -> conv.getUser1Id().equals(userId)
                        ? conv.getUser2Username()
                        : conv.getUser1Username())
                .orElse(result.receiverId().toString());

        messageProducer.publishMessage(new MessageEvent(
                result.messageId(), result.conversationId(),
                result.senderId(), senderUsername,
                result.receiverId(), receiverUsername,
                result.type(), result.textContent(),
                result.payload(), result.payloadType(),
                result.fileUrls(), result.status(), result.sentAt()
        ));

        return ResponseEntity
                .status(AppCode.OK_CREATED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_CREATED, result));
    }

    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody MarkReadRequest req
    ) {
        Long userId = JwtUtil.extractUserId(token);
        int updated = messageService.markAsRead(req.conversationId(), userId);

        // ✅ Publicar read receipt por WS para que el emisor vea el "visto"
        if (updated > 0) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            messageProducer.publishReadReceipt(new ReadReceiptRabbitEvent(
                    req.conversationId(), userId, username,
                    updated, LocalDateTime.now()
            ));
        }

        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC,
                        updated + " mensaje(s) marcados como leídos")
        );
    }
}