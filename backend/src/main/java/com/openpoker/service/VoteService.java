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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final GameSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public VoteResponse castVote(String username, String code, CastVoteRequest request) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        if(session.getStatus() != SessionStatus.VOTING ) {
            throw new SessionNotInVotingException("Session no esta en Votacion");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if(participantRepository.findByGameSessionAndUser(session, user).isEmpty()) {
            throw new UsernameIsNotParticipantSessionException("No eres participante");
        }

        if (session.getDeck() == null) {
            throw new IllegalStateException("La sesion no tiene deck configurado");
        }

        boolean valid = session.getDeck().getValues().stream().anyMatch(v -> v.getValue().equals(request.cardValue()));

        if(!valid) {
            throw new InvalidVoteValueException("Valor invalido");
        }

        Vote vote;

        try {
            vote = voteRepository.findByGameSessionAndUser(session, user)
                    .orElseGet(() -> Vote.builder().gameSession(session).user(user).build());
        } catch (DataIntegrityViolationException ex) {
            vote = voteRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> ex);
        }

        vote.setCardValue(request.cardValue());

        Vote savedVote = voteRepository.save(vote);

        long participantCount = participantRepository.countByGameSession(session);
        long voteCount = voteRepository.findAllByGameSession(session).size();

        if (participantCount > 0 && voteCount >= participantCount) {
            session.setStatus(SessionStatus.WAITING);
            sessionRepository.save(session);
        }

        return new VoteResponse(user.getUsername(), savedVote.getCardValue(), savedVote.getUpdatedAt());
    }

    public VotingResultsResponse getVotes(String code, String username) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException("No eres participante"));

        List<Vote> votes = voteRepository.findAllByGameSession(session);

        List<VoteResponse> response = votes.stream().map(v -> new VoteResponse(v.getUser().getUsername(), session.isVotesRevealed() ? v.getCardValue() : "*", v
                .getUpdatedAt())).toList();

        return new VotingResultsResponse(code, response, session.isVotesRevealed());
    }

    public VotingResultsResponse revealVotes(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionNotInVotingException("Session finalizada");
        }

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new SessionNotInVotingException("Session aun no ha cerrado la votacion");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if(participant.getRole() != Participant.Role.HOST) {
            throw new OnlyHostCanRevealVotesException("Solo el host puede revelar");
        }

        session.setVotesRevealed(true);
        session.setStatus(SessionStatus.REVEALED);

        sessionRepository.save(session);

        return getVotes(code, username);
    }

    @Transactional
    public void resetVotes(String username, UUID sessionId) {
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede reiniciar la votacion");
        }

        voteRepository.deleteAllByGameSession(session);
        session.setVotesRevealed(false);
        session.setStatus(SessionStatus.VOTING);
        sessionRepository.save(session);
    }
}
