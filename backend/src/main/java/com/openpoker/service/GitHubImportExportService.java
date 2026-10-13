package com.openpoker.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.openpoker.dto.ExportEstimationRequest;
import com.openpoker.dto.GitHubIssueDto;
import com.openpoker.dto.ImportGitHubIssuesRequest;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitHubImportExportService {

    private final GameSessionRepository sessionRepository;
    private final TicketRepository ticketRepository;
    private final SessionAccessService sessionAccessService;
    private final GitHubIntegrationService gitHubService;

    @Transactional
    public List<Ticket> importIssues(String username, ImportGitHubIssuesRequest request) {
        sessionAccessService.requireHost(request.getSessionId(), username);

        GameSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));

        List<Ticket> savedTickets = new ArrayList<>();
        int currentPosition = (int) ticketRepository.countByGameSessionId(session.getId());

        for (GitHubIssueDto issue : request.getIssues()) {
            String title = formatTitle("#" + issue.getNumber() + " " + (issue.getTitle() != null ? issue.getTitle() : ""));
            String description = summarizeIssueBody(issue.getBody());

            Ticket ticket = Ticket.builder()
                    .gameSession(session)
                    .tittle(title)
                    .description(description)
                    .status(TicketStatus.WAITING)
                    .createdAt(Instant.now())
                    .currentRound(1)
                    .position(++currentPosition)
                    .externalSource("GITHUB")
                    .externalId(String.valueOf(issue.getNumber()))
                    .externalUrl(issue.getHtmlUrl())
                    .externalRepo(request.getRepo())
                    .build();

            savedTickets.add(ticketRepository.save(ticket));
        }

        return savedTickets;
    }

    @Transactional(readOnly = true)
    public void exportEstimation(String username, UUID ticketId, ExportEstimationRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        sessionAccessService.requireHost(ticket.getGameSession().getId(), username);

        // Validación: El ticket debe estar finalizado
        if (ticket.getStatus() != TicketStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden exportar tickets finalizados");
        }

        if (!"GITHUB".equalsIgnoreCase(ticket.getExternalSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ticket no proviene de GitHub");
        }

        // Obtener valor acordado de forma segura
        String estimatedValue = "Consensuado (sin carta asignada)";
        if (ticket.getEstimatedCard() != null) {
            estimatedValue = String.valueOf(ticket.getEstimatedCard().getValue());
        }

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
    }

    // Truncado seguro del título para no exceder VARCHAR(255)
    private String formatTitle(String rawTitle) {
        final int MAX_TITLE_LENGTH = 250;
        if (rawTitle == null || rawTitle.isBlank()) {
            return "Ticket sin título";
        }
        String trimmed = rawTitle.trim();
        return trimmed.length() <= MAX_TITLE_LENGTH ? trimmed : trimmed.substring(0, MAX_TITLE_LENGTH) + "...";
    }

    // Limpieza y truncado de descripción
    private String summarizeIssueBody(String body) {
        if (body == null || body.isBlank()) {
            return "Sin descripción.";
        }

        String clean = body.replaceAll("(?s)<!--.*?-->", "");
        clean = clean.replaceAll("(?s)```.*?```", "[código omitido]");
        clean = clean.replaceAll("!\\[.*?\\]\\(.*?\\)", "");
        clean = clean.replaceAll("\\r\\n|\\r", "\n").trim();

        final int MAX_CHARS = 450;
        if (clean.length() <= MAX_CHARS) {
            return clean;
        }

        int cutoff = clean.lastIndexOf(' ', MAX_CHARS);
        if (cutoff <= 0) {
            cutoff = MAX_CHARS;
        }

        return clean.substring(0, cutoff).trim() + "...";
    }
}