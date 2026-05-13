package com.openpoker.integration;

import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.dto.WebSocketJoinSessionRequest;
import com.openpoker.entity.DeckValue;
import com.openpoker.entity.GameSession;
import com.openpoker.entity.SessionStatus;
import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;
import com.openpoker.entity.VotingDeck;
import com.openpoker.repository.GameSessionRepository;
import com.openpoker.repository.ParticipantRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.repository.VoteRepository;
import com.openpoker.repository.VotingDeckRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:openpoker_ws_it;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-only-jwt-secret-that-is-at-least-32-bytes-long"
})
class WebSocketStompIntegrationTest {

    private static final String INVITE_CODE = "ROOM42";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private VotingDeckRepository deckRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private VoteRepository voteRepository;

    private final WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

    @AfterEach
    void cleanup() throws Exception {
        try {
            voteRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        try {
            participantRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        try {
            sessionRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        try {
            deckRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        try {
            userRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void stompFlow_voteRevealReset_publishesExpectedTopics() throws Exception {
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());

        User host = userRepository.save(buildUser("host", "host@example.com", UserRole.HOST));
        User alice = userRepository.save(buildUser("alice", "alice@example.com", UserRole.VOTER));
        deckRepository.save(buildDeck());
        sessionRepository.save(GameSession.builder()
                .sessionCode(INVITE_CODE)
                .name("Sprint Planning")
                .hostUserId(host.getId())
                .status(SessionStatus.VOTING)
                .deck(deckRepository.findByName("Fibonacci").orElseThrow())
                .build());

        BlockingQueue<SessionResponse> stateMessages = new LinkedBlockingQueue<>();
        BlockingQueue<VotingResultsResponse> voteMessages = new LinkedBlockingQueue<>();

        StompSession hostSession = connectClient();
        hostSession.subscribe("/topic/session/" + INVITE_CODE + "/state", new QueueFrameHandler<>(SessionResponse.class, stateMessages));
        hostSession.subscribe("/topic/session/" + INVITE_CODE + "/votes", new QueueFrameHandler<>(VotingResultsResponse.class, voteMessages));

        StompSession aliceSession = connectClient();
        aliceSession.subscribe("/topic/session/" + INVITE_CODE + "/state", new QueueFrameHandler<>(SessionResponse.class, new LinkedBlockingQueue<>()));
        aliceSession.subscribe("/topic/session/" + INVITE_CODE + "/votes", new QueueFrameHandler<>(VotingResultsResponse.class, new LinkedBlockingQueue<>()));

        hostSession.send("/app/session.join", new WebSocketJoinSessionRequest(INVITE_CODE, "host"));
        aliceSession.send("/app/session.join", new WebSocketJoinSessionRequest(INVITE_CODE, "alice"));

        awaitMessage(stateMessages, state -> state.participantCount() == 2L && "VOTING".equals(state.status()));
        stateMessages.clear();
        voteMessages.clear();

        aliceSession.send("/app/session.vote", Map.of(
                "inviteCode", "OTHER",
                "username", "mallory",
                "cardValue", "3"
        ));


        VotingResultsResponse firstVoteSnapshot = awaitMessage(voteMessages, votes ->
                !votes.revealed() &&
                votes.votes().size() == 1 &&
                votes.votes().stream().allMatch(vote -> "*".equals(vote.cardValue()))
        );
        SessionResponse votingState = awaitMessage(stateMessages, state -> "VOTING".equals(state.status()));

        hostSession.send("/app/session.vote", Map.of(
                "inviteCode", "WRONG",
                "username", "mallory",
                "cardValue", "5"
        ));

        VotingResultsResponse waitingVotes = awaitMessage(voteMessages, votes ->
                !votes.revealed() &&
                votes.votes().size() == 2 &&
                votes.votes().stream().allMatch(vote -> "*".equals(vote.cardValue()))
        );
        SessionResponse waitingState = awaitMessage(stateMessages, state -> "WAITING".equals(state.status()));

        assertThat(firstVoteSnapshot.sessionCode()).isEqualTo(INVITE_CODE);
        assertThat(votingState.sessionCode()).isEqualTo(INVITE_CODE);
        assertThat(waitingVotes.sessionCode()).isEqualTo(INVITE_CODE);
        assertThat(waitingState.sessionCode()).isEqualTo(INVITE_CODE);

        hostSession.send("/app/session.reveal", Map.of(
                "inviteCode", "WRONG",
                "username", "mallory"
        ));

        VotingResultsResponse revealedVotes = awaitMessage(voteMessages, VotingResultsResponse::revealed);
        SessionResponse revealedState = awaitMessage(stateMessages, state -> "REVEALED".equals(state.status()));

        assertThat(revealedVotes.sessionCode()).isEqualTo(INVITE_CODE);
        assertThat(revealedVotes.votes())
                .extracting(VoteResponse::username, VoteResponse::cardValue)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("alice", "3"),
                        org.assertj.core.groups.Tuple.tuple("host", "5")
                );
        assertThat(revealedState.sessionCode()).isEqualTo(INVITE_CODE);

        hostSession.send("/app/session.reset-votes", Map.of(
                "inviteCode", "OTHER-ROOM",
                "username", "mallory",
                "sessionId", UUID.randomUUID().toString()
        ));

        VotingResultsResponse resetVotes = awaitMessage(voteMessages, votes -> !votes.revealed() && votes.votes().isEmpty());
        SessionResponse resetState = awaitMessage(stateMessages, state -> "VOTING".equals(state.status()));

        assertThat(resetVotes.sessionCode()).isEqualTo(INVITE_CODE);
        assertThat(resetState.sessionCode()).isEqualTo(INVITE_CODE);

        hostSession.disconnect();
        aliceSession.disconnect();
    }

    private StompSession connectClient() throws Exception {
        CompletableFuture<StompSession> future = stompClient.connectAsync(
                "ws://127.0.0.1:" + port + "/ws-native",
                new StompSessionHandlerAdapter() {
                }
        );

        return future.get(10, TimeUnit.SECONDS);
    }

    private User buildUser(String username, String email, UserRole role) {
        return User.builder()
                .username(username)
                .email(email)
                .role(role)
                .passwordHash("test-password")
                .build();
    }

    private VotingDeck buildDeck() {
        VotingDeck deck = VotingDeck.builder()
                .name("Fibonacci")
                .build();
        DeckValue three = DeckValue.builder().value("3").deck(deck).build();
        DeckValue five = DeckValue.builder().value("5").deck(deck).build();
        deck.setValues(List.of(three, five));
        return deck;
    }

    private <T> T awaitMessage(BlockingQueue<T> queue, Predicate<T> predicate) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

        while (System.nanoTime() < deadline) {
            T message = queue.poll(500, TimeUnit.MILLISECONDS);

            if (message == null) {
                continue;
            }

            if (predicate.test(message)) {
                return message;
            }
        }

        throw new AssertionError("No llego el mensaje esperado antes del timeout");
    }

    private static final class QueueFrameHandler<T> implements StompFrameHandler {
        private final Class<T> payloadType;
        private final BlockingQueue<T> queue;

        private QueueFrameHandler(Class<T> payloadType, BlockingQueue<T> queue) {
            this.payloadType = payloadType;
            this.queue = queue;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return payloadType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((T) payload);
        }
    }
}
