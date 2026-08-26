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

    // ─── Broker relay externo (opcional) ────────────────────────────────────
    //
    // Por defecto (app.websocket.relay.enabled=false) se usa el broker
    // simple en memoria de Spring: funciona perfecto con una sola instancia
    // del backend, pero con más de una, un mensaje solo llega a los clientes
    // conectados a esa misma instancia. Activando el relay, todas las
    // instancias comparten el mismo fan-out a través de RabbitMQ (plugin
    // STOMP) — ver docker-compose.yml → servicio "rabbitmq".
    @Value("${app.websocket.relay.enabled:false}")
    private boolean relayEnabled;

    @Value("${app.websocket.relay.host:localhost}")
    private String relayHost;

    @Value("${app.websocket.relay.port:61613}")
    private int relayPort;

    @Value("${app.websocket.relay.login:rabbit}")
    private String relayLogin;

    @Value("${app.websocket.relay.passcode:rabbit123}")
    private String relayPasscode;

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
        if (relayEnabled) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayLogin)
                    .setClientPasscode(relayPasscode)
                    .setSystemLogin(relayLogin)
                    .setSystemPasscode(relayPasscode);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
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