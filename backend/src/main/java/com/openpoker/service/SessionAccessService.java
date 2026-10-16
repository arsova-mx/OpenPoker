package com.openpoker.service;

import com.openpoker.entity.GameSession;
import com.openpoker.entity.Ticket;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.globalexception.TicketNotFoundException;
import com.openpoker.globalexception.UsernameIsNotParticipantSessionException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Punto único para verificar que un usuario autenticado participa en una sala.
 * Lo usan los endpoints REST (tickets, comentarios) y la autorización de suscripciones STOMP.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionAccessService {
    private final GameSessionRepository sessionRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public boolean isParticipant(String sessionCode, String username) {
        if (sessionCode == null || username == null) {
            return false;
        }
        return sessionRepository.findBySessionCode(sessionCode)
                .map(session -> isParticipant(session, username))
                .orElse(false);
    }

    public GameSession requireParticipant(UUID sessionId, String username) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));
        assertParticipant(session, username);
        return session;
    }

    public Ticket requireParticipantOfTicket(UUID ticketId, String username) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket no encontrado"));
        assertParticipant(ticket.getGameSession(), username);
        return ticket;
    }

    private void assertParticipant(GameSession session, String username) {
        if (!isParticipant(session, username)) {
            throw new UsernameIsNotParticipantSessionException("No participas en esta sesión");
        }
    }

    private boolean isParticipant(GameSession session, String username) {
        if (username == null) {
            return false;
        }
        return userRepository.findByUsername(username)
                .flatMap(user -> participantRepository.findByGameSessionAndUser(session, user))
                .isPresent();
    }

    public void requireHost(UUID sessionId, String username) {
        GameSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));

        if (!session.getHost().getUsername().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: solo el Host puede realizar esta acción");
        }
    }
}
