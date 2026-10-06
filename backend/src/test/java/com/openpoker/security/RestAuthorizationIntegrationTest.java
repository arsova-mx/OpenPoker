package com.openpoker.security;

import com.openpoker.config.TestDataSeeding;
import com.openpoker.support.ApiTestClient;
import com.openpoker.support.ApiTestClient.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** #49: solo los participantes de una sala pueden leer o escribir sus datos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RestAuthorizationIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestDataSeeding seeding;

    private ApiTestClient api;
    private TestUser host;
    private TestUser outsider;
    private String sessionId;
    private String sessionCode;
    private String ticketId;

    @BeforeEach
    void setUp() {
        seeding.seedDecks();
        api = new ApiTestClient(port);
        host = api.registerUser("host");
        outsider = api.registerUser("outsider");

        Map<String, Object> session = api.createSession(host, "Sprint 1");
        sessionId = session.get("id").toString();
        sessionCode = session.get("sessionCode").toString();
        ticketId = api.createTicket(host, sessionId, "Login con GitHub").get("id").toString();
    }

    @Test
    @DisplayName("Un usuario que no participa no puede leer el backlog de la sala")
    void outsiderCannotReadBacklog() {
        assertEquals(403, api.call("GET", "/api/tickets/session/" + sessionId, outsider.token(), null).status());
        assertEquals(200, api.call("GET", "/api/tickets/session/" + sessionId, host.token(), null).status());
    }

    @Test
    @DisplayName("Al unirse a la sala, el usuario sí puede leer el backlog")
    void joinedUserCanReadBacklog() {
        assertEquals(200, api.call("POST", "/api/sessions/" + sessionCode + "/join", outsider.token(), null).status());
        assertEquals(200, api.call("GET", "/api/tickets/session/" + sessionId, outsider.token(), null).status());
    }

    @Test
    @DisplayName("Un usuario que no participa no puede leer ni escribir comentarios")
    void outsiderCannotReadOrWriteComments() {
        String path = "/api/tickets/" + ticketId + "/comments";

        assertEquals(403, api.call("GET", path, outsider.token(), null).status());
        assertEquals(403, api.call("POST", path, outsider.token(), Map.of("content", "hola")).status());

        assertEquals(201, api.call("POST", path, host.token(), Map.of("content", "Necesitamos migración")).status());
        assertEquals(200, api.call("GET", path, host.token(), null).status());
    }

    @Test
    @DisplayName("Comentar un ticket inexistente responde 404, no 500")
    void unknownTicketIsNotFound() {
        String path = "/api/tickets/00000000-0000-0000-0000-000000000000/comments";
        assertEquals(404, api.call("GET", path, host.token(), null).status());
    }

    @Test
    @DisplayName("El perfil de otro usuario no expone email ni teléfono; /me sí devuelve el perfil completo")
    void publicProfileHidesContactData() {
        ApiTestClient.Response publicProfile = api.call("GET", "/api/users/" + host.id(), outsider.token(), null);
        assertEquals(200, publicProfile.status());
        assertEquals(host.username(), publicProfile.field("username"));
        assertFalse(publicProfile.asMap().containsKey("email"));
        assertFalse(publicProfile.asMap().containsKey("phoneNumber"));

        ApiTestClient.Response me = api.call("GET", "/api/users/me", host.token(), null);
        assertEquals(200, me.status());
        assertEquals(host.email(), me.field("email"));
    }

    @Test
    @DisplayName("Actualizar el perfil usa el usuario del token")
    void updateProfileUsesAuthenticatedUser() {
        ApiTestClient.Response response = api.call("PATCH", "/api/users/profile", host.token(),
                Map.of("companyName", "Arsova"));
        assertEquals(200, response.status());
        assertEquals("Arsova", response.field("companyName"));
        assertEquals(host.username(), response.field("username"));
    }
}
