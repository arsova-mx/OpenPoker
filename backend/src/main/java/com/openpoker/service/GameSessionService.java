package com.openpoker.service;

import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.sessioncodegenerator.SessionCodeGenerator;
import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionService {
    private static final int MAX_SESSION_CODE_RETRIES = 10;

    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionCodeGenerator codeGenerator;
    private final VotingDeckRepository deckRepository;
   

    @Transactional
    public SessionResponse createSession(String username, CreateSessionRequest request, UUID deckId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        VotingDeck deck = deckRepository.findById(deckId).orElseThrow(() -> new DeckNotFoundException("Deck no encontrado"));

        return createSessionWithDeck(user, request, deck);
    }

    @Transactional
    public SessionResponse createSessionWithDefaultDeck(String username, CreateSessionRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        VotingDeck deck = deckRepository.findBySeriesType(CardSeries.FIBONACCI)
                .orElseThrow(() -> new DeckNotFoundException("Deck Fibonacci no encontrado"));

        return createSessionWithDeck(user, request, deck);
    }

    private SessionResponse createSessionWithDeck(User user, CreateSessionRequest request, VotingDeck deck) {

        GameSession session = null;

        for (int attempt = 0; attempt < MAX_SESSION_CODE_RETRIES; attempt++) {
            String code = codeGenerator.generate();

            GameSession candidate = GameSession.builder().sessionCode(code).name(request.name()).hostUserId(user.getId()).deck(deck).build();

            try {
                session = sessionRepository.saveAndFlush(candidate);

                break;
            } catch (DataIntegrityViolationException ex) {
                // Una colisión de restricción única en sessionCode significa que otra solicitud ganó la carrera.
                if (attempt == MAX_SESSION_CODE_RETRIES - 1) {
                    throw ex;
                }
            }
        }

        if (session == null) {
            throw new IllegalStateException("No se pudo crear la sesion");
        }

        Participant host = Participant.builder().gameSession(session).user(user).role(Participant.Role.HOST).build();

        participantRepository.save(host);

        return mapToResponse(session, user.getUsername());
    }

    public SessionResponse getSessionByCode(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HostNotFoundException("Host no encontrado"));

        return mapToResponse(session, host.getUsername());
    }


    public SessionResponse joinSession(String username, String code) {
                log.info("join session ---------------------------------------------------------------------");

        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if(participantRepository.findByGameSessionAndUser(session, user).isPresent()) {
            throw new UserAlreadyInSessionException("El usuario ya esta en la session");
        }

        Participant.Role role = session.getHostUserId().equals(user.getId()) ? Participant.Role.HOST : Participant.Role.VOTER;

        Participant participant = Participant.builder().gameSession(session).user(user).role(role).build();

        participantRepository.save(participant);

        String hostUsername = username;
        if (role != Participant.Role.HOST) {
            if (session.getHostUserId() == null) {
                throw new IllegalStateException("La sesión no tiene un Host ID válido asignado.");
            }
            User host = userRepository.findById(session.getHostUserId())
                    .orElseThrow(() -> new HostNotFoundException("Host no encontrado"));
            hostUsername = host.getUsername();
        }
        log.info("si hizo el join bien");
        return mapToResponse(session, hostUsername);
    }

    @Transactional
    public SessionResponse finishSession(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede finalizar la session");
        }

        //session.setStatus(SessionStatus.FINISHED);
        sessionRepository.save(session);
        participantRepository.deleteAllByGameSession(session);

        return mapToResponse(session, user.getUsername());
    }

    public List<Participant> getParticipants(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        return participantRepository.findAllByGameSession(session);
    }

    public SessionResponse leaveSession(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (session.getHostUserId().equals(user.getId())) {
            throw new InsufficientRoleException("El host no puede abandonar la session");
        }

        participantRepository.delete(participant);

        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HostNotFoundException("Host no encontrado"));

        return mapToResponse(session, host.getUsername());
    }

    private SessionResponse mapToResponse(GameSession session, String hostUsername) {
        long count = participantRepository.countByGameSession(session);

        return new SessionResponse(session.getId(), session.getSessionCode(), session.getName(), hostUsername, count, session.getCreatedAt());
    }

    @Transactional
    public void handleDisconnect(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (session.getHostUserId().equals(user.getId())) {
            // Si es el host, finalizar la sesión
            finishSession(username, code);
        } else {
            // Si no es el host, simplemente abandonar
            leaveSession(username, code);
        }
    }

}
