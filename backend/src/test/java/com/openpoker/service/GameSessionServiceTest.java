package com.openpoker.service;

import com.openpoker.SessionCodeGenerator.SessionCodeGenerator;
import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.SessionStatus;
import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSessionServiceTest {
    @Mock
    private GameSessionRepository sessionRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionCodeGenerator codeGenerator;

    @InjectMocks
    private GameSessionService gameSessionService;

    @Test
    void createSession_success() {
        User user = User.builder().id(UUID.randomUUID()).username("user").role(UserRole.HOST).build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(codeGenerator.generate()).thenReturn("ABC123");
        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.empty());
        when(participantRepository.countByGameSession(any())).thenReturn(1L);

        SessionResponse res = gameSessionService.createSession("user", new CreateSessionRequest("Sprint 1"));

        assertNotNull(res);
        assertEquals("ABC123", res.sessionCode());
        assertEquals("Sprint 1", res.name());
    }

    @Test
    void getSession_valid() {
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").name("Sprint 1")
                .hostUserId(UUID.randomUUID()).status(SessionStatus.WAITING).createdAt(new Timestamp(System.
                        currentTimeMillis())).build();

        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(participantRepository.countByGameSession(session)).thenReturn(1L);

        SessionResponse res = gameSessionService.getSessionByCode("ABC123");

        assertEquals("ABC123", res.sessionCode());
        assertEquals("Sprint 1", res.name());
        assertEquals("WAITING", res.status());
    }

    @Test
    void getSession_invalid() {
        when(sessionRepository.findBySessionCode("INVALID")).thenReturn(Optional.empty());

        SessionNotFoundException ex = assertThrows(SessionNotFoundException.class, () -> gameSessionService.
                getSessionByCode("INVALID"));

        assertEquals("Session no encontrada", ex.getMessage());
    }

    @Test
    void joinSession_forbiddenForHost() {
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").name("Sprint 1")
                .hostUserId(UUID.randomUUID()).status(SessionStatus.WAITING).createdAt(new Timestamp(System.
                        currentTimeMillis())).build();
        User user = User.builder().id(UUID.randomUUID()).username("host").role(UserRole.HOST).build();

        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("host")).thenReturn(Optional.of(user));

        InsufficientRoleException ex = assertThrows(InsufficientRoleException.class,
                () -> gameSessionService.joinSession("host", "ABC123"));

        assertEquals("Solo los usuarios VOTER pueden unirse a sesiones", ex.getMessage());
    }
}
