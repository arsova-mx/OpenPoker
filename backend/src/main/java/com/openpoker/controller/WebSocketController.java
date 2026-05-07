package com.openpoker.controller;

import com.openpoker.dto.WebSocketJoinSessionRequest;
import com.openpoker.dto.WebSocketLeaveSessionRequest;
import com.openpoker.dto.WebSocketParticipantResponse;
import com.openpoker.entity.Participant;
import com.openpoker.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WebSocketController {
    private final GameSessionService service;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/session.join")
    public void join(WebSocketJoinSessionRequest payload) {
        service.joinSession(payload.username(), payload.inviteCode());

        List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(payload.inviteCode()));

        messagingTemplate.convertAndSend("/topic/session/" + payload.inviteCode() + "/participants", participants);
    }

    @MessageMapping("/session.leave")
    public void leave(WebSocketLeaveSessionRequest payload) {
        service.leaveSession(payload.username(), payload.inviteCode());

        List<WebSocketParticipantResponse> participants = mapParticipants(service.getParticipants(payload.inviteCode()));

        messagingTemplate.convertAndSend("/topic/session/" + payload.inviteCode() + "/participants", participants);
    }

    private List<WebSocketParticipantResponse> mapParticipants(List<Participant> participants) {
        return participants.stream()
                .map(participant -> new WebSocketParticipantResponse(
                        participant.getUser().getId(),
                        participant.getUser().getUsername(),
                        participant.getRole().name()))
                .toList();
    }
}
