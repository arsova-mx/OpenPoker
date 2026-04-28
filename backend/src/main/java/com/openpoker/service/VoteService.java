package com.openpoker.service;

import com.openpoker.domain.FibonacciDeck;
import com.openpoker.domain.VotingDeck;
import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final GameSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public VoteResponse castVote(String username, String code, CastVoteRequest request) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        if(session.getStatus() != SessionStatus.VOTING ) {
            throw new SessionNotInVotingException("Session no esta en Votacion");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if(participantRepository.findByGameSessionAndUser(session, user).isEmpty()) {
            throw new UsernameIsNotParticipantSessionException("No eres participante");
        }

        VotingDeck deck = new FibonacciDeck();

        if(!deck.isValid(request.cardValue())) {
            throw new InvalidVoteValueException("Valor de voto invalido");
        }

        Vote vote = voteRepository.findByGameSessionAndUser(session, user).orElse(Vote.builder().gameSession(session).user(user).build());

        vote.setCardValue(request.cardValue());

        voteRepository.save(vote);

        return new VoteResponse(user.getUsername(), vote.getCardValue(), vote.getUpdatedAt());
    }

    public VotingResultsResponse getVotes(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        List<Vote> votes = voteRepository.findAllByGameSession(session);

        List<VoteResponse> response = votes.stream().map(v -> new VoteResponse(v.getUser().getUsername(), session.isVotesRevealed() ? v.getCardValue() : "*", v.
                getUpdatedAt())).toList();

        return new VotingResultsResponse(code, response, session.isVotesRevealed());
    }

    public VotingResultsResponse revealVotes(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        if (session.getStatus() != SessionStatus.VOTING) {
            throw new SessionNotInVotingException("Session no esta en Votacion");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if(participant.getRole() != Participant.Role.HOST) {
            throw new OnlyHostCanRevealVotesException("Solo el host puede revelar");
        }

        session.setVotesRevealed(true);

        sessionRepository.save(session);

        return getVotes(code);
    }
}
