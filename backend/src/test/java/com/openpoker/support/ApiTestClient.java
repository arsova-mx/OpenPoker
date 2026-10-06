package com.openpoker.support;

import com.fasterxml.jackson.databind.ObjectMapper; // ✅ Correcto

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente HTTP mínimo para tests de integración contra el servidor levantado en un puerto aleatorio.
 * Usa java.net.http para no depender de módulos de test adicionales.
 */
public class ApiTestClient {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public ApiTestClient(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public record Response(int status, String body, ObjectMapper json, Map<String, List<String>> headers) {
        public String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .flatMap(entry -> entry.getValue().stream())
                    .findFirst()
                    .orElse(null);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> asMap() {
            return json.readValue(body, Map.class);
        }

        public String field(String name) {
            Object value = asMap().get(name);
            return value == null ? null : value.toString();
        }
    }

    public record TestUser(String username, String email, UUID id, String token) {
    }

    public Response call(String method, String path, String token, Object body) {
        return call(method, path, token, body, Map.of());
    }

    public Response call(String method, String path, String token, Object body, Map<String, String> extraHeaders) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json");
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            extraHeaders.forEach(request::header);
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
            request.method(method, publisher);

            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), json, response.headers().map());
        } catch (IOException e) {
            throw new IllegalStateException("Fallo la petición " + method + " " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Registra un usuario nuevo con nombre único y devuelve su token. */
    public TestUser registerUser(String prefix) {
        String username = prefix + COUNTER.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);
        String email = username + "@example.com";
        Response response = call("POST", "/api/auth/register", null,
                Map.of("username", username, "email", email, "password", "secret-123"));
        if (response.status() != 201) {
            throw new IllegalStateException("Registro falló (" + response.status() + "): " + response.body());
        }
        return new TestUser(username, email, UUID.fromString(response.field("id")), response.field("token"));
    }

    /** Crea una sala con la baraja por defecto y devuelve el cuerpo de SessionResponse. */
    public Map<String, Object> createSession(TestUser host, String name) {
        Response response = call("POST", "/api/sessions", host.token(), Map.of("name", name));
        if (response.status() != 201) {
            throw new IllegalStateException("Crear sala falló (" + response.status() + "): " + response.body());
        }
        return response.asMap();
    }

    public Map<String, Object> createTicket(TestUser host, String sessionId, String title) {
        Response response = call("POST", "/api/tickets", host.token(),
                Map.of("title", title, "gameSessionId", sessionId));
        if (response.status() != 200 && response.status() != 201) {
            throw new IllegalStateException("Crear ticket falló (" + response.status() + "): " + response.body());
        }
        return response.asMap();
    }
}
