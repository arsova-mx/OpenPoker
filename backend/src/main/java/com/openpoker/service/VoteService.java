package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.repository.CardValueRepository;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final GameSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final TicketRepository ticketRepository;
    private final CardValueRepository cardValueRepository;

    @Transactional
    public VoteResponse submitVote(UUID sessionId,UUID ticketId, UUID participantId, UUID value) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
        CardValue card = cardValueRepository.findById(value).orElseThrow(() -> new IllegalArgumentException("Valor de la carta no encontrado"));


        if(session.getStatus() != SessionStatus.VOTING ) {
            throw new SessionNotInVotingException("Session no esta en Votacion");
        }

        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        if (session.getDeck() == null) {
            throw new IllegalStateException("La sesion no tiene deck configurado");
        }

        
        boolean valid = session.getDeck().equals(card.getDeck());

        if(!valid) {
            throw new InvalidVoteValueException("Valor invalido");
        }

        Vote vote = voteRepository.findByTicketAndUser(ticket, participant.getUser()).orElse(null);

        Vote savedVote;

        if (vote != null) {
            vote.setCardValue(card);
            savedVote = voteRepository.saveAndFlush(vote);
        } else {
            vote = Vote.builder().ticket(ticket).user(participant.getUser()).cardValue(card).build();
            savedVote = voteRepository.saveAndFlush(vote);
        }

        long participantCount = participantRepository.countByGameSession(session);
        long voteCount = voteRepository.findAllByTicket(ticket).size();

        if (participantCount > 0 && voteCount >= participantCount) {
            session.setStatus(SessionStatus.WAITING);
            sessionRepository.save(session);
        }

        return new VoteResponse(participant.getUser().getUsername(), savedVote.getCardValue().getValue(), savedVote.getUpdatedAt());
    }

    public VotingResultsResponse getVotes(UUID sessionId,UUID ticketId, UUID participantId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        List<Vote> votes = voteRepository.findAllByTicket(ticket);

        List<VoteResponse> response = votes.stream().map(v -> new VoteResponse(v.getUser().getUsername(), session.isVotesRevealed() ? v.getCardValue().getValue() : "*", v
                .getUpdatedAt())).toList();

        return new VotingResultsResponse(session.getSessionCode(), response, session.isVotesRevealed());
    }


    public Map<UUID, Boolean> getVoteStatus(UUID sessionId,UUID ticketId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        List<Participant> participants = participantRepository.findAllByGameSession(session);
        Set<UUID> votedUserIds = voteRepository.findAllByTicket(ticket).stream()
                .map(vote -> vote.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
  

        Map<UUID, Boolean> voteStatus = new HashMap<>();
        for (Participant participant : participants) {
            voteStatus.put(participant.getId(), votedUserIds.contains(participant.getUser().getId()));
        }

        return voteStatus;
    }

    public VotingResultsResponse revealVotes(UUID sessionId, UUID participantId,UUID ticketId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionNotInVotingException("Session finalizada");
        }

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new SessionNotInVotingException("Session aun no ha cerrado la votacion");
        }

        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        if(participant.getRole() != Participant.Role.HOST) {
            throw new OnlyHostCanRevealVotesException("Solo el host puede revelar");
        }

        session.setVotesRevealed(true);
        session.setStatus(SessionStatus.REVEALED);

        sessionRepository.save(session);

        return getVotes(sessionId,ticketId, participantId);
    }

    public VoteResponse castVote(String username, String sessionCode,UUID ticketId, CastVoteRequest request) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);

        UUID cardValueId = UUID.fromString(request.cardValue());

        return submitVote(session.getId(),ticketId, participant.getId(), cardValueId);
    }

    public VotingResultsResponse getVotes(String sessionCode,UUID ticketId, String username) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);
        return getVotes(session.getId(),ticketId, participant.getId());
    }

    public VotingResultsResponse revealVotes(String username, String sessionCode,UUID ticketId) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);
        return revealVotes(session.getId(),ticketId, participant.getId());
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
    public void resetVotes(UUID sessionId,UUID ticketId, UUID participantId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        if(session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionNotInVotingException("Session finalizada");
        }

        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede reiniciar la votacion");
        }

        voteRepository.deleteAllByTicket(ticket);
        session.setVotesRevealed(false);
        session.setStatus(SessionStatus.VOTING);
        sessionRepository.save(session);
    }
}
