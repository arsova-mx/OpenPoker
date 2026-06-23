package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.InvalidVoteValueException;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.SessionNotInVotingException;
import com.openpoker.repository.*; // Importamos todos los repositorios
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {
    @Mock
    GameSessionRepository sessionRepository;

    @Mock
    ParticipantRepository participantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    TicketRepository ticketRepository; // 🚀 1. AGREGADO: Mock del nuevo repositorio de tickets

    @InjectMocks
    VoteService service;

    private GameSession session;
    private User user;
    private Participant participant;
    private Ticket ticket; // 🚀 2. AGREGADO: Atributo de apoyo para el ticket de pruebas

    @BeforeEach
    void setUp() {
        VotingDeck deck = VotingDeck.builder()
                .id(UUID.randomUUID())
                .name("Fibonacci")
                .values(List.of(
                CardValue.builder().value("1").orderIndex(0).build(),
                CardValue.builder().value("2").orderIndex(1).build(),
                CardValue.builder().value("3").orderIndex(2).build(),
                CardValue.builder().value("5").orderIndex(3).build(),
                CardValue.builder().value("8").orderIndex(4).build()
                ))
                .build();

        session = GameSession.builder()
                .id(UUID.randomUUID())
                .sessionCode("ABC")
                .status(SessionStatus.VOTING)
                .deck(deck)
                .build();

        user = User.builder().id(UUID.randomUUID()).username("user").build();

        participant = Participant.builder()
                .id(UUID.randomUUID())
                .gameSession(session)
                .user(user)
                .role(Participant.Role.VOTER)
                .build();

        // 🚀 3. AGREGADO: Inicializamos un objeto Ticket para asociarlo a los escenarios
        ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .gameSession(session)
                .tittle("Refactorizar base de datos")
                .description("PR de Votos por ticket")
                .build();
    }

    @Test
    void castVote_valid() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        
        // 🔄 CORREGIDO: Cambiado de findByGameSessionAndUser a findByTicketAndUser
        when(voteRepository.findByTicketAndUser(ticket, user)).thenReturn(Optional.empty());
        when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.countByGameSession(session)).thenReturn(2L);
        
        // 🔄 CORREGIDO: Cambiado de findAllByGameSession a findAllByTicket
        when(voteRepository.findAllByTicket(ticket)).thenReturn(List.of(Vote.builder().ticket(ticket).user(user).cardValue("5").build()));

        // 🔄 CORREGIDO: Pasamos el ticket.getId() como nuevo parámetro requerido
        VoteResponse res = service.castVote("user", "ABC", ticket.getId(), new CastVoteRequest("5"));

        assertEquals("5", res.cardValue());
        assertEquals(SessionStatus.VOTING, session.getStatus());
    }

    @Test
    void castVote_invalidCardValue() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));

        // 🔄 CORREGIDO: Añadido ticket.getId()
        assertThrows(InvalidVoteValueException.class, () -> service.castVote("user", "ABC", ticket.getId(), new CastVoteRequest("999")));
    }

    @Test
    void castVote_rejectsWhenSessionIsNotVoting() {
        session.setStatus(SessionStatus.WAITING);
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket

        // 🔄 CORREGIDO: Añadido ticket.getId()
        assertThrows(SessionNotInVotingException.class, () -> service.castVote("user", "ABC", ticket.getId(), new CastVoteRequest("5")));
    }

    @Test
    void castVote_movesSessionToWaitingWhenAllParticipantsVoted() {
        User secondUser = User.builder().id(UUID.randomUUID()).username("user2").build();
        Vote otherVote = Vote.builder().ticket(ticket).user(secondUser).cardValue("3").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        
        // 🔄 CORREGIDO: Métodos apuntando a Ticket
        when(voteRepository.findByTicketAndUser(ticket, user)).thenReturn(Optional.empty());
        when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.countByGameSession(session)).thenReturn(2L);
        when(voteRepository.findAllByTicket(ticket)).thenReturn(List.of(otherVote, Vote.builder().ticket(ticket).user(user).cardValue("8").build()));
        when(sessionRepository.save(session)).thenReturn(session);

        // 🔄 CORREGIDO: Añadido ticket.getId()
        VoteResponse response = service.castVote("user", "ABC", ticket.getId(), new CastVoteRequest("8"));

        assertEquals("8", response.cardValue());
        assertEquals(SessionStatus.WAITING, session.getStatus());
    }

    @Test
    void revealVotes_onlyHostAndOnlyWhenWaiting() {
        session.setStatus(SessionStatus.WAITING);
        participant.setRole(Participant.Role.HOST);

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(sessionRepository.save(session)).thenReturn(session);
        
        // 🔄 CORREGIDO: Cambiado a findAllByTicket
        when(voteRepository.findAllByTicket(ticket)).thenReturn(List.of());

        // 🔄 CORREGIDO: Añadido ticket.getId()
        VotingResultsResponse res = service.revealVotes("user", "ABC", ticket.getId());

        assertTrue(res.revealed());
        assertEquals(SessionStatus.REVEALED, session.getStatus());
    }

    @Test
    void getVotes_masked_whenNotRevealed() {
        session.setVotesRevealed(false);
        Vote vote = Vote.builder().ticket(ticket).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        
        // 🔄 CORREGIDO: Cambiado a findAllByTicket
        when(voteRepository.findAllByTicket(ticket)).thenReturn(List.of(vote));

        // 🔄 CORREGIDO: Añadido ticket.getId()
        VotingResultsResponse res = service.getVotes("ABC", ticket.getId(), "user");

        assertEquals("*", res.votes().get(0).cardValue());
    }

    @Test
    void getVotes_showRealValues() {
        session.setVotesRevealed(true);
        Vote vote = Vote.builder().ticket(ticket).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        
        // 🔄 CORREGIDO: Cambiado a findAllByTicket
        when(voteRepository.findAllByTicket(ticket)).thenReturn(List.of(vote));

        // 🔄 CORREGIDO: Añadido ticket.getId()
        VotingResultsResponse res = service.getVotes("ABC", ticket.getId(), "user");

        assertEquals("5", res.votes().get(0).cardValue());
    }

    @Test
    void resetVotes_deletesCurrentRoundVotesAndMovesSessionToVoting() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        session.setId(sessionId);
        session.setStatus(SessionStatus.REVEALED);
        session.setVotesRevealed(true);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        when(sessionRepository.save(session)).thenReturn(session);

        participant.setRole(Participant.Role.HOST);
        participant.setId(participantId);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        // 🔄 CORREGIDO: Pasamos ticket.getId() al método resetVotes
        service.resetVotes(sessionId, ticket.getId(), participantId);

        // 🔄 CORREGIDO: Verificamos que llame a deleteAllByTicket pasándole el objeto completo ticket
        verify(voteRepository).deleteAllByTicket(ticket);
        verify(sessionRepository).save(session);
        assertEquals(SessionStatus.VOTING, session.getStatus());
        assertTrue(!session.isVotesRevealed());
    }

    @Test
    void resetVotes_rejectsWhenUserIsNotHost() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        session.setId(sessionId);
        session.setStatus(SessionStatus.REVEALED);
        session.setVotesRevealed(true);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket)); // Mock ticket
        participant.setId(participantId);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        // 🔄 CORREGIDO: Añadido ticket.getId()
        InsufficientRoleException ex = assertThrows(InsufficientRoleException.class,
                () -> service.resetVotes(sessionId, ticket.getId(), participantId));

        assertEquals("Solo el host puede reiniciar la votacion", ex.getMessage());
    }
}