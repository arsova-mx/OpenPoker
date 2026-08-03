package com.openpoker.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.openpoker.dto.SetTimerRequest;
import com.openpoker.dto.TimerStatusDTO;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.Participant.Role;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketTimerService {

    private final TicketRepository ticketRepository;
    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public TimerStatusDTO setTimer(UUID sessionId, UUID ticketId, UUID participantId, SetTimerRequest request){
        GameSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new SessionNotFoundException("Sesion no encontrada"));
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if(!ticket.getGameSession().getId().equals(sessionId)){
            throw new IllegalArgumentException("El ticket no pertenece a la sesión");
        }

        if(!participant.getGameSession().getId().equals(sessionId)){
            throw new IllegalArgumentException("El participante no pertenece a la sesión");
        }

        if(!participant.getRole().equals(Role.HOST)){
            throw new InsufficientRoleException("Solo el HOST puede iniciar el timer");
        }

        Instant timerExpiresAt = null;
        Integer duration = request.durationSeconds();


        // 1. Si la duración es válida, calculamos expiración. Si es 0 o null, queda sin límite.
        if (duration != null && duration > 0) {
            timerExpiresAt = Instant.now().plusSeconds(duration);
        } else {
            duration = null;
        }

        ticket.setTimerExpiresAt(timerExpiresAt);
        ticket.setDurationSeconds(duration);
        ticketRepository.save(ticket);

        TimerStatusDTO dto = TimerStatusDTO.of(duration, timerExpiresAt);

        // Notificar por WebSocket
        messagingTemplate.convertAndSend("/topic/session/" + session.getSessionCode() + "/timer", dto);

        return dto;
    }

}
