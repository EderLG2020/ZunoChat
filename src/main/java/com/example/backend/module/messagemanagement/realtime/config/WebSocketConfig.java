package com.example.backend.module.messagemanagement.realtime.config;

import com.example.backend.module.messagemanagement.realtime.handshake.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Autowired
    private StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Value("${websocket.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Endpoint de conexión WebSocket.
     * Clientes conectan a: ws://host/ws?token=<jwt>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();   // fallback para navegadores sin soporte nativo WS
    }

    /**
     * Configuración del broker de mensajes STOMP.
     *
     * /topic  → broadcast (1 → muchos) — conversaciones, presencia
     * /queue  → punto a punto (1 → 1)  — notificaciones personales
     * /app    → prefijo de destinos manejados por @MessageMapping
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Interceptor en el canal de entrada para autenticar con JWT
     * en el frame CONNECT de STOMP (header: Authorization).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}