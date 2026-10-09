package com.openpoker.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.openpoker.dto.SessionHistorySummaryDto;
import com.openpoker.dto.TicketHistoryDto;
import com.openpoker.entity.User;
import com.openpoker.repository.UserRepository;
import com.openpoker.service.HistoryService;
import com.openpoker.service.SessionAccessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final UserRepository userRepository;
    private final SessionAccessService sessionAccessService;

    /**
     * Obtiene el listado paginado de sesiones en las que el usuario ha participado o ha sido host.
     * Soporta el parámetro ?filter=ALL (default), HOST o PARTICIPANT
     */
    @GetMapping("/sessions")
    public ResponseEntity<Page<SessionHistorySummaryDto>> getMySessions(
            @AuthenticationPrincipal String username,
            @RequestParam(defaultValue = "ALL") String filter,
            @PageableDefault(size = 10) Pageable pageable) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        Page<SessionHistorySummaryDto> history = historyService.getUserSessions(user.getId(), filter, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Trae el desglose histórico de tickets de una sesión.
     * Valida mediante SessionAccessService que el usuario haya tenido acceso a la sala.
     */
    @GetMapping("/sessions/{sessionId}/tickets")
    public ResponseEntity<List<TicketHistoryDto>> getSessionTicketsHistory(
            @AuthenticationPrincipal String username,
            @PathVariable UUID sessionId) {

        sessionAccessService.requireParticipant(sessionId, username);

        List<TicketHistoryDto> tickets = historyService.getSessionTicketsHistory(sessionId);
        return ResponseEntity.ok(tickets);
    }
}