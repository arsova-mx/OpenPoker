package com.openpoker.config;

import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.service.GameSessionService;
import com.openpoker.service.VoteService;
import com.openpoker.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketDisconnectListener {

    private final WebSocketSessionRegistry registry;
    private final GameSessionService gameSessionService;
    private final VoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String wsSessionId = event.getSessionId();

        registry.unregister(wsSessionId).ifPresent(info -> {
            log.info("WebSocket disconnected: participant={}, inviteCode={}, reason={}", 
                    info.username(), info.inviteCode(), event.getCloseStatus());

            try {
                UUID sessionId = info.sessionId();

                // 1. Ejecutamos la desconexión (si era HOST, elimina la sesión)
                gameSessionService.handleDisconnect(info.participantId(), info.inviteCode());

                // 2. Intentamos notificar el nuevo estado a los participantes restantes
                try {
                    SessionResponse currentSession = gameSessionService.getSessionByCode(info.inviteCode());

                    List<WebSocketParticipantResponse> participants = gameSessionService.getParticipants(info.inviteCode())
                        .stream()
                        .map(p -> new WebSocketParticipantResponse(
                                p.getId(),
                                p.getEffectiveName(),
                                p.getRole() != null ? p.getRole().name() : "",
                                p.getUser() == null
                        ))
                        .toList();

                    messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/participants", participants);
                    messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/state", currentSession);
                    
                    UUID activeTicketId = null;
                    messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/vote-status", 
                        voteService.getVoteStatus(sessionId, activeTicketId));

                } catch (SessionNotFoundException ex) {
                    // 🚀 CASO HOST DESCONECTADO: Si la sesión fue eliminada, notificamos a todos los clientes que la sala se cerró
                    log.info("La sesión {} fue eliminada por desconexión del HOST. Notificando cierre...", info.inviteCode());
                    
                    messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/participants", List.of());
                    messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/state", (Object) Map.of("status", "FINISHED", "message", "El Host ha cerrado la sesión")); // 👈 Casteo explícito a (Object));
                }

            } catch (Exception e) {
                log.warn("Error cleaning up after disconnect: participantId={}, error={}",
                        info.participantId(), e.getMessage());
            }
        });
    }
}