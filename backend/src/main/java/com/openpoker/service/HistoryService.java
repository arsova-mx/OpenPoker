package com.openpoker.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.dto.SessionHistorySummaryDto;
import com.openpoker.dto.SessionTicketCountsProjection;
import com.openpoker.dto.TicketHistoryDto;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final GameSessionRepository gameSessionRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public Page<SessionHistorySummaryDto> getUserSessions(UUID userId, String filter, Pageable pageable) {
        Page<GameSession> sessionsPage;

        if ("HOST".equalsIgnoreCase(filter)) {
            sessionsPage = gameSessionRepository.findByHostIdOrderByCreatedAtDesc(userId, pageable);
        } else if ("PARTICIPANT".equalsIgnoreCase(filter)) {
            sessionsPage = gameSessionRepository.findSessionsWhereUserParticipated(userId, pageable);
        } else {
            sessionsPage = gameSessionRepository.findUserSessionHistory(userId, pageable);
        }

        // Si la página no tiene resultados, retornamos la página vacía directamente
        if (sessionsPage.isEmpty()) {
            return sessionsPage.map(s -> null);
        }

        // 1. Extraemos los IDs de las sesiones de la página actual
        List<UUID> sessionIds = sessionsPage.getContent().stream()
                .map(GameSession::getId)
                .toList();

        // 2. Ejecutamos una única consulta en batch para traer los conteos agrupados
        Map<UUID, SessionTicketCountsProjection> countsMap = ticketRepository
                .countTicketStatsBySessionIds(sessionIds)
                .stream()
                .collect(Collectors.toMap(
                        SessionTicketCountsProjection::getSessionId,
                        Function.identity()
                ));

        // 3. Mapeamos cada sesión asociando los conteos en memoria (0 consultas extras a BD)
        return sessionsPage.map(session -> {
            SessionTicketCountsProjection stats = countsMap.get(session.getId());
            long total = stats != null ? stats.getTotalTickets() : 0L;
            long estimated = stats != null ? stats.getEstimatedTickets() : 0L;

            return SessionHistorySummaryDto.builder()
                    .sessionId(session.getId())
                    .sessionCode(session.getSessionCode())
                    .name(session.getName())
                    .status(session.getStatus())
                    .createdAt(session.getCreatedAt())
                    .closedAt(session.getClosedAt())
                    .isHost(session.getHost().getId().equals(userId))
                    .totalTickets(total)
                    .estimatedTickets(estimated)
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryDto> getSessionTicketsHistory(UUID sessionId) {
        List<Ticket> tickets = ticketRepository.findByGameSessionIdOrderByPositionAsc(sessionId);

        return tickets.stream().map(t -> TicketHistoryDto.builder()
                .id(t.getId())
                .title(t.getTittle())
                .description(t.getDescription())
                .status(t.getStatus())
                .currentRound(t.getCurrentRound())
                .finishedAt(t.getFinishedAt())
                .estimatedValue(t.getEstimatedCard() != null ? String.valueOf(t.getEstimatedCard().getValue()) : null)
                .build()
        ).collect(Collectors.toList());
    }
}