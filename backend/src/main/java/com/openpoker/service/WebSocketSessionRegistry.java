package com.openpoker.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapea cada sesión WebSocket (sessionId de STOMP) a los datos del usuario conectado,
 * para poder identificar quién se desconectó cuando se pierde la conexión.
 */
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(String username, String inviteCode) {}

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public void register(String wsSessionId, String username, String inviteCode) {
        sessions.put(wsSessionId, new SessionInfo(username, inviteCode));
    }

    public Optional<SessionInfo> unregister(String wsSessionId) {
        return Optional.ofNullable(sessions.remove(wsSessionId));
    }

    public Optional<SessionInfo> get(String wsSessionId) {
        return Optional.ofNullable(sessions.get(wsSessionId));
    }
}
