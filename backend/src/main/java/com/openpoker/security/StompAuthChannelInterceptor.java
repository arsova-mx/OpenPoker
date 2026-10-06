package com.openpoker.security;

import com.openpoker.service.SessionAccessService;
import com.openpoker.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autenticación y autorización de los frames STOMP entrantes.
 *
 * - CONNECT: valida el JWT de usuario o el token de invitado (opcional: sin token la conexión es
 *   anónima) y guarda su expiración. Un invitado queda limitado a la sala de su token.
 * - SEND: los clientes solo pueden publicar en /app/**. Publicar directo en /topic/** permitiría
 *   falsificar eventos de una sala (reveals, estado, timer) que reciben todos sus participantes.
 * - SUBSCRIBE: /topic/session/{code}/** solo para quien participa en esa sala;
 *   /user/queue/** (mensajes privados de la propia conexión) siempre.
 * - SEND y SUBSCRIBE se rechazan si el token con el que se conectó ya expiró.
 *
 * Cualquier rechazo lanza AccessDeniedException: Spring responde con un frame ERROR y cierra la conexión.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    static final String TOKEN_EXPIRES_AT = "openpoker.tokenExpiresAt";
    /** Atributos de la conexión de un invitado autenticado con su token (ver POST /api/sessions/{code}/guests). */
    public static final String GUEST_PARTICIPANT_ID = "openpoker.guestParticipantId";
    public static final String GUEST_SESSION_CODE = "openpoker.guestSessionCode";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern SESSION_TOPIC = Pattern.compile("^/topic/session/([^/]+)/.+$");

    private final JwtService jwtService;
    private final SessionAccessService sessionAccessService;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            ensureTokenNotExpired(accessor);
            authorizeSend(accessor.getDestination());
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            ensureTokenNotExpired(accessor);
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        // Sin token la conexión sigue como anónima (invitados); no podrá suscribirse a salas ajenas.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (jwtService.validateToken(token)) {
            String username = jwtService.extractUsername(token);
            accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
            rememberExpiration(accessor, jwtService.extractExpiration(token));
            return;
        }

        // Token de invitado: identifica a un participante invitado dentro de una sola sala
        Optional<JwtService.GuestClaims> guest = jwtService.parseGuestToken(token);
        if (guest.isPresent()) {
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (attributes != null) {
                attributes.put(GUEST_PARTICIPANT_ID, guest.get().participantId());
                attributes.put(GUEST_SESSION_CODE, guest.get().sessionCode());
            }
            rememberExpiration(accessor, guest.get().expiresAt());
            return;
        }

        // Si mandó un token pero es inválido o expiró, se rechaza la conexión
        throw new AccessDeniedException("Token inválido o expirado");
    }

    private void authorizeSend(String destination) {
        if (destination == null || !destination.startsWith("/app/")) {
            throw new AccessDeniedException("Solo se permite publicar en destinos /app/**");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null) {
            throw new AccessDeniedException("Suscripción sin destino");
        }

        // Cola privada de la propia conexión (errores, tokens, etc.)
        if (destination.startsWith("/user/queue/")) {
            return;
        }

        Matcher matcher = SESSION_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("Destino de suscripción no permitido");
        }

        String sessionCode = matcher.group(1);
        if (sessionRegistry.isJoined(accessor.getSessionId(), sessionCode)) {
            return;
        }

        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null && sessionCode.equals(attributes.get(GUEST_SESSION_CODE))) {
            return;
        }

        Principal user = accessor.getUser();
        if (user != null && sessionAccessService.isParticipant(sessionCode, user.getName())) {
            return;
        }

        throw new AccessDeniedException("No participas en esta sesión");
    }

    private void ensureTokenNotExpired(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return;
        }
        Object expiresAt = attributes.get(TOKEN_EXPIRES_AT);
        if (expiresAt instanceof Instant instant && Instant.now().isAfter(instant)) {
            throw new AccessDeniedException("La sesión expiró, vuelve a iniciar sesión");
        }
    }

    private void rememberExpiration(StompHeaderAccessor accessor, Instant expiresAt) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null && expiresAt != null) {
            attributes.put(TOKEN_EXPIRES_AT, expiresAt);
        }
    }
}
