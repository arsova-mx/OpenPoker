package com.openpoker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Mapea cada sesión WebSocket a los datos del usuario conectado.
 * Coordina la reconexión rápida mediante tareas de limpieza cancelables.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(UUID sessionId, UUID participantId, String username, String inviteCode) {}

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    // Tareas pendientes de limpieza por participante (clave: inviteCode + ":" + participantId)
    private final Map<String, ScheduledFuture<?>> pendingCleanups = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Object lock = new Object();

    // Tiempo de gracia en segundos para tolerar microcortes y reconexiones automáticas
    private static final long GRACE_PERIOD_SECONDS = 10;

    public void register(String wsSessionId, UUID sessionId, UUID participantId, String username, String inviteCode) {
        synchronized (lock) {
            String participantKey = inviteCode + ":" + participantId;
            
            // Si había una limpieza programada porque el socket anterior cayó, se cancela de inmediato
            ScheduledFuture<?> pendingTask = pendingCleanups.remove(participantKey);
            if (pendingTask != null) {
                pendingTask.cancel(false);
                log.info("Reconexión detectada para participantId={}. Tarea de limpieza cancelada.", participantId);
            }

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
     * Programa la limpieza con margen de espera. Si el cliente vuelve a conectar en ese lapso,
     * la tarea se aborta evitando borrar participantes o destruir salas activas.
     */
    public void scheduleCleanupIfLast(String wsSessionId, Consumer<SessionInfo> cleanupAction) {
        synchronized (lock) {
            SessionInfo removedInfo = sessions.remove(wsSessionId);
            if (removedInfo == null) return;

            UUID participantId = removedInfo.participantId();
            String inviteCode = removedInfo.inviteCode();
            String participantKey = inviteCode + ":" + participantId;

            // Verificamos si aún tiene otros sockets (otra pestaña abierta)
            boolean hasOthers = sessions.values().stream()
                    .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

            if (hasOthers) {
                log.info("El participante {} aún tiene otras pestañas abiertas. No se programa limpieza.", removedInfo.username());
                return;
            }

            log.info("Último socket cerrado para {}. Programando limpieza en {}s...", removedInfo.username(), GRACE_PERIOD_SECONDS);

            ScheduledFuture<?> task = scheduler.schedule(() -> {
                synchronized (lock) {
                    pendingCleanups.remove(participantKey);
                    // Verificación final bajo lock antes de ejecutar la acción destructiva
                    boolean reconnected = sessions.values().stream()
                            .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

                    if (!reconnected) {
                        cleanupAction.accept(removedInfo);
                    }
                }
            }, GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);

            pendingCleanups.put(participantKey, task);
        }
    }
}