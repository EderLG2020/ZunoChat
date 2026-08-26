package com.example.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pool acotado para tareas @Async (hoy: envío de correos vía Brevo).
 *
 * Sin esto, @EnableAsync usa SimpleAsyncTaskExecutor por defecto, que crea
 * un hilo nuevo por cada llamada sin límite — un pico de registros/OTPs
 * podría abrir cientos de hilos concurrentes hacia Brevo. Con un pool
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
}
