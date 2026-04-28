package com.openpoker.service;

import com.openpoker.entity.SessionStatus;
import com.openpoker.globalexception.HostNotFoundException;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.globalexception.UserAlreadyInSessionException;
import com.openpoker.globalexception.UsernameIsNotParticipantSessionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.sessioncodegenerator.SessionCodeGenerator;
import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    private static final int MAX_SESSION_CODE_RETRIES = 10;

    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionCodeGenerator codeGenerator;

    @Transactional
    public SessionResponse createSession(String username, CreateSessionRequest request) {
       User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        GameSession session = null;

        for (int attempt = 0; attempt < MAX_SESSION_CODE_RETRIES; attempt++) {
            String code = codeGenerator.generate();

            GameSession candidate = GameSession.builder().sessionCode(code).name(request.name()).hostUserId(user.getId()).status(SessionStatus.VOTING).build();

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
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if(participantRepository.findByGameSessionAndUser(session, user).isPresent()) {
            throw new UserAlreadyInSessionException("El usuario ya esta en la session");
        }

        Participant participant = Participant.builder().gameSession(session).user(user).role(Participant.Role.VOTER).build();

        participantRepository.save(participant);

        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HostNotFoundException("Host no encontrado"));

        return mapToResponse(session, host.getUsername());
    }

    public SessionResponse startVoting(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede iniciar la votacion");
        }

        session.setStatus(SessionStatus.VOTING);
        session.setVotesRevealed(false);
        sessionRepository.save(session);

        return mapToResponse(session, user.getUsername());
    }

    private SessionResponse mapToResponse(GameSession session, String hostUsername) {
        long count = participantRepository.countByGameSession(session);

        return new SessionResponse(session.getId(), session.getSessionCode(), session.getName(), hostUsername, session.getStatus().name(), count, session.getCreatedAt());
    }

}
