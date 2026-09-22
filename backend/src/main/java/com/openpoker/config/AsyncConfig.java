package com.openpoker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Pool acotado para tareas de I/O pesado (Base de Datos, avisos STOMP, limpiezas).
     * Evita el desbordamiento de hilos y conexiones ante desconexiones masivas.
     */
    @Bean(name = "wsCleanupExecutor")
    public ThreadPoolTaskExecutor wsCleanupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        // Límite superior estricto para no agotar conexiones de HikariCP/BD
        executor.setMaxPoolSize(Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
        executor.setQueueCapacity(500); 
        executor.setThreadNamePrefix("ws-cleanup-");
        
        // Backpressure: Si la cola de 500 se satura, el hilo invocador ejecuta la tarea,
        // frenando el ingreso de nuevas tareas y protegiendo la memoria.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Apagado ordenado gestionado por Spring
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        
        executor.initialize();
        return executor;
    }

    /**
     * Scheduler gestionado por Spring para contar el tiempo de gracia (grace period).
     * Reemplaza al Executors.newScheduledThreadPool manual en el servicio.
     */
    @Bean(name = "wsCleanupScheduler")
    public ThreadPoolTaskScheduler wsCleanupScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // Con 2 a 4 hilos es suficiente, ya que solo cuenta segundos y delega el I/O al executor
        scheduler.setPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors()));
        scheduler.setThreadNamePrefix("ws-timer-");
        
        // Ciclo de vida y shutdown gestionado por el contenedor de Spring
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.setRemoveOnCancelPolicy(true); // Purga de inmediato tareas canceladas de la cola interna
        
        scheduler.initialize();
        return scheduler;
    }
}