package com.openpoker.service;

import com.openpoker.sessioncodegenerator.SessionCodeGenerator;
import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.SessionStatus;
import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;
import com.openpoker.entity.VotingDeck;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VotingDeckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;


import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Mock
    private VotingDeckRepository deckRepository;

    @InjectMocks
    private GameSessionService gameSessionService;

    @Test
    void createSession_success() {
        User user = User.builder().id(UUID.randomUUID()).username("user").role(UserRole.HOST).build();
        VotingDeck deck = VotingDeck.builder().id(UUID.randomUUID()).name("Fibonacci").build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(codeGenerator.generate()).thenReturn("ABC123");
        when(sessionRepository.saveAndFlush(any(GameSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.countByGameSession(any())).thenReturn(1L);

        SessionResponse res = gameSessionService.createSession("user", new CreateSessionRequest("Sprint 1"), deck.getId());

        assertNotNull(res);
        assertEquals("ABC123", res.sessionCode());
        assertEquals("Sprint 1", res.name());
        assertEquals("VOTING", res.status());
    }

    @Test
    void createSession_retriesWhenUniqueConstraintCollides() {
        User user = User.builder().id(UUID.randomUUID()).username("user").role(UserRole.HOST).build();
        VotingDeck deck = VotingDeck.builder().id(UUID.randomUUID()).name("Fibonacci").build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(codeGenerator.generate()).thenReturn("ABC123", "XYZ789");
        when(sessionRepository.saveAndFlush(any(GameSession.class))).thenThrow(new DataIntegrityViolationException("duplicate key")).thenAnswer(invocation -> invocation
                .getArgument(0));
        when(participantRepository.countByGameSession(any())).thenReturn(1L);

        SessionResponse res = gameSessionService.createSession("user", new CreateSessionRequest("Sprint 1"), deck.getId());

        assertNotNull(res);
        assertEquals("XYZ789", res.sessionCode());
        assertEquals("VOTING", res.status());
        verify(codeGenerator, times(2)).generate();
        verify(sessionRepository, times(2)).saveAndFlush(any(GameSession.class));
    }

    @Test
    void getSession_valid() {
        UUID hostId = UUID.randomUUID();

        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").name("Sprint 1").hostUserId(hostId).status(SessionStatus.WAITING)
                .createdAt(new Timestamp(System.currentTimeMillis())).build();

        User host = User.builder().id(hostId).username("host").role(UserRole.HOST).build();

        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(participantRepository.countByGameSession(session)).thenReturn(1L);

        SessionResponse res = gameSessionService.getSessionByCode("ABC123");

        assertEquals("ABC123", res.sessionCode());
    }

    @Test
    void getSession_invalid() {
        when(sessionRepository.findBySessionCode("INVALID")).thenReturn(Optional.empty());

        SessionNotFoundException ex = assertThrows(SessionNotFoundException.class, () -> gameSessionService.getSessionByCode("INVALID"));

        assertEquals("Session no encontrada", ex.getMessage());
    }

    @Test
    void startVoting_hostCanOpenRound() {
        UUID hostId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").name("Sprint 1").hostUserId(hostId).status(SessionStatus.WAITING)
                .votesRevealed(true).createdAt(new Timestamp(System.currentTimeMillis())).build();
        User host = User.builder().id(hostId).username("host").role(UserRole.HOST).build();
        Participant participant = Participant.builder().gameSession(session).user(host).role(Participant.Role.HOST).build();

        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("host")).thenReturn(Optional.of(host));
        when(participantRepository.findByGameSessionAndUser(session, host)).thenReturn(Optional.of(participant));
        when(sessionRepository.save(session)).thenReturn(session);
        when(participantRepository.countByGameSession(session)).thenReturn(2L);

        SessionResponse response = gameSessionService.startVoting("host", "ABC123");

        assertEquals("VOTING", response.status());
        assertFalse(session.isVotesRevealed());
    }

    @Test
    void startVoting_rejectsNonHost() {
        UUID hostId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").name("Sprint 1").hostUserId(hostId).status(SessionStatus.WAITING).build();
        User voter = User.builder().id(UUID.randomUUID()).username("voter").role(UserRole.VOTER).build();
        Participant participant = Participant.builder().gameSession(session).user(voter).role(Participant.Role.VOTER).build();

        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("voter")).thenReturn(Optional.of(voter));
        when(participantRepository.findByGameSessionAndUser(session, voter)).thenReturn(Optional.of(participant));

        InsufficientRoleException ex = assertThrows(InsufficientRoleException.class, () -> gameSessionService.startVoting("voter", "ABC123"));

        assertEquals("Solo el host puede iniciar la votacion", ex.getMessage());
    }

    @Test
    void finishSession_hostCanFinish() {
        UUID hostId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").name("Sprint 1").hostUserId(hostId).status(SessionStatus.WAITING)
                .createdAt(new Timestamp(System.currentTimeMillis())).build();
        User host = User.builder().id(hostId).username("host").role(UserRole.HOST).build();
        Participant participant = Participant.builder().gameSession(session).user(host).role(Participant.Role.HOST).build();

        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("host")).thenReturn(Optional.of(host));
        when(participantRepository.findByGameSessionAndUser(session, host)).thenReturn(Optional.of(participant));
        when(sessionRepository.save(session)).thenReturn(session);
        when(participantRepository.countByGameSession(session)).thenReturn(2L);

        SessionResponse response = gameSessionService.finishSession("host", "ABC123");

        assertEquals("FINISHED", response.status());
        assertEquals(SessionStatus.FINISHED, session.getStatus());
    }
}
