package com.openpoker.security;

import com.openpoker.service.SessionAccessService;
import com.openpoker.service.WebSocketSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StompAuthChannelInterceptorTest {

    private StompAuthChannelInterceptor interceptor;
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(
                mock(JwtService.class),
                mock(SessionAccessService.class),
                mock(WebSocketSessionRegistry.class));
    }

    private Message<byte[]> frame(StompCommand command, String destination, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setSessionId("ws-1");
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("SEND a /app con token vigente pasa")
    void sendToAppIsAllowed() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(StompAuthChannelInterceptor.TOKEN_EXPIRES_AT, Instant.now().plusSeconds(60));

        assertNotNull(interceptor.preSend(frame(StompCommand.SEND, "/app/session.vote", attributes), channel));
    }

    @Test
    @DisplayName("SEND fuera de /app se rechaza")
    void sendOutsideAppIsRejected() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, "/topic/session/ABC123/votes", new HashMap<>()), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, "/queue/errors", new HashMap<>()), channel));
    }

    @Test
    @DisplayName("Con el token expirado se rechazan SEND y SUBSCRIBE")
    void expiredTokenIsRejected() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(StompAuthChannelInterceptor.TOKEN_EXPIRES_AT, Instant.now().minusSeconds(1));

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, "/app/session.vote", attributes), channel));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/errors", attributes), channel));
    }

    @Test
    @DisplayName("Suscribirse a destinos fuera de las salas o la cola privada se rechaza")
    void unknownSubscriptionIsRejected() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/anything", new HashMap<>()), channel));
    }
}
