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
            // 1. Invalidamos cualquier generación anterior
            participantGenerations.computeIfAbsent(participantKey, k -> new AtomicLong(0)).incrementAndGet();

            // 2. Si había una limpieza programada en el scheduler, se aborta de inmediato
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
     * Valida atómicamente si la generación de desconexión aún es válida y no hay sockets activos.
     * Si sigue siendo válida, consume la generación definitivamente.
     */
    public boolean validateAndConsumeGeneration(String inviteCode, UUID participantId, long expectedGeneration) {
        String participantKey = inviteCode + ":" + participantId;
        synchronized (lock) {
            AtomicLong gen = participantGenerations.get(participantKey);
            if (gen == null || gen.get() != expectedGeneration) {
                return false;
            }

            boolean hasActiveSockets = sessions.values().stream()
                    .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

            if (hasActiveSockets) {
                return false;
            }

            // Consumir la generación atómicamente: la acción destructiva tiene luz verde definitiva
            participantGenerations.remove(participantKey);
            return true;
        }
    }

    /**
     * Consulta si la generación actual sigue intacta (para chequeos defensivos previos).
     */
    public boolean isGenerationActive(String inviteCode, UUID participantId, long expectedGeneration) {
        String participantKey = inviteCode + ":" + participantId;
        synchronized (lock) {
            AtomicLong gen = participantGenerations.get(participantKey);
            if (gen == null || gen.get() != expectedGeneration) {
                return false;
            }
            return sessions.values().stream()
                    .noneMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));
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
                // 1. Verificación preliminar al expirar el tiempo de gracia (SIN borrar la generación)
                boolean stillValid;
                synchronized (lock) {
                    pendingCleanups.remove(participantKey);

                    long currentGeneration = participantGenerations
                            .getOrDefault(participantKey, new AtomicLong(-1))
                            .get();

                    boolean hasSockets = sessions.values().stream()
                            .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

                    stillValid = (currentGeneration == expectedGeneration && !hasSockets);
                }

                if (!stillValid) {
                    log.info("Limpieza descartada por reconexión o generación obsoleta para participantId={}", participantId);
                    return;
                }

                // 2. Encolar en el executor para no bloquear el scheduler
                cleanupExecutor.submit(() -> {
                    // 3. Revalidación atómica en el límite de la mutación destructiva
                    // Si el usuario se reconectó mientras esperaba en la cola del pool,
                    // validateAndConsumeGeneration devolverá false.
                    boolean proceed = validateAndConsumeGeneration(inviteCode, participantId, expectedGeneration);

                    if (!proceed) {
                        log.info("Reconexión detectada justo antes de la mutación en DB para {}. Abortando limpieza.",
                                removedInfo.username());
                        return;
                    }

                    try {
                        cleanupAction.accept(removedInfo);
                    } catch (Exception ex) {
                        log.error("Error durante la ejecución de cleanupAction para {}", removedInfo.username(), ex);
                    }
                });

            }, executionTime);

            pendingCleanups.put(participantKey, task);
        }
    }
}