package com.openpoker.controller;

import com.openpoker.dto.JoinSessionRequest;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.service.GameSessionService;
import com.openpoker.service.TicketService;
import com.openpoker.service.VoteService;
import com.openpoker.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final TicketService ticketService;

    @MessageMapping("/session.join")
    public void join(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = payload.get("inviteCode");
        String guestName = payload.get("guestName");
        UUID ticketId = payload.get("ticketId") != null ? UUID.fromString(payload.get("ticketId")) : null;
        try {
            String username = resolveUsernameOrNull(headerAccessor);
            var session = sessionRepository.findBySessionCode(inviteCode)
                                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));
            
            Participant participant;

                // 1. Caso Usuario Registrado
            if (username != null) {
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
                var existingParticipant = participantRepository.findByGameSessionAndUser(session, user);

                if (existingParticipant.isPresent()) {
                    participant = existingParticipant.get(); // 👈 Si ya está en la sala, solo lo recuperamos
                } else {
                    JoinSessionRequest joinRequest = new JoinSessionRequest(inviteCode, username, null);
                    service.joinSession(joinRequest); // 👈 Si no existe, lo unimos
                    participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow();
                }

            // 2. Caso Invitado (Guest)
            } else if (guestName != null && !guestName.isBlank()) {
                var existingGuest = participantRepository.findByGameSessionAndGuestDisplayName(session, guestName);

                if (existingGuest.isPresent()) {
                    participant = existingGuest.get(); // 👈 Si el invitado ya existe (ej. reconexión)
                } else {
                    JoinSessionRequest joinRequest = new JoinSessionRequest(inviteCode, null, guestName);
                    service.joinSession(joinRequest); // 👈 Si no existe, lo unimos
                    participant = participantRepository.findByGameSessionAndGuestDisplayName(session, guestName).orElseThrow();
                }
            } else {
                throw new IllegalArgumentException("Se requiere un usuario autenticado o un nombre de invitado.");
            }

            // 4. Registramos la conexión del WebSocket usando el nombre unificado
            sessionRegistry.register(
                headerAccessor.getSessionId(), 
                session.getId(), 
                participant.getId(), 
                participant.getEffectiveName(), // 👈 Nombre unificado
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
            log.error("Error en WebSocket join", ex);
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

            service.leaveSession(sessionInfo.participantId(), inviteCode);

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
            log.error("💥 ERROR CRÍTICO AL VOTAR EN SALA [{}]:", inviteCode, ex);
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

            // 🚀 Extraemos el participantId de la sesión de WebSocket
            UUID participantId = sessionInfo.participantId();

            // 1. Obtenemos el ticketId que viene en el payload
            String ticketIdStr = payload.get("ticketId");
            if (ticketIdStr == null || ticketIdStr.isBlank()) {
                throw new IllegalArgumentException("Se requiere el ticketId para finalizar la estimación.");
            }
            UUID ticketId = UUID.fromString(ticketIdStr);

            // 2. Finalizamos el ticket en TicketService
            var ticketResponse = ticketService.finishTicket(ticketId,participantId);

            // 3. Transmitimos el estado actualizado del ticket a la sala
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/ticket-updated", ticketResponse);

        } catch (RuntimeException ex) {
            publishError(inviteCode, "ticket.finish", ex);
        }
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
            return null; // Es un invitado sin token JWT
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
