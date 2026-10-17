package com.openpoker.security;

import com.openpoker.config.TestDataSeeding;
import com.openpoker.support.ApiTestClient;
import com.openpoker.support.ApiTestClient.TestUser;
import com.openpoker.support.StompTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** #48: autorización de SEND/SUBSCRIBE por sala y errores privados por conexión. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketSecurityIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestDataSeeding seeding;

    private final List<StompTestClient> clients = new ArrayList<>();
    private ApiTestClient api;
    private TestUser host;
    private String sessionCode;
    private String ticketId;

    @BeforeEach
    void setUp() {
        seeding.seedDecks();
        api = new ApiTestClient(port);
        host = api.registerUser("wshost");
        Map<String, Object> session = api.createSession(host, "WS security");
        sessionCode = session.get("sessionCode").toString();
        ticketId = api.createTicket(host, session.get("id").toString(), "Ticket WS").get("id").toString();
    }

    @AfterEach
    void closeClients() {
        clients.forEach(StompTestClient::close);
    }

    private StompTestClient connect(String token) throws Exception {
        StompTestClient client = StompTestClient.connect(port, token);
        clients.add(client);
        return client;
    }

    @Test
    @DisplayName("Un participante puede suscribirse a su sala y recibir eventos")
    void participantReceivesRoomEvents() throws Exception {
        StompTestClient hostClient = connect(host.token());
        BlockingQueue<String> participants = hostClient.subscribe("/topic/session/" + sessionCode + "/participants");

        hostClient.send("/app/session.join", "{\"inviteCode\":\"" + sessionCode + "\"}");

        String message = participants.poll(5, TimeUnit.SECONDS);
        assertNotNull(message, "El host debía recibir la lista de participantes");
        assertTrue(message.contains(host.username()));
        assertTrue(hostClient.isConnected());
    }

    @Test
    @DisplayName("Publicar directo en /topic se rechaza y nadie recibe el evento falso")
    void clientsCannotPublishToTopics() throws Exception {
        StompTestClient hostClient = connect(host.token());
        BlockingQueue<String> votes = hostClient.subscribe("/topic/session/" + sessionCode + "/votes");
        Thread.sleep(300);

        StompTestClient attacker = connect(null);
        attacker.send("/topic/session/" + sessionCode + "/votes", "{\"revealed\":true,\"votes\":[]}");

        assertTrue(attacker.awaitRejection(5, TimeUnit.SECONDS), "El servidor debía rechazar el SEND a /topic");
        assertNull(votes.poll(1, TimeUnit.SECONDS), "El evento falsificado no debía llegar a la sala");
    }

    @Test
    @DisplayName("Un usuario que no participa no puede suscribirse a la sala")
    void outsiderCannotSubscribe() throws Exception {
        TestUser outsider = api.registerUser("wsoutsider");
        StompTestClient outsiderClient = connect(outsider.token());

        outsiderClient.subscribe("/topic/session/" + sessionCode + "/participants");

        assertTrue(outsiderClient.awaitRejection(5, TimeUnit.SECONDS), "La suscripción debía rechazarse");
    }

    @Test
    @DisplayName("Una conexión anónima no puede escuchar una sala")
    void anonymousCannotSubscribe() throws Exception {
        StompTestClient anonymous = connect(null);

        anonymous.subscribe("/topic/session/" + sessionCode + "/votes");

        assertTrue(anonymous.awaitRejection(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Los errores llegan solo a quien los provocó, sin detalles internos")
    void errorsArePrivate() throws Exception {
        TestUser voter = api.registerUser("wsvoter");
        assertEquals(200, api.call("POST", "/api/sessions/" + sessionCode + "/join", voter.token(), null).status());

        StompTestClient hostClient = connect(host.token());
        BlockingQueue<String> roomErrors = hostClient.subscribe("/topic/session/" + sessionCode + "/errors");

        StompTestClient voterClient = connect(voter.token());
        BlockingQueue<String> voterErrors = voterClient.subscribe("/user/queue/errors");
        BlockingQueue<String> participants = voterClient.subscribe("/topic/session/" + sessionCode + "/participants");
        voterClient.send("/app/session.join", "{\"inviteCode\":\"" + sessionCode + "\"}");
        assertNotNull(participants.poll(5, TimeUnit.SECONDS), "El votante debía unirse a la sala");

        // Un votante no puede revelar: debe recibir el error en su cola privada
        voterClient.send("/app/session.reveal", "{\"ticketId\":\"" + ticketId + "\"}");

        String error = voterErrors.poll(5, TimeUnit.SECONDS);
        assertNotNull(error, "El votante debía recibir el error en /user/queue/errors");
        assertTrue(error.contains("\"code\":\"FORBIDDEN\""), error);
        assertTrue(error.contains("session.reveal"), error);
        assertFalse(error.contains("Exception"), "No debe exponer nombres de clases: " + error);

        assertNull(roomErrors.poll(1, TimeUnit.SECONDS), "El error no debía difundirse al resto de la sala");
    }
}
