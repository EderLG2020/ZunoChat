package com.example.backend.module.messagemanagement.controller;

import com.example.backend.module.messagemanagement.application.MessageService;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.dto.ws.ReadReceiptEvent;
import com.example.backend.module.messagemanagement.dto.ws.TypingEvent;
import com.example.backend.module.messagemanagement.dto.ws.WsInboundMessage;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.realtime.messaging.IMessageProducer;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceBroadcastEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptBroadcastEvent;
import com.example.backend.module.messagemanagement.realtime.presence.IPresenceService;
import com.example.backend.module.messagemanagement.realtime.session.IWebSocketSessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

@Slf4j
@Controller
public class WebSocketController {

    @Autowired private MessageService            messageService;
    @Autowired private IMessageProducer          messageProducer;
    @Autowired private IPresenceService          presenceService;
    @Autowired private IWebSocketSessionRegistry sessionRegistry;
    @Autowired private SimpMessagingTemplate     messagingTemplate;
    @Autowired private ConversationRepository    conversationRepository; // ✅ para obtener receiverUsername

    // ─── Conexión ─────────────────────────────────────────────────────────────

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sessionId = accessor.getSessionId();
        Long userId = extractUserId(principal);
        if (userId == null) return;

        sessionRegistry.registerSession(userId, sessionId);
        presenceService.markOnline(userId);
        messageProducer.publishPresence(new PresenceBroadcastEvent(userId, principal.getName(), true, null));
        log.info("WS CONNECTED: user={} session={}", principal.getName(), sessionId);
    }

    // ─── Desconexión ─────────────────────────────────────────────────────────

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sessionId = event.getSessionId();
        Long userId = extractUserId(principal);
        if (userId == null) return;

        sessionRegistry.removeSession(userId, sessionId);

        if (!sessionRegistry.hasActiveSessions(userId)) {
            presenceService.markOffline(userId);
            String lastSeen = presenceService.getLastSeen(userId);
            messageProducer.publishPresence(
                    new PresenceBroadcastEvent(userId, principal.getName(), false, lastSeen));
            log.info("WS DISCONNECTED (todas las sesiones): user={}", principal.getName());
        } else {
            log.info("WS DISCONNECTED (sesiones restantes): user={} session={}",
                    principal.getName(), sessionId);
        }
    }

    // ─── Enviar mensaje ───────────────────────────────────────────────────────

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WsInboundMessage inbound,
                            SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;
        Long senderId = extractUserId(principal);
        if (senderId == null) return;

        SendMessageRequest req = new SendMessageRequest(
                inbound.conversationId(), inbound.type(), inbound.textContent(),
                inbound.payload(), inbound.payloadType(), inbound.fileUrls()
        );
        MessageResponse saved = messageService.sendMessage(senderId, req);

        // ✅ Obtener receiverUsername desde la conversación (ya desnormalizado)
        String receiverUsername = conversationRepository.findById(saved.conversationId())
                .map(conv -> conv.getUser1Id().equals(senderId)
                        ? conv.getUser2Username()
                        : conv.getUser1Username())
                .orElse(saved.receiverId().toString());

        messageProducer.publishMessage(new MessageEvent(
                saved.messageId(), saved.conversationId(),
                saved.senderId(), principal.getName(),
                saved.receiverId(), receiverUsername,  // ✅ receiverUsername incluido
                saved.type(), saved.textContent(),
                saved.payload(), saved.payloadType(),
                saved.fileUrls(), saved.status(), saved.sentAt(),
                saved.deleted(), saved.editedAt()
        ));
    }

    // ─── Typing indicator ─────────────────────────────────────────────────────

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event,
                       SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;
        Long userId = extractUserId(principal);
        if (userId == null) return;

        if (event.typing()) presenceService.setTyping(event.conversationId(), userId);
        else                presenceService.clearTyping(event.conversationId(), userId);

        messagingTemplate.convertAndSend(
                "/topic/typing." + event.conversationId(),
                new TypingEvent(event.conversationId(), userId, principal.getName(), event.typing())
        );
    }

    // ─── Marcar como leído ────────────────────────────────────────────────────

    @MessageMapping("/chat.read")
    public void markRead(@Payload ReadReceiptEvent event,
                         SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;
        Long userId = extractUserId(principal);
        if (userId == null) return;

        int updated = messageService.markAsRead(event.conversationId(), userId);
        if (updated == 0) return;

        messageProducer.publishReadReceipt(new ReadReceiptBroadcastEvent(
                event.conversationId(), userId, principal.getName(),
                updated, LocalDateTime.now()
        ));
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    @MessageMapping("/heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;
        Long userId = extractUserId(principal);
        if (userId != null) presenceService.heartbeat(userId);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object details = auth.getDetails();
            if (details instanceof Long l)   return l;
            if (details instanceof Number n) return n.longValue();
        }
        return null;
    }
}