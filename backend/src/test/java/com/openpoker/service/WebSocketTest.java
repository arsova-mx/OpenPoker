package com.openpoker.service;

import com.openpoker.controller.WebSocketController;
import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.dto.WebSocketJoinSessionRequest;
import com.openpoker.dto.WebSocketLeaveSessionRequest;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import com.openpoker.globalexception.InvalidVoteValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private VoteService voteService;

    @Mock
    private WebSocketSessionRegistry sessionRegistry;

    @InjectMocks
    private WebSocketController webSocketController;



    @Test
    void join_broadcastsParticipantsToInviteCodeTopic() {
        Participant participant = buildParticipant("alice", Participant.Role.VOTER);
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(participant.getUser().getId(), "alice", "VOTER"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "VOTING", 1L);

        when(sessionService.getParticipants("ABC123")).thenReturn(participants);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);

        webSocketController.join(new WebSocketJoinSessionRequest("ABC123", "alice"), headerAccessor);

        verify(sessionService).joinSession("alice", "ABC123");
        verify(sessionService).getParticipants("ABC123");
        verify(sessionService).getSessionByCode("ABC123");
        verify(sessionRegistry).register("ws-session-1", "alice", "ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", expected);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", sessionResponse);
    }

    @Test
    void leave_broadcastsParticipantsToInviteCodeTopic() {
        Participant participant = buildParticipant("host", Participant.Role.HOST);
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(participant.getUser().getId(), "host", "HOST"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "VOTING", 1L);

        when(sessionService.getParticipants("ABC123")).thenReturn(participants);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);

        webSocketController.leave(new WebSocketLeaveSessionRequest("ABC123", "alice"), headerAccessor);

        verify(sessionService).leaveSession("alice", "ABC123");
        verify(sessionService).getParticipants("ABC123");
        verify(sessionService).getSessionByCode("ABC123");
        verify(sessionRegistry).unregister("ws-session-1");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", expected);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", sessionResponse);
    }

    @Test
    void vote_broadcastsVotesAndState() {
        VotingResultsResponse votingResults = new VotingResultsResponse("ABC123", List.of(), false);
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "WAITING", 2L);
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo("alice", "ABC123")));
        when(voteService.getVotes("ABC123", "alice")).thenReturn(votingResults);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);

        webSocketController.vote(Map.of("inviteCode", "OTHER", "username", "mallory", "cardValue", "5"), headerAccessor);

        verify(voteService).castVote("alice", "ABC123", new com.openpoker.dto.CastVoteRequest("5"));
        verify(voteService).getVotes("ABC123", "alice");
        verify(sessionService).getSessionByCode("ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/votes", votingResults);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", sessionResponse);
    }

    @Test
    void reveal_broadcastsRevealedVotesAndState() {
        VotingResultsResponse votingResults = new VotingResultsResponse("ABC123", List.of(), true);
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "REVEALED", 2L);
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo("host", "ABC123")));
        when(voteService.revealVotes("host", "ABC123")).thenReturn(votingResults);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);

        webSocketController.reveal(Map.of("inviteCode", "OTHER", "username", "mallory"), headerAccessor);

        verify(voteService).revealVotes("host", "ABC123");
        verify(sessionService).getSessionByCode("ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/votes", votingResults);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", sessionResponse);
    }

    @Test
    void vote_broadcastsErrorWhenVoteFails() {
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo("alice", "ABC123")));
        when(voteService.castVote("alice", "ABC123", new com.openpoker.dto.CastVoteRequest("999")))
                .thenThrow(new InvalidVoteValueException("Valor invalido"));
        Object errorPayload = Map.of(
                "action", "session.vote",
                "type", "InvalidVoteValueException",
                "message", "Valor invalido"
        );

        webSocketController.vote(Map.of("inviteCode", "OTHER", "username", "mallory", "cardValue", "999"), headerAccessor);

        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/errors", errorPayload);
        verify(voteService, never()).getVotes("ABC123", "alice");
        verify(sessionService, never()).getSessionByCode("ABC123");
    }

    @Test
    void resetVotes_broadcastsClearedVotesAndState() {
        UUID sessionId = UUID.randomUUID();
        VotingResultsResponse votingResults = new VotingResultsResponse("ABC123", List.of(), false);
        SessionResponse sessionResponse = new SessionResponse(sessionId, "ABC123", "Sprint Planning", "host",
                "VOTING", 2L, new Timestamp(System.currentTimeMillis()));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo("host", "ABC123")));
        when(voteService.getVotes("ABC123", "host")).thenReturn(votingResults);
        when(sessionService.getSessionByCode("ABC123")).thenReturn(sessionResponse);

        webSocketController.resetVotes(Map.of(
                "inviteCode", "OTHER",
                "username", "mallory",
                "sessionId", UUID.randomUUID().toString()
        ), headerAccessor);

        verify(voteService).resetVotes("host", sessionId);
        verify(voteService).getVotes("ABC123", "host");
        verify(sessionService, times(2)).getSessionByCode("ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/votes", votingResults);
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", sessionResponse);
    }

    @Test
    void finish_broadcastsEmptyParticipantsAndFinishedState() {
        SessionResponse sessionResponse = buildSessionResponse("ABC123", "FINISHED", 0L);
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionRegistry.get("ws-session-1")).thenReturn(Optional.of(new WebSocketSessionRegistry.SessionInfo("host", "ABC123")));
        when(sessionService.finishSession("host", "ABC123")).thenReturn(sessionResponse);

        webSocketController.finish(Map.of("inviteCode", "OTHER", "username", "mallory"), headerAccessor);

        verify(sessionService).finishSession("host", "ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", List.of());
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/state", sessionResponse);
    }

    private Participant buildParticipant(String username, Participant.Role role) {
        User user = User.builder().id(UUID.randomUUID()).username(username).build();
        GameSession session = GameSession.builder().id(UUID.randomUUID()).sessionCode("ABC123").build();

        return Participant.builder().id(UUID.randomUUID()).gameSession(session).user(user).role(role).build();
    }

    private SimpMessageHeaderAccessor buildHeaderAccessor(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);
        return headerAccessor;
    }

    private SessionResponse buildSessionResponse(String code, String status, long participantCount) {
        return new SessionResponse(UUID.randomUUID(), code, "Sprint Planning", "host", status, participantCount,
                new Timestamp(System.currentTimeMillis()));
    }
}
