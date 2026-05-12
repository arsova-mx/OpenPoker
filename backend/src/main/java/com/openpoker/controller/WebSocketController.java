package com.openpoker.controller;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.WebSocketJoinSessionRequest;
import com.openpoker.dto.WebSocketLeaveSessionRequest;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.Participant;
import com.openpoker.service.GameSessionService;
import com.openpoker.service.VoteService;
import com.openpoker.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WebSocketController {
    private final GameSessionService service;
    private final VoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    @MessageMapping("/session.join")
    public void join(WebSocketJoinSessionRequest payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            service.joinSession(payload.username(), payload.inviteCode());

            sessionRegistry.register(headerAccessor.getSessionId(), payload.username(), payload.inviteCode());

            List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(payload.inviteCode()));

            messagingTemplate.convertAndSend("/topic/session/" + payload.inviteCode() + "/participants", participants);
            messagingTemplate.convertAndSend("/topic/session/" + payload.inviteCode() + "/state", service.getSessionByCode(payload.inviteCode()));
        } catch (RuntimeException ex) {
            publishError(payload.inviteCode(), "session.join", ex);
        }
    }

    @MessageMapping("/session.leave")
    public void leave(WebSocketLeaveSessionRequest payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            service.leaveSession(payload.username(), payload.inviteCode());

            sessionRegistry.unregister(headerAccessor.getSessionId());

            List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(payload.inviteCode()));

            messagingTemplate.convertAndSend("/topic/session/" + payload.inviteCode() + "/participants", participants);
            messagingTemplate.convertAndSend("/topic/session/" + payload.inviteCode() + "/state", service.getSessionByCode(payload.inviteCode()));
        } catch (RuntimeException ex) {
            publishError(payload.inviteCode(), "session.leave", ex);
        }
    }

    @MessageMapping("/session.vote")
    public void vote(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;
        String cardValue = payload.get("cardValue");

        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();
            String username = sessionInfo.username();

            voteService.castVote(username, inviteCode, new CastVoteRequest(cardValue));

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", voteService.getVotes(inviteCode, username));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.vote", ex);
        }
    }

    @MessageMapping("/session.reveal")
    public void reveal(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();
            String username = sessionInfo.username();

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/votes", voteService.revealVotes(username, inviteCode));
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", service.getSessionByCode(inviteCode));
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.reveal", ex);
        }
    }

    @MessageMapping("/session.reset-votes")
    public void resetVotes(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();
            String username = sessionInfo.username();
            var sessionState = service.getSessionByCode(inviteCode);
            String resolvedInviteCode = sessionState.sessionCode();

            voteService.resetVotes(username, sessionState.id());

            messagingTemplate.convertAndSend("/topic/session/" + resolvedInviteCode + "/votes", voteService.getVotes(resolvedInviteCode, username));
            messagingTemplate.convertAndSend("/topic/session/" + resolvedInviteCode + "/state", service.getSessionByCode(resolvedInviteCode));
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.reset-votes", ex);
        }
    }

    @MessageMapping("/session.finish")
    public void finish(Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String inviteCode = null;

        try {
            WebSocketSessionRegistry.SessionInfo sessionInfo = getRequiredSessionInfo(headerAccessor);
            inviteCode = sessionInfo.inviteCode();
            String username = sessionInfo.username();
            Object sessionState = service.finishSession(username, inviteCode);

            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/participants", List.of());
            messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/state", sessionState);
        } catch (RuntimeException ex) {
            publishError(inviteCode, "session.finish", ex);
        }
    }

    private List<WebSocketParticipantResponse> mapParticipants(List<Participant> participants) {
        return participants.stream().map(participant -> new WebSocketParticipantResponse(participant.getUser().getId(), participant.getUser().getUsername(), participant
                .getRole().name())).toList();
    }

    private WebSocketSessionRegistry.SessionInfo getRequiredSessionInfo(SimpMessageHeaderAccessor headerAccessor) {
        String wsSessionId = headerAccessor.getSessionId();

        if (wsSessionId == null || wsSessionId.isBlank()) {
            throw new IllegalStateException("No se pudo identificar la sesion WebSocket");
        }

        return sessionRegistry.get(wsSessionId).orElseThrow(() -> new IllegalStateException("Sesion WebSocket no registrada"));
    }

    private void publishError(String inviteCode, String action, RuntimeException ex) {
        if (inviteCode == null || inviteCode.isBlank()) {
            return;
        }

        Object errorPayload = Map.of("action", action, "type", ex.getClass().getSimpleName(), "message", ex.getMessage());

        messagingTemplate.convertAndSend("/topic/session/" + inviteCode + "/errors", errorPayload);
    }
}
