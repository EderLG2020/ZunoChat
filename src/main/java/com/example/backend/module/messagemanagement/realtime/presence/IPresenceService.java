package com.example.backend.module.messagemanagement.realtime.presence;

public interface IPresenceService {
    void markOnline(Long userId);
    void heartbeat(Long userId);
    void markOffline(Long userId);
    boolean isOnline(Long userId);
    String getLastSeen(Long userId);
    void setTyping(Long conversationId, Long userId);
    void clearTyping(Long conversationId, Long userId);
    boolean isTyping(Long conversationId, Long userId);
}