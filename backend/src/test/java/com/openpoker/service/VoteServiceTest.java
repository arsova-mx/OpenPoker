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
import com.openpoker.globalexception.InvalidValueException;
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
        VotingDeck deck = VotingDeck.builder().id(UUID.randomUUID()).name("Fibonacci").build();
        deck.setValues(List.of(
                DeckValue.builder().value("3").deck(deck).build(),
                DeckValue.builder().value("5").deck(deck).build(),
                DeckValue.builder().value("8").deck(deck).build()
        ));

        session = GameSession.builder().sessionCode("ABC").status(SessionStatus.VOTING).deck(deck).build();

        user = User.builder().id(UUID.randomUUID()).username("user").build();

        participant = Participant.builder().gameSession(session).user(user).role(Participant.Role.VOTER).build();
    }

    @Test
    void castVote_valid() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(voteRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
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

        assertThrows(InvalidValueException.class, () -> service.castVote("user", "ABC", new CastVoteRequest("999")));
    }

    @Test
    void castVote_rejectsWhenSessionIsNotVoting() {
        session.setStatus(SessionStatus.WAITING);
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));

        assertThrows(SessionNotInVotingException.class, () -> service.castVote("user", "ABC", new CastVoteRequest("5")));
    }

    @Test
    void castVote_movesSessionToWaitingWhenAllParticipantsVoted() {
        User secondUser = User.builder().id(UUID.randomUUID()).username("user2").build();
        Vote otherVote = Vote.builder().gameSession(session).user(secondUser).cardValue("3").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(voteRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        when(sessionRepository.save(session)).thenReturn(session);
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of());

        VotingResultsResponse res = service.revealVotes("user", "ABC");

        assertTrue(res.revealed());
        assertEquals(SessionStatus.WAITING, session.getStatus());
    }

    @Test
    void getVotes_masked_whenNotRevealed() {
        session.setVotesRevealed(false);

        Vote vote = Vote.builder().gameSession(session).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
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
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(vote));

        VotingResultsResponse res = service.getVotes("ABC", "user");

        assertEquals("5", res.votes().get(0).cardValue());
    }
}
