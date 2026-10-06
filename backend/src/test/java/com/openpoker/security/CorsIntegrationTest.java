package com.openpoker.security;

import com.openpoker.config.TestDataSeeding;
import com.openpoker.support.ApiTestClient;
import com.openpoker.support.ApiTestClient.Response;
import com.openpoker.support.StompTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/** #57: solo los orígenes configurados pueden usar la API y el WebSocket desde un navegador. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=http://localhost:3000, https://*.openpoker-preview.pages.dev")
@ActiveProfiles("test")
class CorsIntegrationTest {

    private static final String ALLOWED = "http://localhost:3000";
    private static final String PREVIEW = "https://pr-12.openpoker-preview.pages.dev";
    private static final String EVIL = "https://evil.example.com";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestDataSeeding seeding;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        seeding.seedDecks();
        api = new ApiTestClient(port);
    }

    private Response preflight(String origin) {
        return api.call("OPTIONS", "/api/sessions", null, null, Map.of(
                "Origin", origin,
                "Access-Control-Request-Method", "POST",
                "Access-Control-Request-Headers", "authorization,content-type"));
    }

    @Test
    @DisplayName("El preflight desde un origen permitido devuelve los headers CORS")
    void preflightFromAllowedOrigin() {
        Response response = preflight(ALLOWED);

        assertEquals(200, response.status());
        assertEquals(ALLOWED, response.header("Access-Control-Allow-Origin"));
        assertTrue(response.header("Access-Control-Allow-Methods").contains("POST"));
    }

    @Test
    @DisplayName("Los patrones permiten orígenes de previews (https://*.pages.dev)")
    void preflightFromPatternOrigin() {
        Response response = preflight(PREVIEW);

        assertEquals(200, response.status());
        assertEquals(PREVIEW, response.header("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Un origen no permitido se rechaza, tanto en el preflight como en la petición")
    void foreignOriginIsRejected() {
        Response preflight = preflight(EVIL);
        assertEquals(403, preflight.status());
        assertNull(preflight.header("Access-Control-Allow-Origin"));

        Response request = api.call("GET", "/api/card-decks", null, null, Map.of("Origin", EVIL));
        assertEquals(403, request.status());
    }

    @Test
    @DisplayName("Las peticiones sin Origin (curl, servidores) no se ven afectadas")
    void requestsWithoutOriginStillWork() {
        assertEquals(200, api.call("GET", "/api/card-decks", null, null).status());
    }

    @Test
    @DisplayName("Detrás de un proxy que termina TLS, el mismo origen se reconoce por X-Forwarded-Proto/Port")
    void sameOriginBehindTlsProxy() {
        String publicOrigin = "https://localhost:" + port;

        Response viaProxy = api.call("GET", "/api/card-decks", null, null, Map.of(
                "Origin", publicOrigin,
                "X-Forwarded-Proto", "https",
                "X-Forwarded-Port", String.valueOf(port)));
        assertEquals(200, viaProxy.status(), "La petición del mismo origen detrás del proxy debía aceptarse");

        // Control: sin los headers del proxy, un Origin https no coincide con la petición http
        Response direct = api.call("GET", "/api/card-decks", null, null, Map.of("Origin", publicOrigin));
        assertEquals(403, direct.status());
    }

    @Test
    @DisplayName("El WebSocket acepta orígenes permitidos y rechaza los demás en el handshake")
    void websocketHandshakeChecksOrigin() throws Exception {
        try (StompTestClient allowed = StompTestClient.connect(port, null, ALLOWED)) {
            assertTrue(allowed.isConnected());
        }

        ExecutionException rejected = assertThrows(ExecutionException.class,
                () -> StompTestClient.connect(port, null, EVIL));
        assertNotNull(rejected.getCause());
    }
}
