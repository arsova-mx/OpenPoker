package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VoteStatisticsDTO;
import com.openpoker.dto.VotingRRAverage;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.repository.CardValueRepository;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
    private final VoteStatisticsService voteStatisticsService;

    // UUID conocido de tu baraja FIBONACCI en MySQL para fallback defensivo
    private static final String DEFAULT_FIBONACCI_DECK_ID = "a054d38e-62e9-4449-9013-00bd92df6ceb";

    @Transactional
    public VoteResponse submitVote(UUID sessionId, UUID ticketId, UUID participantId, UUID value) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

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

        Vote vote = voteRepository.findByTicketAndParticipant(ticket, participant).orElse(null);
        Vote savedVote;

        if (vote != null) {
            vote.setCardValue(card);
            savedVote = voteRepository.saveAndFlush(vote);
        } else {
            vote = Vote.builder()
                    .ticket(ticket)
                    .participant(participant)
                    .cardValue(card)
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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        List<Vote> votes = voteRepository.findAllByTicket(ticket);

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

        // 🚀 Se retorna el estado de revelado del Ticket específico, no el global de la sesión
        return new VotingResultsResponse(session.getSessionCode(), response, isTicketRevealed);
    }

    public Map<UUID, Boolean> getVoteStatus(UUID sessionId, UUID ticketId) {
        if (ticketId == null) {
            return new HashMap<>();
        }
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
                
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        List<Participant> participants = participantRepository.findAllByGameSession(session);
        
        Set<UUID> votedParticipantIds = voteRepository.findAllByTicket(ticket).stream()
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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

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

        List<Vote> votes = voteRepository.findAllByTicketId(ticketId);

        session.setVotesRevealed(true);
        ticket.setStatus(TicketStatus.REVEALED);

        sessionRepository.save(session);

        // 1. Calcular estadísticas (promedio, consenso, outliers)
        VoteStatisticsDTO statistics = voteStatisticsService.calculateStatistics(votes);
        
        // 2. Buscar la carta sugerida convirtiendo el UUID a String para el WHERE c.deck_id = :deckId
        CardValue suggested = null;
        if (session.getDeck() != null && session.getDeck().getId() != null) {
            suggested = cardValueRepository.findClosestByWeight(
                session.getDeck().getId().toString(), 
                statistics.average()
            ).orElse(null);
        }

        // Fallback defensivo si la sesión no tenía deck asignado o el deck no devolvió carta
        if (suggested == null && statistics.average() > 0) {
            suggested = cardValueRepository.findClosestByWeight(
                DEFAULT_FIBONACCI_DECK_ID, 
                statistics.average()
            ).orElse(null);
        }

        // 3. Mapear votos a DTOs
        List<VoteResponse> voteResponses = votes.stream()
                .map(v -> new VoteResponse(
                    v.getId(), 
                    v.getParticipant().getEffectiveName(), 
                    v.getCardValue().getValue(), 
                    v.getUpdatedAt()
                ))
                .toList();

        // 4. Retornar el DTO con el valor real de la carta encontrada
        return new VotingRRAverage(
                session.getSessionCode(),
                voteResponses,
                true,
                statistics.average(),
                suggested != null ? suggested.getValue() : "—",
                statistics
        );
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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

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

        voteRepository.deleteAllByTicket(ticket);
        session.setVotesRevealed(false);
        ticket.setStatus(TicketStatus.VOTING);
        sessionRepository.save(session);
    }
}