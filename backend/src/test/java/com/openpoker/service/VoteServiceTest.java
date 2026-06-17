package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.DeckValue;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.SessionStatus;
import com.openpoker.entity.User;
import com.openpoker.entity.Vote;
import com.openpoker.entity.VotingDeck;
import com.openpoker.globalexception.InvalidVoteValueException;
import com.openpoker.globalexception.InsufficientRoleException;
import com.openpoker.globalexception.SessionNotInVotingException;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
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

    @InjectMocks
    VoteService service;

    private GameSession session;
    private User user;
    private Participant participant;

    @BeforeEach
    void setUp() {
        VotingDeck deck = VotingDeck.builder()
                .id(UUID.randomUUID())
                .name("Fibonacci")
                .values(List.of(
                DeckValue.builder().value("1").build(),
                DeckValue.builder().value("2").build(),
                DeckValue.builder().value("3").build(),
                DeckValue.builder().value("5").build(),
                DeckValue.builder().value("8").build()
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
    }

    @Test
    void castVote_valid() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(voteRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.empty());
        when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.countByGameSession(session)).thenReturn(2L);
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(Vote.builder().gameSession(session).user(user).cardValue("5").build()));

        VoteResponse res = service.castVote("user", "ABC", new CastVoteRequest("5"));

        assertEquals("5", res.cardValue());
        assertEquals(SessionStatus.VOTING, session.getStatus());
    }

    @Test
    void castVote_invalidCardValue() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));

        assertThrows(InvalidVoteValueException.class, () -> service.castVote("user", "ABC", new CastVoteRequest("999")));
    }

    @Test
    void castVote_rejectsWhenSessionIsNotVoting() {
        session.setStatus(SessionStatus.WAITING);
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThrows(SessionNotInVotingException.class, () -> service.castVote("user", "ABC", new CastVoteRequest("5")));
    }

    @Test
    void castVote_movesSessionToWaitingWhenAllParticipantsVoted() {
        User secondUser = User.builder().id(UUID.randomUUID()).username("user2").build();
        Vote otherVote = Vote.builder().gameSession(session).user(secondUser).cardValue("3").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(voteRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.empty());
        when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.countByGameSession(session)).thenReturn(2L);
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(otherVote, Vote.builder().gameSession(session).user(user).cardValue("8").build()));
        when(sessionRepository.save(session)).thenReturn(session);

        VoteResponse response = service.castVote("user", "ABC", new CastVoteRequest("8"));

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
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(sessionRepository.save(session)).thenReturn(session);
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of());

        VotingResultsResponse res = service.revealVotes("user", "ABC");

        assertTrue(res.revealed());
        assertEquals(SessionStatus.REVEALED, session.getStatus());
    }

    @Test
    void getVotes_masked_whenNotRevealed() {
        session.setVotesRevealed(false);

        Vote vote = Vote.builder().gameSession(session).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(vote));

        VotingResultsResponse res = service.getVotes("ABC", "user");

        assertEquals("*", res.votes().get(0).cardValue());
    }

    @Test
    void getVotes_showRealValues() {
        session.setVotesRevealed(true);

        Vote vote = Vote.builder().gameSession(session).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(vote));

        VotingResultsResponse res = service.getVotes("ABC", "user");

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
        when(sessionRepository.save(session)).thenReturn(session);

        participant.setRole(Participant.Role.HOST);
        participant.setId(participantId);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        service.resetVotes(sessionId, participantId);

        verify(voteRepository).deleteAllByGameSession(session);
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
        participant.setId(participantId);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        InsufficientRoleException ex = assertThrows(InsufficientRoleException.class,
                () -> service.resetVotes(sessionId, participantId));

        assertEquals("Solo el host puede reiniciar la votacion", ex.getMessage());
    }
}
