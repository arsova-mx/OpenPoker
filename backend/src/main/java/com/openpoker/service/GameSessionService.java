package com.openpoker.service;

import com.openpoker.entity.*;
import com.openpoker.globalexception.*;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openpoker.sessioncodegenerator.SessionCodeGenerator;
import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.JoinSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionService {
    private static final int MAX_SESSION_CODE_RETRIES = 10;

    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionCodeGenerator codeGenerator;
    private final VotingDeckRepository deckRepository;
   

    @Transactional
    public SessionResponse createSession(String username, CreateSessionRequest request, UUID deckId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        VotingDeck deck = deckRepository.findById(deckId).orElseThrow(() -> new DeckNotFoundException("Deck no encontrado"));
        return createSessionWithDeck(user, request, deck);
    }

    @Transactional
    public SessionResponse createSessionWithDefaultDeck(String username, CreateSessionRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        VotingDeck deck = deckRepository.findBySeriesType(CardSeries.FIBONACCI)
                .orElseThrow(() -> new DeckNotFoundException("Deck Fibonacci no encontrado"));
        return createSessionWithDeck(user, request, deck);
    }

    private SessionResponse createSessionWithDeck(User user, CreateSessionRequest request, VotingDeck deck) {
        GameSession session = null;

        for (int attempt = 0; attempt < MAX_SESSION_CODE_RETRIES; attempt++) {
            String code = codeGenerator.generate();
            
            GameSession candidate = GameSession.builder()
                .sessionCode(code)
                .name(request.name())
                .host(user)
                .deck(deck)
                .status(SessionStatus.ACTIVE)
                .build();

            try {
                session = sessionRepository.saveAndFlush(candidate);
                break;
            } catch (DataIntegrityViolationException ex) {
                if (attempt == MAX_SESSION_CODE_RETRIES - 1) {
                    throw ex;
                }
            }
        }

        if (session == null) {
            throw new IllegalStateException("No se pudo crear la sesion");
        }

        Participant host = Participant.builder().gameSession(session).user(user).role(Participant.Role.HOST).build();
        participantRepository.save(host);

        return mapToResponse(session, user.getUsername());
    }
    @Transactional
    public SessionResponse getSessionByCode(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        return mapToResponse(session, session.getHost().getUsername());
    }

    /**
     * 🔒 Método centralizado para validar que la sesión se encuentre activa.
     * Si está finalizada, lanza SessionFinishedException (mapeada a HTTP 409 Conflict).
     */
    public void validateSessionIsActive(GameSession session) {
        if (session == null || session.getStatus() == SessionStatus.FINISHED) {
            throw new SessionFinishedException("La sesión ya ha finalizado y no permite nuevas interacciones.");
        }
    }

    @Transactional
    public SessionResponse joinSession(JoinSessionRequest request) {
        GameSession session = sessionRepository.findBySessionCode(request.code())
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        // Validar estado de la sesión usando el método centralizado
        validateSessionIsActive(session);

        Participant participant;

        if (request.username() != null && !request.username().isBlank()) {
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            var existingParticipant = participantRepository.findByGameSessionAndUser(session, user);
            if (existingParticipant.isPresent()) {
                participant = existingParticipant.get(); 
                participant.setLeftAt(null);
                participant = participantRepository.save(participant);
            } else {
                Participant.Role role = (session.getHost() != null && session.getHost().getId().equals(user.getId())) 
                        ? Participant.Role.HOST 
                        : Participant.Role.VOTER;

                participant = Participant.builder()
                        .gameSession(session)
                        .user(user)
                        .guestDisplayName(null)
                        .role(role)
                        .build();
                
                participant = participantRepository.save(participant);
            }

        // Los invitados entran por joinAsGuest (POST /api/sessions/{code}/guests) y reciben un token propio
        } else {
            throw new IllegalArgumentException("Se requiere un usuario autenticado para unirse con este endpoint.");
        }

        String hostName = getHostDisplayName(session);
        return mapToResponse(session, hostName);
    }
    
    private String getHostDisplayName(GameSession session) {
        if (session.getHost() == null) {
            return "Host Anónimo";
        }
        return session.getHost().getUsername();
    }

    /**
     * Entrada de invitados: siempre crea un participante nuevo. El nombre no puede repetir el de otro
     * participante de la sala ni el de un usuario registrado, para que nadie se haga pasar por otro.
     */
    @Transactional
    public Participant joinAsGuest(String code, String guestName) {
        GameSession session = sessionRepository.findBySessionCode(code)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        validateSessionIsActive(session);
        String displayName = guestName.trim();

        boolean takenInSession = participantRepository.findAllByGameSession(session).stream()
                .anyMatch(existing -> existing.getEffectiveName().equalsIgnoreCase(displayName));
        if (takenInSession || userRepository.existsByUsernameIgnoreCase(displayName)) {
            throw new GuestNameUnavailableException("Ese nombre ya está en uso, elige otro");
        }

        Participant guest = Participant.builder()
                .gameSession(session)
                .user(null)
                .guestDisplayName(displayName)
                .role(Participant.Role.VOTER)
                .build();
        return participantRepository.save(guest);
    }

    public SessionResponse getSessionResponse(GameSession session) {
        return mapToResponse(session, getHostDisplayName(session));
    }

    @Transactional
    public List<Participant> getParticipants(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        return participantRepository.findAllByGameSession(session);
    }

    @Transactional
    public SessionResponse leaveSession(UUID participantId, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(session.getId())) {
            throw new UsernameIsNotParticipantSessionException("El participante no pertenece a esta sesión");
        }

        if (participant.getRole() == Participant.Role.HOST) {
            throw new InsufficientRoleException("El host no puede abandonar la session. Debe finalizarla.");
        }

        participant.setLeftAt(Instant.now());
        participantRepository.save(participant);

        return mapToResponse(session, session.getHost().getUsername());
    }

    private SessionResponse mapToResponse(GameSession session, String hostUsername) {
        long count = participantRepository.countByGameSession(session);

        UUID deckId = session.getDeck() != null ? session.getDeck().getId() : null;
        String seriesType = session.getDeck() != null && session.getDeck().getSeriesType() != null
                ? session.getDeck().getSeriesType().name()
                : "FIBONACCI";

        return new SessionResponse(
            session.getId(),
            session.getSessionCode(),
            session.getName(),
            hostUsername,
            count,
            session.getCreatedAt(),
            deckId,
            seriesType
        );
    }

    @Transactional
    public void handleDisconnect(UUID participantId, String code) {
        try {
            leaveSession(participantId, code);
        } catch (InsufficientRoleException e) {
            log.info("El host se desconectó de la sesión {}. La sesión se mantiene activa.", code);
        } catch (Exception e) {
            log.warn("Error al manejar la desconexión del participante {}: {}", participantId, e.getMessage());
        }
    }
    
    @Transactional
    public SessionResponse finishSession(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede finalizar la session");
        }

        session.setStatus(SessionStatus.FINISHED);
        session.setClosedAt(Instant.now());
        sessionRepository.save(session);
        
        return mapToResponse(session, user.getUsername());
    }
}