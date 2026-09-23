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

/**
 * Mapea cada sesión WebSocket a los datos del usuario conectado.
 * Coordina la reconexión rápida mediante generaciones atómicas sin bloquear
 * el registro ante limpiezas lentas de base de datos.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(UUID sessionId, UUID participantId, String username, String inviteCode) {}

    // Registro de sockets activos
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    // Tareas pendientes por participante
    private final Map<String, ScheduledFuture<?>> pendingCleanups = new ConcurrentHashMap<>();

    // Generador de versión por participante para invalidar limpiezas obsoletas sin lock
    private final Map<String, AtomicLong> participantGenerations = new ConcurrentHashMap<>();

    // Componentes administrados por Spring e inyectados desde AsyncConfig
    private final TaskScheduler scheduler;
    private final AsyncTaskExecutor cleanupExecutor;

    private final Object lock = new Object();

    // Tiempo de gracia para reconexiones automáticas
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

            // 2. Si había una limpieza programada, se aborta de inmediato
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
     * Programa la limpieza con margen de espera. La decisión y el estado se toman bajo lock,
     * pero la ejecución destructiva pesada (DB / STOMP) corre en el pool acotado fuera del monitor.
     */
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

            // Verificamos si aún tiene otros sockets (otra pestaña abierta)
            boolean hasOthers = sessions.values().stream()
                    .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

            if (hasOthers) {
                log.info("El participante {} aún tiene otros sockets activos. Omitiendo limpieza.", removedInfo.username());
                return;
            }

            // Marcamos una nueva generación esperada para este ciclo de desconexión
            expectedGeneration = participantGenerations
                    .computeIfAbsent(participantKey, k -> new AtomicLong(0))
                    .incrementAndGet();

            log.info("Último socket cerrado para {}. Programando limpieza (gen={}) en {}s...",
                    removedInfo.username(), expectedGeneration, GRACE_PERIOD_SECONDS);

            Instant executionTime = Instant.now().plusSeconds(GRACE_PERIOD_SECONDS);
            ScheduledFuture<?> task = scheduler.schedule(() -> {
                boolean proceedWithCleanup = false;

                // 1. Verificación preliminar al vencer el temporizador
                synchronized (lock) {
                    pendingCleanups.remove(participantKey);

                    long currentGeneration = participantGenerations
                            .getOrDefault(participantKey, new AtomicLong(-1))
                            .get();

                    if (currentGeneration == expectedGeneration) {
                        boolean reconnected = sessions.values().stream()
                                .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

                        if (!reconnected) {
                            proceedWithCleanup = true;
                            // OJO: No removemos participantGenerations aquí para permitir
                            // que si ocurre un register() mientras la tarea hace cola,
                            // se incremente y se detecte en el worker.
                        }
                    } else {
                        log.info("Limpieza descartada por generación obsoleta para participantId={}", participantId);
                    }
                }

                if (proceedWithCleanup) {
                    cleanupExecutor.submit(() -> {
                        boolean canExecuteDestructiveAction = false;

                        // 2. Doble verificación inmediata antes de la mutación destructiva
                        synchronized (lock) {
                            long finalGeneration = participantGenerations
                                    .getOrDefault(participantKey, new AtomicLong(-1))
                                    .get();

                            boolean hasActiveSockets = sessions.values().stream()
                                    .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

                            // Si nadie se reconectó en la cola del pool y sigue la misma generación
                            if (finalGeneration == expectedGeneration && !hasActiveSockets) {
                                canExecuteDestructiveAction = true;
                                participantGenerations.remove(participantKey);
                            } else {
                                log.info("Reconexión detectada justo antes de ejecutar la limpieza para {}. Abortando.", removedInfo.username());
                            }
                        }

                        // 3. Ejecución I/O pesada (DB, broadcast STOMP, etc.) fuera de todo lock
                        if (canExecuteDestructiveAction) {
                            try {
                                cleanupAction.accept(removedInfo);
                            } catch (Exception ex) {
                                log.error("Error durante la ejecución de cleanupAction para {}", removedInfo.username(), ex);
                            }
                        }
                    });
                }
            }, executionTime);

            pendingCleanups.put(participantKey, task);
        }
    }
}