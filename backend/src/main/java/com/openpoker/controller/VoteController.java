package com.openpoker.controller;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingRRAverage;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.service.GameSessionService;
import com.openpoker.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{code}/votes")
@RequiredArgsConstructor
public class VoteController {
    private final VoteService service;
    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final TicketRepository ticketRepository;
    private final GameSessionService gameSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<VoteResponse> vote(
            @AuthenticationPrincipal String username, 
            @PathVariable String code,
            @RequestParam UUID ticketId, 
            @RequestBody @Valid CastVoteRequest request) {
        
        Participant participant = getParticipantByUsername(code, username);
        UUID cardValueId = UUID.fromString(request.cardValue());

        VoteResponse response = service.submitVote(
            participant.getGameSession().getId(), 
            ticketId, 
            participant.getId(), 
            cardValueId
        );

        // Notificar el cambio de estado de votos por WebSocket a los demás clientes
        messagingTemplate.convertAndSend(
            "/topic/session/" + code + "/vote-status", 
            service.getVoteStatus(participant.getGameSession().getId(), ticketId)
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<VotingResultsResponse> getVotes(
            @AuthenticationPrincipal String username, 
            @PathVariable String code,
            @RequestParam UUID ticketId) {
        Participant participant = getParticipantByUsername(code, username);
        return ResponseEntity.ok(service.getVotes(participant.getGameSession().getId(), ticketId, participant.getId()));
    }

    @PostMapping("/reveal")
    public ResponseEntity<VotingRRAverage> reveal(
            @AuthenticationPrincipal String username, 
            @PathVariable String code,
            @RequestParam UUID ticketId) {
        Participant participant = getParticipantByUsername(code, username);
        VotingRRAverage results = service.revealVotes(participant.getGameSession().getId(), participant.getId(), ticketId);

        // Broadcast de resultados para clientes WebSocket conectados
        messagingTemplate.convertAndSend("/topic/session/" + code + "/votes", results);
        messagingTemplate.convertAndSend("/topic/session/" + code + "/vote-status", service.getVoteStatus(participant.getGameSession().getId(), ticketId));

        return ResponseEntity.ok(results);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetVotes(
            @AuthenticationPrincipal String username, 
            @PathVariable String code,
            @RequestParam UUID ticketId) {
        
        Participant participant = getParticipantByUsername(code, username);

        // 1. Resetear votos y flags de revelado en base de datos
        service.resetVotes(participant.getGameSession().getId(), ticketId, participant.getId());

        // 2. Broadcast WebSocket para sincronizar a cualquier cliente conectado
        VotingRRAverage resetVotesPayload = new VotingRRAverage(
            code,
            List.of(),
            false,
            0.0,
            null,
            null
        );
        messagingTemplate.convertAndSend("/topic/session/" + code + "/votes", resetVotesPayload);
        messagingTemplate.convertAndSend("/topic/session/" + code + "/vote-status", (Object) Collections.emptyMap());

        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            TicketResponseDTO ticketDto = new TicketResponseDTO(
                ticket.getId(),
                ticket.getTittle(),
                ticket.getDescription(),
                ticket.getGameSession().getId(),
                ticket.getStatus()
            );
            messagingTemplate.convertAndSend("/topic/session/" + code + "/ticket-updated", ticketDto);
        });

        messagingTemplate.convertAndSend("/topic/session/" + code + "/state", gameSessionService.getSessionByCode(code));

        return ResponseEntity.noContent().build();
    }

    // Helper para obtener el Participant a partir del código de sala y el username autenticado por JWT
    private Participant getParticipantByUsername(String sessionCode, String username) {
        GameSession session = sessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada con código: " + sessionCode));
                
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
                
        return participantRepository.findByGameSessionAndUser(session, user)
                .orElseThrow(() -> new ParticipantNotFoundException("El usuario no es participante de esta sesión"));
    }
}