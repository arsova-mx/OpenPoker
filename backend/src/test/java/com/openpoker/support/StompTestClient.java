package com.openpoker.support;

import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Conexión STOMP real contra /ws-native para tests de integración.
 * Los payloads viajan como JSON en bytes para no depender de un conversor de Jackson en el cliente.
 */
public class StompTestClient implements AutoCloseable {

    private final StompSession session;
    /** Frames ERROR del servidor y errores de transporte (p. ej. conexión cerrada tras un rechazo). */
    private final BlockingQueue<String> errors;

    private StompTestClient(StompSession session, BlockingQueue<String> errors) {
        this.session = session;
        this.errors = errors;
    }

    public static StompTestClient connect(int port, String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new SimpleMessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }

        BlockingQueue<String> errors = new LinkedBlockingQueue<>();
        StompSession session = client.connectAsync(
                "ws://localhost:" + port + "/ws-native",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return byte[].class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        errors.add("ERROR frame: " + headers.getFirst("message"));
                    }

                    @Override
                    public void handleTransportError(StompSession s, Throwable exception) {
                        errors.add("Transport error: " + exception);
                    }
                }).get(5, TimeUnit.SECONDS);

        return new StompTestClient(session, errors);
    }

    /** Suscribe y devuelve la cola donde se acumulan los mensajes (como String JSON). */
    public BlockingQueue<String> subscribe(String destination) {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add(new String((byte[]) payload, StandardCharsets.UTF_8));
            }
        });
        return messages;
    }

    public void send(String destination, String json) {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
        session.send(headers, json.getBytes(StandardCharsets.UTF_8));
    }

    /** Espera a que el servidor rechace la conexión (frame ERROR o cierre). */
    public boolean awaitRejection(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (!errors.isEmpty() || !session.isConnected()) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    @Override
    public void close() {
        if (session.isConnected()) {
            session.disconnect();
        }
    }
}
