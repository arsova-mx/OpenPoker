package com.openpoker.config;

import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.WebSocketParticipantResponse;
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
            log.info("WebSocket disconnected: user={}, inviteCode={}, reason={}", info.username(), info.inviteCode(), event.getCloseStatus());

            try {
                UUID sessionId = info.sessionId();
                gameSessionService.handleDisconnect(info.username(), info.inviteCode());

                SessionResponse currentSession = gameSessionService.getSessionByCode(info.inviteCode());

                List<WebSocketParticipantResponse> participants = gameSessionService.getParticipants(info.inviteCode())
                    .stream()
                    .map(p -> new WebSocketParticipantResponse(
                            p.getId(),               // 👈 En lugar de p.getUser().getId()
                            p.getEffectiveName(),   // 👈 En lugar de p.getUser().getUsername()
                            p.getRole() != null ? p.getRole().name() : "",
                            p.getUser() == null      // 👈 isGuest (si actualizaste el DTO a 4 campos)
                    ))
                    .toList();

                messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/participants", participants);
                messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/state", currentSession);
                UUID activeTicketId = null;
                messagingTemplate.convertAndSend("/topic/session/" + info.inviteCode() + "/vote-status", 
                    voteService.getVoteStatus(sessionId, activeTicketId));
            } catch (Exception e) {
                log.warn("Error cleaning up after disconnect: user={}, error={}",
                        info.username(), e.getMessage());
            }
        });
    }
}
