package com.example.backend.module.messagemanagement.realtime.session;

public interface IWebSocketSessionRegistry {
    void registerSession(Long userId, String sessionId);
    void removeSession(Long userId, String sessionId);
    boolean hasActiveSessions(Long userId);
    long countSessions(Long userId);
    Long getUserIdBySession(String sessionId);
}