package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional
    public VoteResponse submitVote(UUID sessionId, UUID participantId, String value) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

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

        boolean valid = session.getDeck().getValues().stream().anyMatch(v -> v.getValue().equals(value));

        if(!valid) {
            throw new InvalidVoteValueException("Valor invalido");
        }

        Vote vote;

        try {
            vote = voteRepository.findByGameSessionAndUser(session, participant.getUser())
                    .orElseGet(() -> Vote.builder().gameSession(session).user(participant.getUser()).build());
        } catch (DataIntegrityViolationException ex) {
            vote = voteRepository.findByGameSessionAndUser(session, participant.getUser()).orElseThrow(() -> ex);
        }

        vote.setCardValue(value);

        Vote savedVote = voteRepository.save(vote);

        long participantCount = participantRepository.countByGameSession(session);
        long voteCount = voteRepository.findAllByGameSession(session).size();

        if (participantCount > 0 && voteCount >= participantCount) {
            session.setStatus(SessionStatus.WAITING);
            sessionRepository.save(session);
        }

        return new VoteResponse(participant.getUser().getUsername(), savedVote.getCardValue(), savedVote.getUpdatedAt());
    }

    public VotingResultsResponse getVotes(UUID sessionId, UUID participantId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(sessionId)) {
            throw new UsernameIsNotParticipantSessionException("Participante no pertenece a la sesion");
        }

        List<Vote> votes = voteRepository.findAllByGameSession(session);

        List<VoteResponse> response = votes.stream().map(v -> new VoteResponse(v.getUser().getUsername(), session.isVotesRevealed() ? v.getCardValue() : "*", v
                .getUpdatedAt())).toList();

        return new VotingResultsResponse(session.getSessionCode(), response, session.isVotesRevealed());
    }

    public Map<UUID, Boolean> getVoteStatus(UUID sessionId) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        List<Participant> participants = participantRepository.findAllByGameSession(session);
        Set<UUID> votedUserIds = voteRepository.findAllByGameSession(session).stream()
                .map(vote -> vote.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, Boolean> voteStatus = new HashMap<>();
        for (Participant participant : participants) {
            voteStatus.put(participant.getId(), votedUserIds.contains(participant.getUser().getId()));
        }

        return voteStatus;
    }

    public VotingResultsResponse revealVotes(UUID sessionId, UUID participantId) {
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

        return getVotes(sessionId, participantId);
    }

    public VoteResponse castVote(String username, String sessionCode, CastVoteRequest request) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);
        return submitVote(session.getId(), participant.getId(), request.cardValue());
    }

    public VotingResultsResponse getVotes(String sessionCode, String username) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);
        return getVotes(session.getId(), participant.getId());
    }

    public VotingResultsResponse revealVotes(String username, String sessionCode) {
        GameSession session = getSessionByCode(sessionCode);
        Participant participant = getParticipant(session, username);
        return revealVotes(session.getId(), participant.getId());
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
    public void resetVotes(UUID sessionId, UUID participantId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

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

        voteRepository.deleteAllByGameSession(session);
        session.setVotesRevealed(false);
        session.setStatus(SessionStatus.VOTING);
        sessionRepository.save(session);
    }
}
