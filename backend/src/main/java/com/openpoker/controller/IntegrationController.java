package com.openpoker.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.openpoker.dto.ExportEstimationRequest;
import com.openpoker.dto.GitHubIssueDto;
import com.openpoker.dto.ImportGitHubIssuesRequest;
import com.openpoker.dto.QueryGitHubIssuesRequest;
import com.openpoker.dto.TicketResponseDTO;
import com.openpoker.entity.Ticket;
import com.openpoker.service.GitHubImportExportService;
import com.openpoker.service.GitHubIntegrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/integrations/github")
@RequiredArgsConstructor
public class IntegrationController {

    private final GitHubIntegrationService gitHubService;
    private final GitHubImportExportService importExportService;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. Consultar issues del repositorio vía POST (Evita exponer el PAT en la URL)
    @PostMapping("/issues/query")
    public ResponseEntity<List<GitHubIssueDto>> queryIssues(@RequestBody QueryGitHubIssuesRequest request) {
        return ResponseEntity.ok(gitHubService.fetchOpenIssues(request.getRepo(), request.getPersonalAccessToken()));
    }

    // 2. Importar los issues seleccionados a la sesión de forma atómica
    @PostMapping("/import")
    public ResponseEntity<List<TicketResponseDTO>> importIssues(
            @AuthenticationPrincipal String username,
            @RequestBody ImportGitHubIssuesRequest request) {

        List<Ticket> savedTickets = importExportService.importIssues(username, request);

        if (!savedTickets.isEmpty()) {
            String sessionCode = savedTickets.get(0).getGameSession().getSessionCode();
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionCode + "/tickets-imported",
                    Map.of("message", "Tickets importados desde GitHub", "count", savedTickets.size())
            );
        }

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

        importExportService.exportEstimation(username, ticketId, request);
        return ResponseEntity.ok(Map.of("message", "Estimación exportada exitosamente"));
    }
}