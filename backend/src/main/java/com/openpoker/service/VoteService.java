package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VoteStatisticsDTO;
import com.openpoker.dto.VotingRRAverage;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.CardValueRepository;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import com.openpoker.repository.VotingDeckRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
// NUEVO: WEBSOCKET - Importación necesaria para enviar mensajes
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final GameSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final TicketRepository ticketRepository;
    private final CardValueRepository cardValueRepository;
    private final VotingDeckRepository deckRepository;
    private final VoteStatisticsService voteStatisticsService;
    
    // NUEVO: WEBSOCKET - Inyección de la plantilla de mensajería
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public VoteResponse submitVote(UUID sessionId, UUID ticketId, UUID participantId, UUID value) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        // 🔒 Validación: No permitir votos si la sesión ya finalizó
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionFinishedException("La sesión ya ha finalizado. No se permiten más votos ni interacciones.");
        }

        Ticket ticket = ticketRepository.findByIdAndGameSessionId(ticketId, sessionId)
        .orElseThrow(() -> new IllegalArgumentException("El ticket no existe o no pertenece a la sesión proporcionada"));

        if (!ticket.getGameSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("El ticket no pertenece a la sesión proporcionada");
        }

        CardValue card = cardValueRepository.findById(value)
                .orElseThrow(() -> new IllegalArgumentException("Valor de la carta no encontrado"));

        if (ticket.getStatus() != TicketStatus.VOTING) {
            throw new SessionNotInVotingException("Session no esta en Votacion");
        }

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        if (session.getDeck() == null) {
            throw new IllegalStateException("La sesion no tiene deck configurado");
        }

        if (ticket.getTimerExpiresAt() != null && Instant.now().isAfter(ticket.getTimerExpiresAt())) {
            throw new IllegalStateException("El tiempo de votación para este ticket ha finalizado");
        }

        boolean valid = session.getDeck().getId().equals(card.getDeck().getId());
        if (!valid) {
            throw new InvalidVoteValueException("Valor invalido");
        }

        Vote vote = voteRepository.findByTicketAndParticipantAndRound(ticket, participant, ticket.getCurrentRound()).orElse(null);
        Vote savedVote;

        if (vote != null) {
            vote.setCardValue(card);
            savedVote = voteRepository.saveAndFlush(vote);
        } else {
            vote = Vote.builder()
                    .ticket(ticket)
                    .participant(participant)
                    .cardValue(card)
                    .round(ticket.getCurrentRound())
                    .build();
            savedVote = voteRepository.saveAndFlush(vote);
        }

        return new VoteResponse(
                savedVote.getId(),
                participant.getEffectiveName(),
                savedVote.getCardValue().getValue(),
                savedVote.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public VotingResultsResponse getVotes(UUID sessionId, UUID ticketId, UUID participantId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        Ticket ticket = ticketRepository.findByIdAndGameSessionId(ticketId, sessionId)
        .orElseThrow(() -> new IllegalArgumentException("El ticket no existe o no pertenece a la sesión proporcionada"));
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        List<Vote> votes = voteRepository.findAllByTicketAndRound(ticket, ticket.getCurrentRound());

        boolean isTicketRevealed = ticket.getStatus() == TicketStatus.REVEALED 
                                || ticket.getStatus() == TicketStatus.FINISHED;

        List<VoteResponse> response = votes.stream()
                .map(v -> {
                    boolean isOwnVote = v.getParticipant().getId().equals(participantId);
                    String cardDisplay = (isTicketRevealed || isOwnVote) 
                            ? v.getCardValue().getValue() 
                            : "*";

                    return new VoteResponse(
                            v.getId(),
                            v.getParticipant().getEffectiveName(), 
                            cardDisplay, 
                            v.getUpdatedAt()
                    );
                })
                .toList();

        return new VotingResultsResponse(session.getSessionCode(), response, isTicketRevealed);
    }

    public Map<UUID, Boolean> getVoteStatus(UUID sessionId, UUID ticketId) {
        if (ticketId == null) {
            return new HashMap<>();
        }
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
                
        Ticket ticket = ticketRepository.findByIdAndGameSessionId(ticketId, sessionId)
        .orElseThrow(() -> new IllegalArgumentException("El ticket no existe o no pertenece a la sesión proporcionada"));

        List<Participant> participants = participantRepository.findAllByGameSession(session);
        
        Set<UUID> votedParticipantIds = voteRepository.findAllByTicketAndRound(ticket, ticket.getCurrentRound()).stream()
                .map(vote -> vote.getParticipant().getId())
                .collect(Collectors.toSet());

        Map<UUID, Boolean> voteStatus = new HashMap<>();
        for (Participant participant : participants) {
            voteStatus.put(participant.getId(), votedParticipantIds.contains(participant.getId()));
        }

        return voteStatus;
    }

    @Transactional
    public VotingRRAverage revealVotes(UUID sessionId, UUID participantId, UUID ticketId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        // 🔒 Validación: No permitir revelar si la sesión ya finalizó
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionFinishedException("La sesión ya ha finalizado. No se permiten más interacciones.");
        }

        Ticket ticket = ticketRepository.findByIdAndGameSessionId(ticketId, sessionId)
        .orElseThrow(() -> new IllegalArgumentException("El ticket no existe o no pertenece a la sesión proporcionada"));

        if (ticket.getStatus() == TicketStatus.FINISHED) {
            throw new SessionNotInVotingException("Session finalizada");
        }

        if (ticket.getStatus() == TicketStatus.REVEALED) {
            throw new SessionNotInVotingException("Los votos ya han sido revelados");
        }

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        if (participant.getRole() != Participant.Role.HOST) {
            throw new OnlyHostCanRevealVotesException("Solo el host puede revelar");
        }

        List<Vote> votes = voteRepository.findAllByTicketIdAndRound(ticketId, ticket.getCurrentRound());

        session.setVotesRevealed(true);
        ticket.setStatus(TicketStatus.REVEALED);

        sessionRepository.save(session);
        ticketRepository.save(ticket);

        VoteStatisticsDTO statistics = voteStatisticsService.calculateStatistics(votes);
        
        VotingDeck activeDeck = session.getDeck();
        if (activeDeck == null) {
            activeDeck = deckRepository.findBySeriesType(CardSeries.FIBONACCI).orElse(null);
        }

        CardValue suggested = null;
        if (activeDeck != null && statistics.average() > 0) {
            suggested = cardValueRepository.findClosestByWeight(
                activeDeck.getId().toString(), 
                statistics.average()
            ).orElse(null);
        }

        List<VoteResponse> voteResponses = votes.stream()
                .map(v -> new VoteResponse(
                    v.getId(), 
                    v.getParticipant().getEffectiveName(), 
                    v.getCardValue().getValue(), 
                    v.getUpdatedAt()
                ))
                .toList();

        VotingRRAverage result = new VotingRRAverage(
                session.getSessionCode(),
                voteResponses,
                true,
                statistics.average(),
                suggested != null ? suggested.getValue() : "—",
                statistics
        );

        return result;
    }

    public VoteResponse castVote(String username, String sessionCode, UUID ticketId, CastVoteRequest request) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);

        UUID cardValueId = UUID.fromString(request.cardValue());

        return submitVote(session.getId(), ticketId, participant.getId(), cardValueId);
    }

    public VotingResultsResponse getVotes(String sessionCode, UUID ticketId, UUID participantId) {
        GameSession session = getSessionByCode(sessionCode);
        return getVotes(session.getId(), ticketId, participantId);
    }

    public VotingRRAverage revealVotes(UUID participantId, String sessionCode, UUID ticketId) {
        GameSession session = getSessionByCode(sessionCode);
        return revealVotes(session.getId(), participantId, ticketId);
    }

    private GameSession getSessionByCode(String sessionCode) {
        return sessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
    }

    private Participant getParticipant(GameSession session, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return participantRepository.findByGameSessionAndUser(session, user)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));
    }

    @Transactional
    public void resetVotes(UUID sessionId, UUID ticketId, UUID participantId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        // 🔒 Validación: No permitir resetear rondas si la sesión ya finalizó
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionFinishedException("La sesión ya ha finalizado. No se permiten más interacciones.");
        }

        Ticket ticket = ticketRepository.findByIdAndGameSessionId(ticketId, sessionId)
            .orElseThrow(() -> new IllegalArgumentException("El ticket no existe o no pertenece a la sesión proporcionada"));

        if (ticket.getStatus() == TicketStatus.FINISHED) {
            throw new SessionNotInVotingException("Session finalizada");
        }

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede reiniciar la votacion");
        }

        ticket.setCurrentRound(ticket.getCurrentRound() + 1);
        
        session.setVotesRevealed(false);
        ticket.setStatus(TicketStatus.VOTING);
        
        ticketRepository.save(ticket);
        sessionRepository.save(session);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "VOTES_RESET");
        payload.put("ticketId", ticket.getId());
        payload.put("newRound", ticket.getCurrentRound());

        messagingTemplate.convertAndSend("/topic/session/" + session.getSessionCode() + "/votes", payload);
    }
}