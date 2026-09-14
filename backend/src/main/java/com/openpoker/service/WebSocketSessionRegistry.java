package com.openpoker.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Mapea cada sesión WebSocket a los datos del usuario conectado de forma atómica.
 */
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(UUID sessionId, UUID participantId, String username, String inviteCode) {}

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public void register(String wsSessionId, UUID sessionId, UUID participantId, String username, String inviteCode) {
        synchronized (lock) {
            sessions.put(wsSessionId, new SessionInfo(sessionId, participantId, username, inviteCode));
        }
    }

    public Optional<SessionInfo> unregister(String wsSessionId) {
        synchronized (lock) {
            return Optional.ofNullable(sessions.remove(wsSessionId));
        }
    }

    public Optional<SessionInfo> get(String wsSessionId) {
        return Optional.ofNullable(sessions.get(wsSessionId));
    }

    /**
     * Desregistra el socket y, de forma estrictamente atómica, ejecuta la limpieza
     * únicamente si no existen otras conexiones activas para el mismo participante en esa sala.
     *
     * @param wsSessionId ID del socket cerrado
     * @param cleanupAction Acción a ejecutar pasando el SessionInfo si ya no quedan sockets
     */
    public void unregisterAndCleanupIfLast(String wsSessionId, Consumer<SessionInfo> cleanupAction) {
        SessionInfo removedInfo;
        boolean shouldCleanup = false;

        synchronized (lock) {
            removedInfo = sessions.remove(wsSessionId);
            if (removedInfo != null) {
                UUID participantId = removedInfo.participantId();
                String inviteCode = removedInfo.inviteCode();

                boolean hasOthers = sessions.values().stream()
                        .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

                if (!hasOthers) {
                    shouldCleanup = true;
                }
            }
        }

        // Si fue la última conexión y nadie se conectó en medio, se dispara la limpieza de base de datos
        if (shouldCleanup && removedInfo != null) {
            cleanupAction.accept(removedInfo);
        }
    }
}