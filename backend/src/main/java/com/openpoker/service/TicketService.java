package com.openpoker.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.controller.TicketController;
import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final GameSessionRepository sessionRepository;

    @Transactional
    public TicketResponseDTO createTicket(TicketController.CreateTicketDTO dto) {
        GameSession session = sessionRepository.findById(dto.getGameSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .tittle(dto.getTitle())
                .description(dto.getDescription())
                .gameSession(session)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // Aquí ocurre la "magia": convertimos la entidad a DTO y rompemos el ciclo
        return new TicketResponseDTO(
            savedTicket.getId(),
            savedTicket.getTittle(),
            savedTicket.getDescription(),
            savedTicket.getGameSession().getId()
        );
    }

    @Transactional
    public TicketResponseDTO updateStatus(UUID ticketId, TicketStatus newStatus) {
        // 1. Buscamos el ticket en la base de datos
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        // 2. Cambiamos el estado
        ticket.setStatus(newStatus);

        // 3. Guardamos los cambios
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 4. Mapeamos y retornamos el mismo DTO que usa tu controlador
        return new TicketResponseDTO(
                updatedTicket.getId(),
                updatedTicket.getTittle(), // Mantén tu propiedad 'tittle'
                updatedTicket.getDescription(),
                updatedTicket.getGameSession().getId()
        );
    }

}
