package com.openpoker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.openpoker.config.TestDataSeeding;
import com.openpoker.entity.Vote;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.VoteRepository;
import com.openpoker.support.ApiTestClient;
import com.openpoker.support.ApiTestClient.Response;
import com.openpoker.support.ApiTestClient.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** #40: reiniciar la votación abre una ronda nueva y conserva los votos de las anteriores. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VoteRoundsIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestDataSeeding seeding;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @Transactional
    @DisplayName("Tras reiniciar la votación se vuelve a votar en la ronda 2 y se conservan los votos de la ronda 1")
    void votesAreKeptPerRound() throws Exception {
        seeding.seedDecks();
        ApiTestClient api = new ApiTestClient(port);
        TestUser host = api.registerUser("host");
        TestUser voter = api.registerUser("voter");
        Map<String, Object> session = api.createSession(host, "Refinamiento");
        String sessionId = session.get("id").toString();
        String code = session.get("sessionCode").toString();
        String ticketId = api.createTicket(host, sessionId, "Exportar estimaciones").get("id").toString();
        UUID ticket = UUID.fromString(ticketId);
        List<String> cards = cardIds(api, session.get("deckId").toString());
        assertEquals(200, api.call("POST", "/api/sessions/" + code + "/join", voter.token(), null).status());

        Response voting = api.call("PATCH", "/api/tickets/" + ticketId + "/status?newStatus=VOTING", host.token(), null);
        assertEquals(200, voting.status(), voting.body());

        // Ronda 1: votan los dos
        assertVoted(vote(api, host, code, ticketId, cards.get(0)), "host, ronda 1");
        assertVoted(vote(api, voter, code, ticketId, cards.get(1)), "voter, ronda 1");

        Response reset = api.call("POST", "/api/sessions/" + code + "/votes/reset?ticketId=" + ticketId, host.token(), null);
        assertEquals(204, reset.status(), reset.body());
        assertEquals(2, ticketRepository.findById(ticket).orElseThrow().getCurrentRound());

        // Ronda 2: el host vota y luego cambia su voto; debe quedar un solo voto suyo con la última carta
        assertVoted(vote(api, host, code, ticketId, cards.get(2)), "host, ronda 2");
        assertVoted(vote(api, host, code, ticketId, cards.get(3)), "host cambia su voto en la ronda 2");

        assertEquals(2, voteRepository.findAllByTicketIdAndRound(ticket, 1).size(),
                "Se deben conservar los dos votos de la ronda 1");
        List<Vote> secondRound = voteRepository.findAllByTicketIdAndRound(ticket, 2);
        assertEquals(1, secondRound.size(), "Cambiar el voto en la misma ronda actualiza, no duplica");
        assertEquals(UUID.fromString(cards.get(3)), secondRound.get(0).getCardValue().getId(),
                "El voto de la ronda 2 es la última carta elegida");
    }

    private static void assertVoted(Response response, String what) {
        assertEquals(200, response.status(), "Voto (" + what + "): " + response.body());
    }

    private static Response vote(ApiTestClient api, TestUser user, String code, String ticketId, String cardId) {
        return api.call("POST", "/api/sessions/" + code + "/votes?ticketId=" + ticketId, user.token(),
                Map.of("cardValue", cardId));
    }

    @SuppressWarnings("unchecked")
    private static List<String> cardIds(ApiTestClient api, String deckId) throws Exception {
        Response decks = api.call("GET", "/api/card-decks", null, null);
        assertEquals(200, decks.status(), decks.body());
        List<Map<String, Object>> all = decks.json().readValue(decks.body(), new TypeReference<>() { });
        Map<String, Object> deck = all.stream()
                .filter(d -> deckId.equals(d.get("id").toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró la baraja " + deckId));
        return ((List<Map<String, Object>>) deck.get("cards")).stream()
                .map(card -> card.get("id").toString())
                .toList();
    }
}
