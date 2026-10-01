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

            GameSession candidate = GameSession.builder().sessionCode(code).name(request.name()).hostUserId(user.getId()).deck(deck).build();

            try {
                session = sessionRepository.saveAndFlush(candidate);

                break;
            } catch (DataIntegrityViolationException ex) {
                // Una colisión de restricción única en sessionCode significa que otra solicitud ganó la carrera.
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

    public SessionResponse getSessionByCode(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HostNotFoundException("Host no encontrado"));

        return mapToResponse(session, host.getUsername());
    }


    @Transactional
    public SessionResponse joinSession(JoinSessionRequest request) {
        GameSession session = sessionRepository.findBySessionCode(request.code())
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        Participant participant;

        // CASO 1: Es Usuario Registrado (trae username)
        if (request.username() != null && !request.username().isBlank()) {
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            var existingParticipant = participantRepository.findByGameSessionAndUser(session, user);
            if (existingParticipant.isPresent()) {
                participant = existingParticipant.get(); // Reutilizar si ya existe
            } else {
                Participant.Role role = (session.getHostUserId() != null && session.getHostUserId().equals(user.getId())) 
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
    
    /**
     * Une a un invitado sin cuenta. El nombre no puede coincidir (sin distinguir mayúsculas) con otro
     * participante de la sala ni con un usuario registrado, para que nadie se haga pasar por otro.
     */
    @Transactional
    public Participant joinAsGuest(String code, String guestName) {
        GameSession session = sessionRepository.findBySessionCode(code)
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
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

    // Helper para obtener el nombre del Host sin asumir que es un User
    private String getHostDisplayName(GameSession session) {
        if (session.getHostUserId() == null) {
            return "Host Anónimo";
        }
        return userRepository.findById(session.getHostUserId())
                .map(User::getUsername)
                .orElse("Host");
        }

    public List<Participant> getParticipants(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        return participantRepository.findAllByGameSession(session);
    }

    public SessionResponse leaveSession(UUID participantId, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));


        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "Participante no encontrado"));

        if (!participant.getGameSession().getId().equals(session.getId())) {
            throw new UsernameIsNotParticipantSessionException(
                "El participante no pertenece a esta sesión");
    }

        if (participant.getRole() == Participant.Role.HOST) {
            throw new InsufficientRoleException("El host no puede abandonar la session");
        }

        participantRepository.delete(participant);

        User host = userRepository.findById(session.getHostUserId()).orElseThrow(() -> new HostNotFoundException("Host no encontrado"));

        return mapToResponse(session, host.getUsername());
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


    // TODO: Cuando se implemente el historial de sesiones,
    // reemplazar la eliminación por un cierre lógico (closedAt/status).
    @Transactional
    public void handleDisconnect(UUID participantId, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        Participant participant = participantRepository.findById(participantId).orElseThrow(() -> new UsernameIsNotParticipantSessionException("Participante no encontrado"));

        if (participant.getRole() == Participant.Role.HOST) {
            // Si es el host, finalizar la sesión
            deleteSession(code);
        } else {
            // Si no es el host, simplemente abandonar
            leaveSession(participantId, code);
        }
    }

        @Transactional
        public void deleteSession(String code) {

            GameSession session = sessionRepository.findBySessionCode(code)
                    .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));

            sessionRepository.delete(session);
        }

            /* 
    @Transactional
    public SessionResponse finishSession(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede finalizar la session");
        }

        //session.setStatus(SessionStatus.FINISHED);
        sessionRepository.save(session);
        participantRepository.deleteAllByGameSession(session);

        return mapToResponse(session, user.getUsername());
    }
    */

}
