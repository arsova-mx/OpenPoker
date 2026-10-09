package com.openpoker.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.dto.SessionHistorySummaryDto;
import com.openpoker.dto.TicketHistoryDto;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;
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

        return sessionsPage.map(session -> {
            long totalTickets = ticketRepository.countByGameSessionId(session.getId());
            long estimatedTickets = ticketRepository.countByGameSessionIdAndStatus(session.getId(), TicketStatus.FINISHED);

            return SessionHistorySummaryDto.builder()
                    .sessionId(session.getId())
                    .sessionCode(session.getSessionCode())
                    .name(session.getName())
                    .status(session.getStatus())
                    .createdAt(session.getCreatedAt())
                    .closedAt(session.getClosedAt())
                    .isHost(session.getHost().getId().equals(userId))
                    .totalTickets(totalTickets)
                    .estimatedTickets(estimatedTickets)
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