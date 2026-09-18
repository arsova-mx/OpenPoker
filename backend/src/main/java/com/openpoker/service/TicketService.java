package com.openpoker.service;

import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.controller.TicketController.CreateTicketDTO;
import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;
import com.openpoker.entity.User;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.globalexception.UsernameIsNotParticipantSessionException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository; // 👈 Inyección requerida para autenticar por username

    @Transactional
    public TicketResponseDTO createTicket(String username, CreateTicketDTO dto) {
        GameSession session = sessionRepository.findById(dto.getGameSessionId())
                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));

        // 🔒 Autorización: Validar que quien crea sea HOST de la sesión
        validateHost(session, username);

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .tittle(dto.getTitle())
                .description(dto.getDescription())
                .gameSession(session)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        return new TicketResponseDTO(
            savedTicket.getId(),
            savedTicket.getTittle(),
            savedTicket.getDescription(),
            savedTicket.getGameSession().getId(),
            savedTicket.getStatus()
        );
    }

    @Transactional
    public TicketResponseDTO updateStatus(String username, UUID ticketId, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        // 🔒 Autorización: Validar que quien cambia el estado sea HOST de la sesión del ticket
        validateHost(ticket.getGameSession(), username);

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);

        return new TicketResponseDTO(
                updatedTicket.getId(),
                updatedTicket.getTittle(),
                updatedTicket.getDescription(),
                updatedTicket.getGameSession().getId(),
                updatedTicket.getStatus()
        );
    }

    @Transactional
    public TicketResponseDTO finishTicket(UUID ticketId, UUID participantId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(ticket.getGameSession().getId())) {
            throw new UsernameIsNotParticipantSessionException("El participante no pertenece a la sesión de este ticket");
        }

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el Host de la sala puede finalizar la estimación del ticket");
        }

        ticket.setStatus(TicketStatus.FINISHED);
        Ticket savedTicket = ticketRepository.save(ticket);

        return new TicketResponseDTO(
                savedTicket.getId(),
                savedTicket.getTittle(),
                savedTicket.getDescription(),
                savedTicket.getGameSession().getId(),
                savedTicket.getStatus()
        );
    }

    private void validateHost(GameSession session, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user)
                .orElseThrow(() -> new UsernameIsNotParticipantSessionException("No perteneces a esta sesión"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede gestionar tickets en esta sala");
        }
    }
}
