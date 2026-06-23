package com.openpoker.service;

import com.openpoker.controller.WebSocketController;
import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.globalexception.InvalidVoteValueException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketTest {

    @Mock
    private GameSessionService sessionService;

    @Mock
    private GameSessionRepository sessionRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private VoteService voteService;

    @Mock
    private WebSocketSessionRegistry sessionRegistry;

    @InjectMocks
    private WebSocketController webSocketController;

    // 🚀 Atributo global para el ticketId de pruebas
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
    }

    @Test
    void join_broadcastsParticipantsToInviteCodeTopic() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").build();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").build();
        Participant participant = Participant.builder().id(UUID.randomUUID()).gameSession(session).user(user).role(Participant.Role.VOTER).build();
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(participant.getUser().getId(), "alice", "VOTER"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "alice");
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "VOTING", 1L);

        when(sessionService.getParticipants("ABC123")).thenReturn(participants);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);
        // 🔄 CORREGIDO: voteService.getVoteStatus ahora requiere ticketId (pasa como ticketId)
        when(voteService.getVoteStatus(session.getId(), ticketId)).thenReturn(Map.of());
        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.empty(), Optional.of(participant));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.join(Map.of("inviteCode", "ABC123", "ticketId", ticketId.toString()), headerAccessor);

        verify(sessionService).joinSession("alice", "ABC123");
        verify(sessionService).getParticipants("ABC123");
        verify(sessionService).getSessionByCode("ABC123");
        verify(sessionRegistry).register("ws-session-1", session.getId(), participant.getId(), "alice", "ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", (Object) expected);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
        // 🔄 CORREGIDO: Verificación con ticketId
        verify(voteService).getVoteStatus(session.getId(), ticketId);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/vote-status", (Object) Map.of());
    }

    @Test
    void join_toleratesExistingParticipant() {
        User user = User.builder().id(UUID.randomUUID()).username("host").build();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").build();
        Participant participant = Participant.builder().id(UUID.randomUUID()).gameSession(session).user(user).role(Participant.Role.HOST).build();
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(participant.getUser().getId(), "host", "HOST"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "host");
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "VOTING", 1L);

        when(sessionService.getParticipants("ABC123")).thenReturn(participants);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);
        // 🔄 CORREGIDO: voteService.getVoteStatus requiere ticketId (pasa como ticketId)
        when(voteService.getVoteStatus(session.getId(), ticketId)).thenReturn(Map.of());
        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("host")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.join(Map.of("inviteCode", "ABC123", "ticketId", ticketId.toString()), headerAccessor);

        verify(sessionService, never()).joinSession("host", "ABC123");
        verify(sessionRegistry).register("ws-session-1", session.getId(), participant.getId(), "host", "ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", (Object) expected);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
    }

    @Test
    void leave_broadcastsParticipantsToInviteCodeTopic() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(sessionId).sessionCode("ABC123").build();
        Participant participant = buildParticipant("host", Participant.Role.HOST);
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(participant.getUser().getId(), "host", "HOST"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "alice");
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "VOTING", 1L);

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo(sessionId, participantId, "alice", "ABC123")));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionService.getParticipants("ABC123")).thenReturn(participants);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);
        // 🔄 CORREGIDO: voteService.getVoteStatus requiere ticketId (pasa como ticketId)
        when(voteService.getVoteStatus(session.getId(), ticketId)).thenReturn(Map.of());
        when(sessionRepository.findBySessionCode("ABC123")).thenReturn(Optional.of(session));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.leave(Map.of("ticketId", ticketId.toString()), headerAccessor);

        verify(sessionService).leaveSession("alice", "ABC123");
        verify(sessionService).getParticipants("ABC123");
        verify(sessionService).getSessionByCode("ABC123");
        verify(sessionRegistry).unregister("ws-session-1");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", (Object) expected);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/vote-status", (Object) Map.of());
    }

    @Test
    void vote_broadcastsVotesAndState() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(sessionId).sessionCode("ABC123").build();
        VotingResultsResponse votingResults = new VotingResultsResponse("ABC123", List.of(), false);
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "WAITING", 2L);
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "mallory");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo(sessionId, participantId, "mallory", "ABC123")));
        // 🔄 CORREGIDO: voteService.getVotes requiere ticketId
        when(voteService.getVotes(sessionId, ticketId, participantId)).thenReturn(votingResults);
        // 🔄 CORREGIDO: voteService.getVoteStatus requiere ticketId
        when(voteService.getVoteStatus(sessionId, ticketId)).thenReturn(Map.of());
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.vote(Map.of("inviteCode", "OTHER", "username", "mallory", "cardValue", "5", "ticketId", ticketId.toString()), headerAccessor);

        // 🔄 CORREGIDO: Se verifica enviando el ticketId
        verify(voteService).submitVote(sessionId, ticketId, participantId, "5");
        verify(voteService).getVotes(sessionId, ticketId, participantId);
        verify(voteService).getVoteStatus(sessionId, ticketId);
        verify(sessionService).getSessionByCode("ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/votes", (Object) votingResults);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/vote-status", (Object) Map.of());
    }

    @Test
    void reveal_broadcastsRevealedVotesAndState() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(sessionId).sessionCode("ABC123").build();
        VotingResultsResponse votingResults = new VotingResultsResponse("ABC123", List.of(), true);
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "REVEALED", 2L);
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "mallory");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo(sessionId, participantId, "mallory", "ABC123")));
        // 🔄 CORREGIDO: voteService.revealVotes requiere ticketId
        when(voteService.revealVotes(sessionId, ticketId, participantId)).thenReturn(votingResults);
        // 🔄 CORREGIDO: voteService.getVoteStatus requiere ticketId
        when(voteService.getVoteStatus(sessionId, ticketId)).thenReturn(Map.of());
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.reveal(Map.of("inviteCode", "OTHER", "username", "mallory", "ticketId", ticketId.toString()), headerAccessor);

        // 🔄 CORREGIDO: Verificaciones actualizadas
        verify(voteService).revealVotes(sessionId, ticketId, participantId);
        verify(voteService).getVoteStatus(sessionId, ticketId);
        verify(sessionService).getSessionByCode("ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/votes", (Object) votingResults);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/vote-status", (Object) Map.of());
    }

    @Test
    void vote_broadcastsErrorWhenVoteFails() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(sessionId).sessionCode("ABC123").build();
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "mallory");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo(sessionId, participantId, "mallory", "ABC123")));
        // 🔄 CORREGIDO: voteService.submitVote requiere ticketId
        when(voteService.submitVote(sessionId, ticketId, participantId, "999")).thenThrow(new InvalidVoteValueException("Valor invalido"));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.vote(Map.of("inviteCode", "OTHER", "username", "mallory", "cardValue", "999", "ticketId", ticketId.toString()), headerAccessor);

        verify(messagingTemplate).convertAndSend(
                "/topic/session/ABC123/errors",
                (Object) Map.of(
                        "action", "session.vote",
                        "type", "InvalidVoteValueException",
                        "message", "Valor invalido"
                )
        );
        // 🔄 CORREGIDO: Firma de getVotes con ticketId
        verify(voteService, never()).getVotes(sessionId, ticketId, participantId);
        verify(sessionService, never()).getSessionByCode("ABC123");
    }

    @Test
    void resetVotes_broadcastsClearedVotesAndState() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        GameSession session = GameSession.builder().id(sessionId).sessionCode("ABC123").build();
        VotingResultsResponse votingResults = new VotingResultsResponse("ABC123", List.of(), false);
        SessionResponse sessionResponse = new SessionResponse(sessionId, "ABC123", "Sprint Planning", "host",
                "VOTING", 2L, new Timestamp(System.currentTimeMillis()));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "mallory");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo(sessionId, participantId, "mallory", "ABC123")));
        // 🔄 CORREGIDO: voteService.getVotes requiere ticketId
        when(voteService.getVotes(sessionId, ticketId, participantId)).thenReturn(votingResults);
        // 🔄 CORREGIDO: voteService.getVoteStatus requiere ticketId
        when(voteService.getVoteStatus(sessionId, ticketId)).thenReturn(Map.of());
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 🔄 CORREGIDO: Añadimos ticketId al payload
        webSocketController.resetVotes(Map.of(
                "inviteCode", "OTHER",
                "username", "mallory",
                "sessionId", UUID.randomUUID().toString(),
                "ticketId", ticketId.toString()
        ), headerAccessor);

        // 🔄 CORREGIDO: Verificaciones actualizadas
        verify(voteService).resetVotes(sessionId, ticketId, participantId);
        verify(voteService).getVotes(sessionId, ticketId, participantId);
        verify(voteService).getVoteStatus(sessionId, ticketId);
        verify(sessionService).getSessionByCode("ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/votes", (Object) votingResults);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/vote-status", (Object) Map.of());
    }

    @Test
    void finish_broadcastsEmptyParticipantsAndFinishedState() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "FINISHED", 0L);
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1", "host");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo(sessionId, participantId, "host", "ABC123")));
        @SuppressWarnings("unchecked")
        Object castedResponse = sessionResponse;
        when(sessionService.finishSession("host", "ABC123")).thenReturn((SessionResponse) castedResponse);

        webSocketController.finish(Map.of("inviteCode", "OTHER", "username", "mallory"), headerAccessor);

        verify(sessionService).finishSession("host", "ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", (Object) List.of());
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", (Object) sessionResponse);
    }

    private Participant buildParticipant(String username, Participant.Role role) {
        User user = User.builder().id(UUID.randomUUID()).username(username).build();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").build();

        return Participant.builder().id(UUID.randomUUID()).gameSession(session).user(user).role(role).build();
    }

    private SimpMessageHeaderAccessor buildHeaderAccessor(String sessionId, String username) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        return headerAccessor;
    }

    private SessionResponse buildSessionResponse(String code, String status, long participantCount) {
        return new SessionResponse(UUID.randomUUID(), code, "Sprint Planning", "host", status, participantCount,
                new Timestamp(System.currentTimeMillis()));
    }
}