package com.openpoker.controller;

import com.openpoker.dto.JoinSessionRequest;
import com.openpoker.dto.SetTimerRequest;
import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.dto.TimerStatusDTO;
import com.openpoker.dto.VotingRRAverage;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.StompAuthChannelInterceptor;
import com.openpoker.service.GameSessionService;
import com.openpoker.service.TicketService;
import com.openpoker.service.TicketTimerService;
import com.openpoker.service.VoteService;
import com.openpoker.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
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
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final TicketTimerService ticketTimerService;

    @MessageMapping("/session.join")
    public void join(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = payload.get("inviteCode");
        UUID ticketId = payload.get("ticketId") != null ? UUID.fromString(payload.get("ticketId")) : null;
        try {
            String username = resolveUsernameOrNull(headerAccessor);
            var session = sessionRepository.findBySessionCode(inviteCode)
                    .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));
            
            Participant participant;

            if (username != null) {
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
                var existingParticipant = participantRepository.findByGameSessionAndUser(session, user);

                if (existingParticipant.isPresent()) {
                    participant = existingParticipant.get();
                } else {
                    JoinSessionRequest joinRequest = new JoinSessionRequest(inviteCode, username, null);
                    service.joinSession(joinRequest);
                    participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow();
                }
            } else {
                // Invitado: se identifica por el token emitido en POST /api/sessions/{code}/guests,
                // nunca por el nombre (antes cualquiera con el nombre de un invitado tomaba su lugar).
                participant = resolveGuestParticipant(headerAccessor, session);
            }

            sessionRegistry.register(
                headerAccessor.getSessionId(), 
                session.getId(), 
                participant.getId(), 
                participant.getEffectiveName(),
                inviteCode
            );

            List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(inviteCode));

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/participants", participants);
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));

            if (ticketId != null) {
                messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(session.getId(), ticketId));
            } else {
                log.info("No se envía vote-status porque no hay un ticketId seleccionado en el payload de unión.");
            }

        } catch (RuntimeException ex) {
            publishError(headerAccessor, "session.join", ex);
        }
    }

    @MessageMapping("/session.leave")
    public void leave(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;
        UUID ticketId = payload.get("ticketId") != null ? UUID.fromString(payload.get("ticketId")) : null;
        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionRepository.findById(sessionInfo.sessionId()).orElseThrow().getSessionCode();

            service.leaveSession(sessionInfo.participantId(), inviteCode);

            sessionRegistry.unregister(headerAccessor.getSessionId());

            List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(inviteCode));

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/participants", participants);
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(sessionInfo.sessionId(), ticketId));
        } catch (RuntimeException ex) {
            publishError(headerAccessor, "session.leave", ex);
        }
    }

    @MessageMapping("/session.vote")
    public void vote(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = payload.get("inviteCode");

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

            voteService.submitVote(sessionInfo.sessionId(), ticketId, sessionInfo.participantId(), cardValue);
            
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(sessionInfo.sessionId(), ticketId));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            
        } catch (DataIntegrityViolationException ex) {
            log.warn("Voto doble concurrente detectado e ignorado para la sala: {}", inviteCode);
        } catch (RuntimeException ex) {
            publishError(headerAccessor, "session.vote", ex);
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

            var revealResults = voteService.revealVotes(sessionInfo.sessionId(), sessionInfo.participantId(), ticketId);

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", revealResults);
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", voteService.getVoteStatus(sessionInfo.sessionId(), ticketId));

        } catch (RuntimeException ex) {
            publishError(headerAccessor, "session.reveal", ex);
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

            UUID ticketId = UUID.fromString(ticketIdStr);

            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode(); // 🔒 Siempre usar la sala registrada y autenticada

            // 1. Resetear votos en la base de datos
            voteService.resetVotes(sessionInfo.sessionId(), ticketId, sessionInfo.participantId());

            // 2. Notificar que los votos y estadísticas quedan vacíos
            VotingRRAverage resetVotesPayload = new VotingRRAverage(
                inviteCode,
                List.of(),
                false,
                0.0,
                null,
                null
            );
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", resetVotesPayload);

            // 3. Regresar todos los indicadores de voto al reloj de espera
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/vote-status", (Object) Collections.emptyMap());

            // 4. Notificar que el ticket regresó a estado VOTING
            final String finalInviteCode = inviteCode;
            ticketRepository.findById(ticketId).ifPresent(ticket -> {
                TicketResponseDTO ticketDto = new TicketResponseDTO(
                    ticket.getId(),
                    ticket.getTittle(),
                    ticket.getDescription(),
                    ticket.getGameSession().getId(),
                    ticket.getStatus()
                );
                messagingTemplate.convertAndSend("/topic/session/" + finalInviteCode + "/ticket-updated", ticketDto);
            });

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));

        } catch (RuntimeException ex) {
            publishError(headerAccessor, "session.reset-votes", ex);
        }
    }

    @MessageMapping("/session.finish")
    public void finish(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();
            UUID participantId = sessionInfo.participantId();

            String ticketIdStr = payload.get("ticketId");
            if (ticketIdStr == null || ticketIdStr.isBlank()) {
                throw new IllegalArgumentException("Se requiere el ticketId para finalizar la estimación.");
            }
            UUID ticketId = UUID.fromString(ticketIdStr);

            var ticketResponse = ticketService.finishTicket(ticketId, participantId);

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/ticket-updated", ticketResponse);

        } catch (RuntimeException ex) {
            publishError(headerAccessor, "ticket.finish", ex);
        }
    }

    private Participant resolveGuestParticipant(SimpMessageHeaderAccessor headerAccessor, GameSession session) {
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        Object guestParticipantId = attributes == null ? null : attributes.get(StompAuthChannelInterceptor.GUEST_PARTICIPANT_ID);
        Object guestSessionCode = attributes == null ? null : attributes.get(StompAuthChannelInterceptor.GUEST_SESSION_CODE);

        if (!(guestParticipantId instanceof UUID participantId) || !session.getSessionCode().equals(guestSessionCode)) {
            throw new IllegalArgumentException(
                    "Para entrar como invitado primero solicita un token en POST /api/sessions/{code}/guests.");
        }

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("El invitado ya no existe en esta sala; vuelve a unirte."));

        if (participant.getUser() != null || !participant.getGameSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("El token de invitado no corresponde a esta sala.");
        }
        return participant;
    }

    private List<WebSocketParticipantResponse> mapParticipants(List<Participant> participants) {
        return participants.stream().map(participant -> new WebSocketParticipantResponse(
            participant.getId(),
            participant.getEffectiveName(), 
            participant.getRole() != null ? participant.getRole().name() : "",
            participant.getUser() == null
        )).toList();
    }

    private String resolveUsernameOrNull(SimpMessageHeaderAccessor headerAccessor) {
        try {
            return resolveUsername(headerAccessor);
        } catch (Exception e) {
            return null;
        }
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

    /**
     * Envía el error solo a la conexión que lo provocó (cola privada /user/queue/errors).
     * Antes se difundía a toda la sala con el nombre de la clase y el mensaje interno de la excepción.
     * Los errores de dominio conservan su mensaje (pensado para el usuario); el resto se reemplaza
     * por un mensaje genérico y se registra en el log con su stack trace.
     */
    private void publishError(SimpMessageHeaderAccessor headerAccessor, String action, RuntimeException ex) {
        String wsSessionId = headerAccessor.getSessionId();
        boolean expected = isExpectedError(ex);

        if (expected) {
            log.warn("WebSocket {} rechazado: {}", action, ex.getMessage());
        } else {
            log.error("Error inesperado en WebSocket {}", action, ex);
        }

        if (wsSessionId == null) {
            return;
        }

        String message = expected && ex.getMessage() != null
                ? ex.getMessage()
                : "No se pudo completar la acción. Intenta de nuevo.";

        SimpMessageHeaderAccessor errorHeaders = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        errorHeaders.setSessionId(wsSessionId);
        errorHeaders.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(wsSessionId, "/queue/errors", (Object) Map.of(
                "action", action,
                "code", errorCode(ex, expected),
                "message", message
        ), errorHeaders.getMessageHeaders());
    }

    private static boolean isExpectedError(RuntimeException ex) {
        return ex.getClass().getPackageName().equals("com.openpoker.globalexception")
                || ex instanceof IllegalArgumentException
                || ex instanceof IllegalStateException
                || ex instanceof UsernameNotFoundException;
    }

    private static String errorCode(RuntimeException ex, boolean expected) {
        if (!expected) {
            return "INTERNAL_ERROR";
        }
        String name = ex.getClass().getSimpleName();
        if (name.contains("NotFound")) {
            return "NOT_FOUND";
        }
        if (name.contains("Role") || name.contains("OnlyHost") || name.contains("IsNotParticipant")) {
            return "FORBIDDEN";
        }
        if (name.contains("NotInVoting") || name.contains("AlreadyIn")) {
            return "CONFLICT";
        }
        return "BAD_REQUEST";
    }

    @MessageMapping("/session.set-timer")
    public void setTimer(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;
        try {
            String ticketIdStr = payload.get("ticketId");
            String durationStr = payload.get("durationSeconds");

            if (ticketIdStr == null || ticketIdStr.isBlank()) {
                throw new IllegalArgumentException("El parámetro 'ticketId' es obligatorio.");
            }

            UUID ticketId = UUID.fromString(ticketIdStr);
            Integer durationSeconds = (durationStr != null && !durationStr.isBlank()) 
                                        ? Integer.parseInt(durationStr) 
                                        : null;

            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();

            SetTimerRequest request = new SetTimerRequest(durationSeconds);
            ticketTimerService.setTimer(
                sessionInfo.sessionId(), 
                ticketId, 
                sessionInfo.participantId(), 
                request
            );

        } catch (RuntimeException ex) {
            publishError(headerAccessor, "session.set-timer", ex);
        }
    }
}