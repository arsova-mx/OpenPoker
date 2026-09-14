package com.openpoker.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapea cada sesión WebSocket (sessionId de STOMP) a los datos del usuario conectado,
 * permitiendo rastrear múltiples sockets por participante.
 */
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(UUID sessionId, UUID participantId, String username, String inviteCode) {}

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public void register(String wsSessionId, UUID sessionId, UUID participantId, String username, String inviteCode) {
        sessions.put(wsSessionId, new SessionInfo(sessionId, participantId, username, inviteCode));
    }

    public Optional<SessionInfo> unregister(String wsSessionId) {
        return Optional.ofNullable(sessions.remove(wsSessionId));
    }

    public Optional<SessionInfo> get(String wsSessionId) {
        return Optional.ofNullable(sessions.get(wsSessionId));
    }

    /**
     * Verifica si existen otras conexiones WebSocket activas para el mismo participante en la misma sala.
     * Esto evita borrar el registro de Participant cuando se cierra una segunda pestaña o durante una reconexión rápida.
     */
    public boolean hasOtherConnectionsForParticipant(UUID participantId, String inviteCode) {
        if (participantId == null || inviteCode == null) {
            return false;
        }
        return sessions.values().stream()
                .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));
    }
}