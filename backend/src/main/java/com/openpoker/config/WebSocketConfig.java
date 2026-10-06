package com.openpoker.config;

import com.openpoker.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic: broadcast a la sala. /queue: mensajes privados por conexión (vía /user/queue/...)
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app"); // cliente -> servidor
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Mismos orígenes que CORS. Con la lista vacía solo se acepta el mismo origen.
        String[] allowedOrigins = corsProperties.allowedOrigins().toArray(String[]::new);
        registry.addEndpoint("/ws-native").setAllowedOriginPatterns(allowedOrigins);
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins).withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
