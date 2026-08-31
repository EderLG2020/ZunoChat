package com.example.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pool acotado para tareas @Async (hoy: envío de correos vía Resend).
 *
 * Sin esto, @EnableAsync usa SimpleAsyncTaskExecutor por defecto, que crea
 * un hilo nuevo por cada llamada sin límite — un pico de registros/OTPs
 * podría abrir cientos de hilos concurrentes hacia Resend. Con un pool
 * acotado, el exceso simplemente espera en cola en vez de agotar recursos.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("email-async-");
        executor.initialize();
        return executor;
    }

    /**
     * Pool para el fan-out de MessageProducer (WS). Con el broker simple
     * (default) el fan-out ya era rápido al ser todo en memoria del mismo
     * JVM, pero corría igual dentro del hilo HTTP que atendió el POST; con
     * app.websocket.relay.enabled=true (RabbitMQ) cada convertAndSend pasa a
     * depender de I/O de red sobre esa conexión — sin este pool, ese I/O
     * bloqueaba un hilo de Tomcat hasta que el broker confirmaba el envío.
     */
    @Bean(name = "wsBroadcastExecutor")
    public Executor wsBroadcastExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ws-broadcast-");
        executor.initialize();
        return executor;
    }
}
