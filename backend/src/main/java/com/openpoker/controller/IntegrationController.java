package com.openpoker.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.dto.ExportEstimationRequest;
import com.openpoker.dto.GitHubIssueDto;
import com.openpoker.dto.ImportGitHubIssuesRequest;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.service.GitHubIntegrationService;
import com.openpoker.service.SessionAccessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/integrations/github")
@RequiredArgsConstructor
public class IntegrationController {

    private final GitHubIntegrationService gitHubService;
    private final GameSessionRepository sessionRepository;
    private final TicketRepository ticketRepository;
    private final SessionAccessService sessionAccessService;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. Consultar issues del repositorio
    @GetMapping("/issues")
    public ResponseEntity<List<GitHubIssueDto>> getIssues(
            @RequestParam String repo,
            @RequestParam(required = false) String token) {
        return ResponseEntity.ok(gitHubService.fetchOpenIssues(repo, token));
    }

    // 2. Importar los issues seleccionados a la sesión
    @PostMapping("/import")
    public ResponseEntity<List<TicketResponseDTO>> importIssues(
            @AuthenticationPrincipal String username,
            @RequestBody ImportGitHubIssuesRequest request) {

        sessionAccessService.requireHost(request.getSessionId(), username);
        GameSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        List<Ticket> savedTickets = new ArrayList<>();
        int currentPosition = (int) ticketRepository.countByGameSessionId(session.getId());

        for (GitHubIssueDto issue : request.getIssues()) {
            Ticket ticket = Ticket.builder()
                    .gameSession(session)
                    .tittle("#" + issue.getNumber() + " " + issue.getTitle())
                    .description(issue.getBody())
                    .status(TicketStatus.WAITING)
                    .position(++currentPosition)
                    .externalSource("GITHUB")
                    .externalId(String.valueOf(issue.getNumber()))
                    .externalUrl(issue.getHtmlUrl())
                    .externalRepo(request.getRepo())
                    .build();

            savedTickets.add(ticketRepository.save(ticket));
        }

        // Notificar vía WebSocket a toda la sala
        messagingTemplate.convertAndSend(
                "/topic/session/" + session.getSessionCode() + "/tickets-imported",
                Map.of("message", "Tickets importados desde GitHub", "count", savedTickets.size())
        );

        List<TicketResponseDTO> response = savedTickets.stream()
                .map(t -> new TicketResponseDTO(
                        t.getId(),
                        t.getTittle(),
                        t.getDescription(),
                        t.getGameSession().getId(),
                        t.getStatus(),
                        t.getCurrentRound()
                )).toList();

        return ResponseEntity.ok(response);
    }

    // 3. Exportar estimación como comentario al issue
    @PostMapping("/tickets/{ticketId}/export")
    public ResponseEntity<Map<String, String>> exportEstimation(
            @AuthenticationPrincipal String username,
            @PathVariable UUID ticketId,
            @RequestBody ExportEstimationRequest request) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        sessionAccessService.requireHost(ticket.getGameSession().getId(), username);

        if (!"GITHUB".equalsIgnoreCase(ticket.getExternalSource())) {
            throw new IllegalStateException("El ticket no proviene de GitHub");
        }

        String estimatedValue = ticket.getEstimatedCard() != null 
                ? String.valueOf(ticket.getEstimatedCard().getValue()) 
                : "No definido";

        String comment = (request.getCustomComment() != null && !request.getCustomComment().isBlank())
                ? request.getCustomComment()
                : "### 🃏 Estimación de Poker de Planificación (OpenPoker)\n\n"
                  + "- **Estimado final:** `" + estimatedValue + "`\n"
                  + "- **Rondas de votación:** " + ticket.getCurrentRound() + "\n\n"
                  + "_Comentario generado automáticamente desde OpenPoker._";

        gitHubService.postEstimationComment(
                ticket.getExternalRepo(),
                ticket.getExternalId(),
                comment,
                request.getPersonalAccessToken()
        );

        return ResponseEntity.ok(Map.of("message", "Estimación exportada exitosamente"));
    }
}