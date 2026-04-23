package com.openpoker.service;

import com.openpoker.domain.FibonacciDeck;
import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.SessionStatus;
import com.openpoker.entity.User;
import com.openpoker.globalexception.InvalidValue;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.globalexception.SessionNotInVotingException;
import com.openpoker.globalexception.UsernameIsNotParticipantSessionException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final GameSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public VoteResponse castVote(String username, String code, CastVoteRequest request) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new
                SessionNotFoundException("Session no encontrada"));

        if(session.getStatus() != SessionStatus.VOTING ) {
            throw new SessionNotInVotingException("Session no esta en Votacion");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado"));

        if(participantRepository.findByGameSessionAndUser(session, user).isEmpty()) {
            throw new UsernameIsNotParticipantSessionException("No eres participante");
        }
        if(!FibonacciDeck.isValid(request.cardValue())) {
            throw new InvalidValue("Valor invalido");
        }

        Vote vote = voteRepository.findByGameSessionAndUser(session, user).
    }
}
