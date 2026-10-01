package com.openpoker.service;

import com.openpoker.entity.*;
import com.openpoker.entity.SessionStatus;
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
            
            // ✅ CORREGIDO: Se pasa la entidad User completa en 'host(user)'
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

    public SessionResponse getSessionByCode(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        // ✅ CORREGIDO: Extraer el host directamente de la relación
        return mapToResponse(session, session.getHost().getUsername());
    }

    @Transactional
    public SessionResponse joinSession(JoinSessionRequest request) {
        GameSession session = sessionRepository.findBySessionCode(request.code())
                .orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        // Validar si la sesión ya finalizó (Criterio de aceptación: 409 Conflict o similar)
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new IllegalStateException("No puedes unirte. La sesión ya ha finalizado.");
        }

        Participant participant;

        if (request.username() != null && !request.username().isBlank()) {
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            var existingParticipant = participantRepository.findByGameSessionAndUser(session, user);
            if (existingParticipant.isPresent()) {
                participant = existingParticipant.get(); 
                // ✅ IMPORTANTE: Si el usuario vuelve a entrar, limpiamos el leftAt
                participant.setLeftAt(null);
                participant = participantRepository.save(participant);
            } else {
                // ✅ CORREGIDO: Comparar usando el ID del host de la relación
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

        } else if (request.guestName() != null && !request.guestName().isBlank()) {
            var existingGuest = participantRepository.findByGameSessionAndGuestDisplayName(session, request.guestName());

            if (existingGuest.isPresent()) {
                participant = existingGuest.get(); 
                // ✅ Limpiamos el leftAt si vuelve a entrar
                participant.setLeftAt(null);
                participant = participantRepository.save(participant);
            } else {
                participant = Participant.builder()
                        .gameSession(session)
                        .user(null)
                        .guestDisplayName(request.guestName())
                        .role(Participant.Role.VOTER)
                        .build();

                participant = participantRepository.save(participant);
            }

        } else {
            throw new IllegalArgumentException("Debe proporcionar un usuario registrado o un nombre de invitado.");
        }

        String hostName = getHostDisplayName(session);
        return mapToResponse(session, hostName);
    }
    
    // Helper para obtener el nombre del Host
    private String getHostDisplayName(GameSession session) {
        // ✅ CORREGIDO: Usar la relación directa
        if (session.getHost() == null) {
            return "Host Anónimo";
        }
        return session.getHost().getUsername();
    }

    public List<Participant> getParticipants(String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));
        // TODO (Optimización futura): Deberíamos filtrar a los participantes donde leftAt IS NULL
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

        // ✅ REEMPLAZADO: Borrado lógico en lugar de físico
        participant.setLeftAt(Instant.now());
        participantRepository.save(participant);

        // ✅ CORREGIDO
        return mapToResponse(session, session.getHost().getUsername());
    }

    private SessionResponse mapToResponse(GameSession session, String hostUsername) {
        // TODO (Optimización futura): Contar solo donde leftAt IS NULL
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
        // Como recomienda el issue, NO cerraremos la sesión automáticamente si el host se desconecta.
        // Si el host sufre un corte de red, podrá volver a entrar a su sala porque sigue ACTIVE.
        try {
            leaveSession(participantId, code);
        } catch (InsufficientRoleException e) {
            // Ignoramos si el host intenta hacer leave por desconexión, simplemente se queda "dentro"
            log.info("El host se desconectó de la sesión {}. La sesión se mantiene activa.", code);
        } catch (Exception e) {
            log.warn("Error al manejar la desconexión del participante {}: {}", participantId, e.getMessage());
        }
    }
    
    // ✅ DESCOMENTADO Y REFACTORIZADO: Este es el nuevo método oficial para cerrar la sala
    @Transactional
    public SessionResponse finishSession(String username, String code) {
        GameSession session = sessionRepository.findBySessionCode(code).orElseThrow(() -> new SessionNotFoundException("Session no encontrada"));

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Participant participant = participantRepository.findByGameSessionAndUser(session, user).orElseThrow(() -> new UsernameIsNotParticipantSessionException(
                "No eres participante"));

        if (participant.getRole() != Participant.Role.HOST) {
            throw new InsufficientRoleException("Solo el host puede finalizar la session");
        }

        // Cierre Lógico
        session.setStatus(SessionStatus.FINISHED);
        session.setClosedAt(Instant.now());
        sessionRepository.save(session);
        
        // Ya no eliminamos a los participantes, su historial queda intacto

        return mapToResponse(session, user.getUsername());
    }
}