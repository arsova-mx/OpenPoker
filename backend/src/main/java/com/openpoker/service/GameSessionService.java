package com.openpoker.service;

import com.openpoker.entity.SessionStatus;
import com.openpoker.globalexception.HOSTNOTFOUNDEXCEPTION;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.globalexception.UserAlreadyInSessionException;
import org.springframework.stereotype.Service;

import com.openpoker.SessionCodeGenerator.SessionCodeGenerator;
import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionCodeGenerator codeGenerator;

    public SessionResponse createSession(String username, CreateSessionRequest request) {
       User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException(
               "Usuario no encontrado"));

        String code;

        do {
            code = codeGenerator.generate();
        } while (sessionRepository.findBySessionCode(code).isPresent());

        GameSession session = GameSession.builder().sessionCode(code).name(request.name()).hostUserId(user.getId())
                .status(SessionStatus.WAITING).build();

        sessionRepository.save(session);

        Participant host = Participant.builder().gameSession(session).user(user).role(Participant.Role.HOST).
                build();

        participantRepository.save(host);

        return mapToResponse(session, user.getUsername());
    }

    public SessionResponse getSessionByCode(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new
                SessionNotFoundException("Session no encontrada"));
        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HOSTNOTFOUNDEXCEPTION(
                "Host no encontrado"));
        return mapToResponse(session, host.getUsername());
    }


    public SessionResponse joinSession(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new
                SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException(
                "Usuario no encontrado"));

        if (user.getRole() != UserRole.VOTER) {
            throw new InsufficientRoleException("Solo los usuarios VOTER pueden unirse a sesiones");
        }

        if(participantRepository.findByGameSessionAndUser(session, user).isPresent()) {
            throw new UserAlreadyInSessionException("El usuario ya esta en la session");
        }

        Participant participant = Participant.builder().gameSession(session).user(user).role(
                Participant.Role.VOTER).build();

        participantRepository.save(participant);

        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HOSTNOTFOUNDEXCEPTION(
                "Host no encontrado"));

        return mapToResponse(session, host.getUsername());
    }

    private SessionResponse mapToResponse(GameSession session, String hostUsername) {
        long count = participantRepository.countByGameSession(session);

        return new SessionResponse(session.getId(), session.getSessionCode(), session.getName(), hostUsername,
                session.getStatus().name(), count, session.getCreatedAt());
    }

}
