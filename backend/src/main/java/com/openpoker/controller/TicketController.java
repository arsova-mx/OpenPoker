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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public TicketResponseDTO createTicket(@RequestBody CreateTicketDTO dto) {
        GameSession session = sessionRepository.findById(dto.getGameSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .tittle(dto.getTitle()) // Cambiado a 'title' si corregiste el typo de 'tittle'
                .description(dto.getDescription())
                .gameSession(session)
                .build();
        
        Ticket savedTicket = ticketRepository.save(ticket);

        return new TicketResponseDTO(
            savedTicket.getId(),
            savedTicket.getTittle(),
            savedTicket.getDescription(),
            savedTicket.getGameSession().getId()
        );
    }

    // 🚀 GET /api/tickets/session/{sessionId} -> Traer el backlog de la sala
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<TicketResponseDTO>> getTicketsBySession(@PathVariable UUID sessionId) {
        List<Ticket> tickets = ticketRepository.findByGameSessionId(sessionId);

        // 2. Mapeamos de forma limpia a la lista de DTOs seguros
        List<TicketResponseDTO> response = tickets.stream()
                .map(t -> new TicketResponseDTO(
                    t.getId(),
                    t.getTittle(),       // Mantén el typo 'getTittle()' si aún está así en tu entidad original
                    t.getDescription(),
                    t.getGameSession().getId()
                ))
                .toList();

        // 3. Retornamos la lista sanitizada y libre de ciclos JSON
        return ResponseEntity.ok(response);
    }

    // Pequeño DTO interno rápido para mapear el JSON de Postman
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
        
        // Delegamos la lógica al servicio
        TicketResponseDTO response = ticketService.updateStatus(ticketId, newStatus);
        
        return ResponseEntity.ok(response);
    }

}
