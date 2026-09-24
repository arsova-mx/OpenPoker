package com.openpoker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(UUID sessionId, UUID participantId, String username, String inviteCode) {}

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingCleanups = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> participantGenerations = new ConcurrentHashMap<>();

    private final TaskScheduler scheduler;
    private final AsyncTaskExecutor cleanupExecutor;
    private final Object lock = new Object();

    private static final long GRACE_PERIOD_SECONDS = 10;

    public WebSocketSessionRegistry(
            @Qualifier("wsCleanupScheduler") TaskScheduler scheduler,
            @Qualifier("wsCleanupExecutor") AsyncTaskExecutor cleanupExecutor) {
        this.scheduler = scheduler;
        this.cleanupExecutor = cleanupExecutor;
    }

    public void register(String wsSessionId, UUID sessionId, UUID participantId, String username, String inviteCode) {
        String participantKey = inviteCode + ":" + participantId;

        synchronized (lock) {
            // Invalida cualquier generación previa incrementando la versión
            participantGenerations.computeIfAbsent(participantKey, k -> new AtomicLong(0)).incrementAndGet();

            ScheduledFuture<?> pendingTask = pendingCleanups.remove(participantKey);
            if (pendingTask != null) {
                pendingTask.cancel(false);
                log.info("Reconexión detectada para participantId={}. Tarea de limpieza abortada.", participantId);
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
     * Ejecuta la mutación destructiva de forma atómica respecto al registro de nuevos sockets.
     * Si ocurre una reconexión concurrente, cleanupAction no se ejecuta.
     */
    public boolean executeIfStillDisconnected(String inviteCode, UUID participantId, long expectedGeneration, Consumer<SessionInfo> cleanupAction, SessionInfo info) {
        String participantKey = inviteCode + ":" + participantId;

        synchronized (lock) {
            AtomicLong gen = participantGenerations.get(participantKey);
            if (gen == null || gen.get() != expectedGeneration) {
                return false;
            }

            boolean hasActiveSockets = sessions.values().stream()
                    .anyMatch(s -> inviteCode.equals(s.inviteCode()) && participantId.equals(s.participantId()));

            if (hasActiveSockets) {
                return false;
            }

            try {
                // Se ejecuta la mutación antes de remover el estado
                cleanupAction.accept(info);
            } finally {
                // Solo se limpia la entrada si la generación sigue siendo la esperada
                if (gen.get() == expectedGeneration) {
                    participantGenerations.remove(participantKey);
                }
            }
            return true;
        }
    }

    public void scheduleCleanupIfLast(String wsSessionId, Consumer<SessionInfo> cleanupAction) {
        SessionInfo removedInfo;
        String participantKey;
        long expectedGeneration;

        synchronized (lock) {
            removedInfo = sessions.remove(wsSessionId);
            if (removedInfo == null) return;

            UUID participantId = removedInfo.participantId();
            String inviteCode = removedInfo.inviteCode();
            participantKey = inviteCode + ":" + participantId;

            boolean hasOthers = sessions.values().stream()
                    .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

            if (hasOthers) {
                log.info("El participante {} aún tiene otros sockets activos. Omitiendo limpieza.", removedInfo.username());
                return;
            }

            expectedGeneration = participantGenerations
                    .computeIfAbsent(participantKey, k -> new AtomicLong(0))
                    .incrementAndGet();

            log.info("Último socket cerrado para {}. Programando limpieza (gen={}) en {}s...",
                    removedInfo.username(), expectedGeneration, GRACE_PERIOD_SECONDS);

            Instant executionTime = Instant.now().plusSeconds(GRACE_PERIOD_SECONDS);
            ScheduledFuture<?> task = scheduler.schedule(() -> {
                synchronized (lock) {
                    pendingCleanups.remove(participantKey);
                }

                // Encolar al executor asíncrono
                cleanupExecutor.submit(() -> {
                    boolean cleaned = executeIfStillDisconnected(
                            inviteCode, 
                            participantId, 
                            expectedGeneration, 
                            cleanupAction, 
                            removedInfo
                    );

                    if (!cleaned) {
                        log.info("Reconexión detectada en el límite de mutación para {}. Limpieza abortada.", removedInfo.username());
                    }
                });

            }, executionTime);

            pendingCleanups.put(participantKey, task);
        }
    }
}