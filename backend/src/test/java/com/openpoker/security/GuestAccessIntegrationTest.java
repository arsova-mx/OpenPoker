package com.openpoker.security;

import com.openpoker.config.TestDataSeeding;
import com.openpoker.support.ApiTestClient;
import com.openpoker.support.ApiTestClient.Response;
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

/** #50: invitados con token propio, sin suplantación por nombre ni por el principal anónimo. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GuestAccessIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestDataSeeding seeding;

    private final List<StompTestClient> clients = new ArrayList<>();
    private ApiTestClient api;
    private TestUser host;
    private String sessionCode;

    @BeforeEach
    void setUp() {
        seeding.seedDecks();
        api = new ApiTestClient(port);
        host = api.registerUser("guesthost");
        sessionCode = api.createSession(host, "Guests").get("sessionCode").toString();
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

    private Response joinAsGuest(String code, String name) {
        return api.call("POST", "/api/sessions/" + code + "/guests", null, Map.of("guestName", name));
    }

    @Test
    @DisplayName("Un invitado se une sin cuenta y recibe un token de invitado")
    void guestJoinReturnsToken() {
        Response response = joinAsGuest(sessionCode, "Luis");

        assertEquals(201, response.status(), response.body());
        assertEquals("Luis", response.field("displayName"));
        assertNotNull(response.field("participantId"));
        assertFalse(response.field("guestToken").isBlank());
    }

    @Test
    @DisplayName("No se puede repetir el nombre de otro participante ni el de un usuario registrado")
    void guestNameMustBeUnique() {
        assertEquals(201, joinAsGuest(sessionCode, "Luis").status());

        assertEquals(409, joinAsGuest(sessionCode, "luis").status(), "Mismo nombre que otro invitado");
        assertEquals(409, joinAsGuest(sessionCode, host.username()).status(), "Mismo nombre que el host");
    }

    @Test
    @DisplayName("Nombres de invitado inválidos se rechazan con 400")
    void invalidGuestNamesAreRejected() {
        assertEquals(400, joinAsGuest(sessionCode, "").status());
        assertEquals(400, joinAsGuest(sessionCode, "L").status());
        assertEquals(400, joinAsGuest(sessionCode, "<script>").status());
    }

    @Test
    @DisplayName("El join REST sin token ya no crea participantes a nombre de 'anonymousUser'")
    void anonymousRestJoinIsRejected() {
        int status = api.call("POST", "/api/sessions/" + sessionCode + "/join", null, null).status();
        assertTrue(status == 401 || status == 403, "Status: " + status);
    }

    @Test
    @DisplayName("Un token de invitado no sirve como token de usuario en la API REST")
    void guestTokenIsNotAUserToken() {
        String guestToken = joinAsGuest(sessionCode, "Luis").field("guestToken");

        int status = api.call("GET", "/api/users/me", guestToken, null).status();
        assertTrue(status == 401 || status == 403, "Status: " + status);
    }

    @Test
    @DisplayName("No se puede registrar el username 'anonymousUser' ni usernames con caracteres inválidos")
    void reservedAndInvalidUsernamesCannotRegister() {
        assertEquals(400, api.call("POST", "/api/auth/register", null,
                Map.of("username", "anonymousUser", "email", "anon@example.com", "password", "secret-123")).status());

        Response invalid = api.call("POST", "/api/auth/register", null,
                Map.of("username", "ana maria", "email", "ana@example.com", "password", "secret-123"));
        assertEquals(400, invalid.status());
        assertNotNull(invalid.field("message"), "La validación debe incluir un mensaje legible");
    }

    @Test
    @DisplayName("Con su token, el invitado se conecta, se une y ve la sala")
    void guestJoinsRoomWithToken() throws Exception {
        String guestToken = joinAsGuest(sessionCode, "Luis").field("guestToken");

        StompTestClient guest = connect(guestToken);
        BlockingQueue<String> participants = guest.subscribe("/topic/session/" + sessionCode + "/participants");
        guest.send("/app/session.join", "{\"inviteCode\":\"" + sessionCode + "\"}");

        String message = participants.poll(5, TimeUnit.SECONDS);
        assertNotNull(message, "El invitado debía recibir la lista de participantes");
        assertTrue(message.contains("\"displayName\":\"Luis\""), message);
        assertTrue(message.contains("\"isGuest\":true"), message);
    }

    @Test
    @DisplayName("El token de invitado de una sala no permite escuchar otra")
    void guestTokenIsScopedToItsSession() throws Exception {
        String otherCode = api.createSession(host, "Otra sala").get("sessionCode").toString();
        String guestToken = joinAsGuest(sessionCode, "Luis").field("guestToken");

        StompTestClient guest = connect(guestToken);
        guest.subscribe("/topic/session/" + otherCode + "/participants");

        assertTrue(guest.awaitRejection(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Enviar solo un nombre por WebSocket ya no permite tomar la identidad de un invitado")
    void guestNameAloneCannotImpersonate() throws Exception {
        assertEquals(201, joinAsGuest(sessionCode, "Luis").status());

        StompTestClient hostClient = connect(host.token());
        BlockingQueue<String> participants = hostClient.subscribe("/topic/session/" + sessionCode + "/participants");
        hostClient.send("/app/session.join", "{\"inviteCode\":\"" + sessionCode + "\"}");
        assertNotNull(participants.poll(5, TimeUnit.SECONDS));

        StompTestClient attacker = connect(null);
        BlockingQueue<String> attackerErrors = attacker.subscribe("/user/queue/errors");
        attacker.send("/app/session.join", "{\"inviteCode\":\"" + sessionCode + "\",\"guestName\":\"Luis\"}");

        assertNotNull(attackerErrors.poll(5, TimeUnit.SECONDS), "El intento debía responder con un error privado");
        assertNull(participants.poll(1, TimeUnit.SECONDS), "No debía haber un nuevo join en la sala");
    }
}
