package com.openpoker.controller;

import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.Participant;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.service.GameSessionService;
import com.openpoker.service.VoteService;
import com.openpoker.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebSocketController {
    private final GameSessionService service;
    private final VoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;
    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @MessageMapping("/session.join")
    public void join(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = payload.get("inviteCode");
        UUID ticketId = payload.get("ticketId") != null ? UUID.fromString(payload.get("ticketId")) : null;
        try {
            String username = resolveUsername(headerAccessor);

            var session = sessionRepository.findBySessionCode(inviteCode).orElseThrow();
            var user = userRepository.findByUsername(username).orElseThrow();

            var existingParticipant = participantRepository.findByGameSessionAndUser(session, user);
            if (existingParticipant.isEmpty()) {
                service.joinSession(username, inviteCode);
            }

            var participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow();

            sessionRegistry.register(headerAccessor.getSessionId(), session.getId(), participant.getId(), username, inviteCode);

            List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(inviteCode));

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/participants", participants);
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));

            if (ticketId != null) {
                messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(session.getId(), ticketId));
            } else {
                log.info("No se envía vote-status porque no hay un ticketId seleccionado en el payload de unión.");
            }

        } catch (RuntimeException ex) {
            ex.printStackTrace();
            publishError(inviteCode, "session.join", ex);
        }
    }

    @MessageMapping("/session.leave")
    public void leave(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;
        UUID ticketId = payload.get("ticketId") != null ? UUID.fromString(payload.get("ticketId")) : null;
        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionRepository.findById(sessionInfo.sessionId()).orElseThrow().getSessionCode();

            service.leaveSession(sessionInfo.username(), inviteCode);

            sessionRegistry.unregister(headerAccessor.getSessionId());

            List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(inviteCode));
            var session = sessionRepository.findBySessionCode(inviteCode).orElseThrow();

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/participants", participants);
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(session.getId(), ticketId));
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.leave", ex);
        }
    }

    @MessageMapping("/session.vote")
    public void vote(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            String cardValueStr = payload.get("cardValue");
            String ticketIdStr = payload.get("ticketId");

            if (cardValueStr == null || ticketIdStr == null) {
                throw new IllegalArgumentException("Faltan parámetros requeridos ('cardValue' o 'ticketId')");
            }

            UUID cardValue = UUID.fromString(cardValueStr);
            UUID ticketId = UUID.fromString(ticketIdStr);

            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionRepository.findById(sessionInfo.sessionId()).orElseThrow().getSessionCode();

            // 🚀 EJECUCIÓN LIMPIA DIRECTA
            voteService.submitVote(sessionInfo.sessionId(), ticketId, sessionInfo.participantId(), cardValue);
            
            // Notificaciones en tiempo real a la sala
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", voteService.getVotes(sessionInfo.sessionId(), ticketId, sessionInfo.participantId()));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(sessionInfo.sessionId(), ticketId));
            
        } catch (DataIntegrityViolationException ex) {
            // 🛡️ ESCUDO ANTI-CARRERAS: 
            // Si el frontend disparó dos veces el voto simultáneamente, el segundo hilo causará esta excepción de llave única.
            // Como el primer hilo ya guardó el voto con éxito, simplemente lo ignoramos y no alarmamos al usuario.
            log.warn("Voto doble concurrente detectado e ignorado para la sala: {}", inviteCode);
            
        } catch (RuntimeException ex) {
            // Cualquier otro error real se sigue notificando al Frontend
            publishError(inviteCode, "session.vote", ex);
        }
    }
        


    @MessageMapping("/session.reveal")
    public void reveal(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;
        
        try {
            String ticketIdStr = payload.get("ticketId");
            if (ticketIdStr == null || ticketIdStr.isBlank()) {
                throw new IllegalArgumentException("El parámetro 'ticketId' es obligatorio en el payload.");
            }
            UUID ticketId = UUID.fromString(ticketIdStr);

            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionRepository.findById(sessionInfo.sessionId()).orElseThrow().getSessionCode();

            // 🚀 2. CORRECCIÓN DE ORDEN: (sessionId, participantId, ticketId)
            var revealResults = voteService.revealVotes(sessionInfo.sessionId(), sessionInfo.participantId(), ticketId);

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", revealResults);
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(sessionInfo.sessionId(), ticketId));

        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.reveal", ex);
        }
    }

    @MessageMapping("/session.reset-votes")
    public void resetVotes(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            String ticketIdStr = payload.get("ticketId");
            if (ticketIdStr == null || ticketIdStr.trim().isEmpty()) {
                throw new IllegalArgumentException("El parámetro ticketId es requerido y no puede estar vacío");
            }

            UUID ticketId = UUID.fromString(payload.get("ticketId"));

        
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionRepository.findById(sessionInfo.sessionId()).orElseThrow().getSessionCode();

            voteService.resetVotes(sessionInfo.sessionId(), ticketId, sessionInfo.participantId());
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", voteService.getVotes(sessionInfo.sessionId(), ticketId, sessionInfo.participantId()));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(sessionInfo.sessionId(), ticketId));
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.reset-votes", ex);
        }
    }

    @MessageMapping("/session.finish")
    public void finish(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();
            String username = sessionInfo.username();
            Object sessionState = service.finishSession(username, inviteCode);

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/participants", List.of());
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", sessionState);
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.finish", ex);
        }
    }

    private List<WebSocketParticipantResponse> mapParticipants(List<Participant> participants) {
        return participants.stream().map(participant -> new WebSocketParticipantResponse(participant.getUser().getId(), participant.getUser().getUsername(), participant
                .getRole().name())).toList();
    }

    private String resolveUsername(SimpMessageHeaderAccessor headerAccessor) {
        java.security.Principal principal = headerAccessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("No authenticated user on WebSocket session");
        }
        return principal.getName();
    }

    private WebSocketSessionRegistry.SessionInfo getRequiredSessionInfo(SimpMessageHeaderAccessor headerAccessor) {
        String wsSessionId = headerAccessor.getSessionId();

        if (wsSessionId == null || wsSessionId.isBlank()) {
            throw new IllegalStateException("No se pudo identificar la sesion WebSocket");
        }

        return sessionRegistry.get(wsSessionId).orElseThrow(() -> new IllegalStateException("Sesion WebSocket no registrada"));
    }

    private void publishError(String inviteCode, String action, RuntimeException ex) {
        if (inviteCode == null || inviteCode.isBlank()) {
            return;
        }

        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

        messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/errors", (Object) Map.of(
                "action", action,
                "type", ex.getClass().getSimpleName(),
                "message", message
        ));
    }
}
