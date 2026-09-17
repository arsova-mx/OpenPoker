package com.openpoker.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.service.TicketService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;
    private final GameSessionRepository sessionRepository;
    private final TicketService ticketService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public TicketResponseDTO createTicket(@RequestBody CreateTicketDTO dto) {
        GameSession session = sessionRepository.findById(dto.getGameSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .tittle(dto.getTitle())
                .description(dto.getDescription())
                .gameSession(session)
                .build();
        
        Ticket savedTicket = ticketRepository.save(ticket);

        TicketResponseDTO response = new TicketResponseDTO(
            savedTicket.getId(),
            savedTicket.getTittle(),
            savedTicket.getDescription(),
            savedTicket.getGameSession().getId(),
            savedTicket.getStatus()
        );

        // 🚀 Notificar en vivo a toda la sala que se creó un ticket
        messagingTemplate.convertAndSend(
            "/topic/session/" + session.getSessionCode() + "/ticket-updated",
            response
        );

        return response;
    }

    // 🚀 GET /api/tickets/session/{sessionId} -> Traer el backlog de la sala
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<TicketResponseDTO>> getTicketsBySession(@PathVariable UUID sessionId) {
        List<Ticket> tickets = ticketRepository.findByGameSessionId(sessionId);

        List<TicketResponseDTO> response = tickets.stream()
                .map(t -> new TicketResponseDTO(
                    t.getId(),
                    t.getTittle(),
                    t.getDescription(),
                    t.getGameSession().getId(),
                    t.getStatus()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    // DTO interno para el request body de creación
    @lombok.Data
    public static class CreateTicketDTO {
        private String title;
        private String description;
        private UUID gameSessionId;
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponseDTO> updateTicketStatus(
            @PathVariable UUID ticketId, 
            @RequestParam TicketStatus newStatus) {
        
        TicketResponseDTO response = ticketService.updateStatus(ticketId, newStatus);

        // 🚀 Notificar en vivo el cambio de estado (ej: al pasar a VOTING)
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            String inviteCode = ticket.getGameSession().getSessionCode();
            messagingTemplate.convertAndSend(
                "/topic/session/" + inviteCode + "/ticket-updated",
                response
            );
        });
        
        return ResponseEntity.ok(response);
    }
}
