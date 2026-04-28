package com.openpoker.service;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.entity.*;
import com.openpoker.globalexception.InvalidVoteValueException;
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

import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    VoteService service;

    @Mock
    VoteRepository voteRepository;

    private GameSession session;
    private User user;
    private Participant participant;

    @BeforeEach
    void setUp() {
        session = GameSession.builder().sessionCode("ABC").status(SessionStatus.VOTING).build();

        user = User.builder().username("user").build();

        participant = Participant.builder().user(user).role(Participant.Role.VOTER).build();
    }

    @Test
    void castVote_valid() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(voteRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoteResponse res = service.castVote("user", "ABC", new CastVoteRequest("5"));

        assertEquals("5", res.cardValue());
    }

    @Test
    void castVote_invalidCardValue() {
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));

        assertThrows(InvalidVoteValueException.class, () -> service.castVote("user", "ABC", new CastVoteRequest("999")));
    }

    @Test
    void castVote_rejectsWhenSessionIsNotVoting() {
        session.setStatus(SessionStatus.WAITING);
        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));

        assertThrows(SessionNotInVotingException.class, () -> service.castVote("user", "ABC", new CastVoteRequest("5")));
    }

    @Test
    void castVote_updatesExistingVote() {
        Vote existingVote = Vote.builder().gameSession(session).user(user).cardValue("3").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(voteRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(existingVote));
        when(voteRepository.save(existingVote)).thenReturn(existingVote);

        VoteResponse response = service.castVote("user", "ABC", new CastVoteRequest("8"));

        assertEquals("8", response.cardValue());
    }

    @Test
    void revealVotes_onlyHost() {
        participant.setRole(Participant.Role.HOST);

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(userRepository.findByUsername("host")).thenReturn(Optional.of(user));
        when(participantRepository.findByGameSessionAndUser(session, user)).thenReturn(Optional.of(participant));
        when(sessionRepository.save(session)).thenReturn(session);
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of());

        VotingResultsResponse res = service.revealVotes("host", "ABC");

        assertTrue(res.revealed());
        assertEquals(SessionStatus.VOTING, session.getStatus());
    }

    @Test
    void getVotes_masked_whenNotRevealed() {
        session.setVotesRevealed(false);

        Vote vote = Vote.builder().gameSession(session).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(vote));

        VotingResultsResponse res = service.getVotes("ABC");

        assertEquals("*", res.votes().get(0).cardValue());
    }

    @Test
    void getVotes_showRealValues() {
        session.setVotesRevealed(true);

        Vote vote = Vote.builder().gameSession(session).user(user).cardValue("5").build();

        when(sessionRepository.findBySessionCode("ABC")).thenReturn(Optional.of(session));
        when(voteRepository.findAllByGameSession(session)).thenReturn(List.of(vote));

        VotingResultsResponse res = service.getVotes("ABC");

        assertEquals("5", res.votes().get(0).cardValue());
    }
}
