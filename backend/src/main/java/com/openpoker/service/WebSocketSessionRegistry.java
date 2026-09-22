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

            // Programamos el timer a través del TaskScheduler de Spring usando Instant
            Instant executionTime = Instant.now().plusSeconds(GRACE_PERIOD_SECONDS);
            ScheduledFuture<?> task = scheduler.schedule(() -> {
                boolean proceedWithCleanup = false;

                // Transición atómica mínima en memoria
                synchronized (lock) {
                    pendingCleanups.remove(participantKey);

                    long currentGeneration = participantGenerations
                            .getOrDefault(participantKey, new AtomicLong(-1))
                            .get();

                    // Si la generación cambió (el usuario volvió a hacer register()), abortamos
                    if (currentGeneration == expectedGeneration) {
                        boolean reconnected = sessions.values().stream()
                                .anyMatch(info -> inviteCode.equals(info.inviteCode()) && participantId.equals(info.participantId()));

                        if (!reconnected) {
                            proceedWithCleanup = true;
                            participantGenerations.remove(participantKey);
                        }
                    } else {
                        log.info("Limpieza descartada por generación obsoleta para participantId={}", participantId);
                    }
                }

                // El I/O pesado corre en el pool acotado de Spring con control de backpressure
                if (proceedWithCleanup) {
                    cleanupExecutor.submit(() -> {
                        try {
                            cleanupAction.accept(removedInfo);
                        } catch (Exception ex) {
                            log.error("Error durante la ejecución de cleanupAction para {}", removedInfo.username(), ex);
                        }
                    });
                }
            }, executionTime);

            pendingCleanups.put(participantKey, task);
        }
    }
}