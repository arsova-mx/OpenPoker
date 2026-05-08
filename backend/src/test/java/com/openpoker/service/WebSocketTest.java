package com.openpoker.service;

import com.openpoker.controller.WebSocketController;
import com.openpoker.dto.WebSocketJoinSessionRequest;
import com.openpoker.dto.WebSocketLeaveSessionRequest;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketTest {

    @Mock
    private GameSessionService sessionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketSessionRegistry sessionRegistry;

    @InjectMocks
    private WebSocketController webSocketController;

    @Test
    void join_broadcastsParticipantsToInviteCodeTopic() {
        Participant participant = buildParticipant("alice", Participant.Role.VOTER);
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(
                participant.getUser().getId(),
                "alice",
                "VOTER"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionService.getParticipants("ABC123")).thenReturn(participants);

        webSocketController.join(new WebSocketJoinSessionRequest("ABC123", "alice"), headerAccessor);

        verify(sessionService).joinSession("alice", "ABC123");
        verify(sessionService).getParticipants("ABC123");
        verify(sessionRegistry).register("ws-session-1", "alice", "ABC123");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", expected);
    }

    @Test
    void leave_broadcastsParticipantsToInviteCodeTopic() {
        Participant participant = buildParticipant("host", Participant.Role.HOST);
        List<Participant> participants = List.of(participant);
        List<WebSocketParticipantResponse> expected = List.of(new WebSocketParticipantResponse(
                participant.getUser().getId(),
                "host",
                "HOST"));
        SimpMessageHeaderAccessor headerAccessor = buildHeaderAccessor("ws-session-1");

        when(sessionService.getParticipants("ABC123")).thenReturn(participants);

        webSocketController.leave(new WebSocketLeaveSessionRequest("ABC123", "alice"), headerAccessor);

        verify(sessionService).leaveSession("alice", "ABC123");
        verify(sessionService).getParticipants("ABC123");
        verify(sessionRegistry).unregister("ws-session-1");
        verify(messagingTemplate).convertAndSend("/topic/session/ABC123/participants", expected);
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
}
