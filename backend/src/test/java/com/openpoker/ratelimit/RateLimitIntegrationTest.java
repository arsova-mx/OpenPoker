package com.openpoker.ratelimit;

import com.openpoker.config.TestDataSeeding;
import com.openpoker.support.ApiTestClient;
import com.openpoker.support.ApiTestClient.Response;
import com.openpoker.support.ApiTestClient.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** #57: límite de intentos en login, registro y entrada de invitados. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.enabled=true",
                "app.rate-limit.login.capacity=5",
                "app.rate-limit.login.period=1m",
                "app.rate-limit.register.capacity=2",
                "app.rate-limit.register.period=10m",
                "app.rate-limit.guest-join.capacity=2",
                "app.rate-limit.guest-join.period=1m",
                "app.rate-limit.login-failures.capacity=3",
                "app.rate-limit.login-failures.period=5m"
        })
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TestDataSeeding seeding;

    @Autowired
    private RateLimiter rateLimiter;

    private ApiTestClient api;
    private TestUser user;
    private String sessionCode;

    @BeforeEach
    void setUp() {
        seeding.seedDecks();
        rateLimiter.clear();
        api = new ApiTestClient(port);
        user = api.registerUser("rluser");
        sessionCode = api.createSession(user, "Rate limit").get("sessionCode").toString();
        rateLimiter.clear();
    }

    private Response login(String username, String password, String clientIp) {
        Map<String, String> headers = clientIp == null ? Map.of() : Map.of("X-Forwarded-For", clientIp);
        return api.call("POST", "/api/auth/login", null, Map.of("username", username, "password", password), headers);
    }

    private static String unknownUser() {
        return "nobody" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Tras varios intentos de login desde la misma IP responde 429 con Retry-After")
    void loginIsLimitedPerIp() {
        for (int i = 0; i < 5; i++) {
            assertEquals(401, login(unknownUser(), "wrong", null).status(), "Intento " + (i + 1));
        }

        Response limited = login(unknownUser(), "wrong", null);
        assertEquals(429, limited.status());
        assertTrue(Long.parseLong(limited.header("Retry-After")) > 0);
        assertTrue(limited.field("message").startsWith("Demasiados intentos"), limited.body());
    }

    @Test
    @DisplayName("El límite es por IP del cliente (X-Forwarded-For desde el proxy)")
    void limitsArePerClientIp() {
        for (int i = 0; i < 6; i++) {
            login(unknownUser(), "wrong", "198.51.100.10");
        }
        assertEquals(429, login(unknownUser(), "wrong", "198.51.100.10").status());

        assertEquals(401, login(unknownUser(), "wrong", "198.51.100.20").status(), "Otra IP no debe estar limitada");
    }

    @Test
    @DisplayName("Varios fallos contra la misma cuenta la bloquean temporalmente, desde cualquier IP")
    void repeatedFailuresLockTheAccount() {
        assertEquals(401, login(user.username(), "wrong", "203.0.113.1").status());
        assertEquals(401, login(user.username(), "wrong", "203.0.113.2").status());
        assertEquals(401, login(user.username(), "wrong", "203.0.113.3").status());

        Response locked = login(user.username(), "secret-123", "203.0.113.4");
        assertEquals(429, locked.status(), "Incluso con la contraseña correcta, la cuenta queda bloqueada un rato");
        assertNotNull(locked.header("Retry-After"));
    }

    @Test
    @DisplayName("Un login correcto reinicia el contador de fallos de la cuenta")
    void successfulLoginResetsFailures() {
        // Cada intento desde una IP distinta, para medir solo el contador de la cuenta y no el de la IP
        assertEquals(401, login(user.username(), "wrong", "192.0.2.1").status());
        assertEquals(401, login(user.username(), "wrong", "192.0.2.2").status());
        assertEquals(200, login(user.username(), "secret-123", "192.0.2.3").status());

        assertEquals(401, login(user.username(), "wrong", "192.0.2.4").status());
        assertEquals(401, login(user.username(), "wrong", "192.0.2.5").status());
        assertEquals(200, login(user.username(), "secret-123", "192.0.2.6").status(), "No debía quedar bloqueada");
    }

    @Test
    @DisplayName("El registro está limitado por IP")
    void registerIsLimitedPerIp() {
        for (int i = 0; i < 2; i++) {
            String name = "newuser" + UUID.randomUUID().toString().substring(0, 8);
            assertEquals(201, api.call("POST", "/api/auth/register", null,
                    Map.of("username", name, "email", name + "@example.com", "password", "secret-123")).status());
        }

        String name = "newuser" + UUID.randomUUID().toString().substring(0, 8);
        assertEquals(429, api.call("POST", "/api/auth/register", null,
                Map.of("username", name, "email", name + "@example.com", "password", "secret-123")).status());
    }

    @Test
    @DisplayName("La entrada de invitados está limitada por IP")
    void guestJoinIsLimitedPerIp() {
        String path = "/api/sessions/" + sessionCode + "/guests";
        assertEquals(201, api.call("POST", path, null, Map.of("guestName", "Invitado A")).status());
        assertEquals(201, api.call("POST", path, null, Map.of("guestName", "Invitado B")).status());

        assertEquals(429, api.call("POST", path, null, Map.of("guestName", "Invitado C")).status());
    }

    @Test
    @DisplayName("El 429 incluye los headers CORS para que el navegador pueda leerlo")
    void tooManyRequestsIsReadableCrossOrigin() {
        Map<String, String> fromFrontend = Map.of("Origin", "http://localhost:3000");
        Map<String, Object> body = Map.of("username", unknownUser(), "password", "wrong");
        for (int i = 0; i < 5; i++) {
            api.call("POST", "/api/auth/login", null, body, fromFrontend);
        }

        Response limited = api.call("POST", "/api/auth/login", null, body, fromFrontend);
        assertEquals(429, limited.status());
        assertEquals("http://localhost:3000", limited.header("Access-Control-Allow-Origin"));
    }
}
