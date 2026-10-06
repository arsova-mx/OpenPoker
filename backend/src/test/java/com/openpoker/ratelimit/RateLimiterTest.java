package com.openpoker.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    /** 3 intentos que se recuperan en 30 s: uno cada 10 s. */
    private static final RateLimitProperties.Policy POLICY = new RateLimitProperties.Policy(3, Duration.ofSeconds(30));

    private final AtomicLong now = new AtomicLong();
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiter(now::get);
    }

    private void advance(Duration duration) {
        now.addAndGet(duration.toNanos());
    }

    @Test
    @DisplayName("Permite hasta la capacidad y luego indica cuánto esperar")
    void allowsUpToCapacity() {
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryConsume("login:1.2.3.4", POLICY).isEmpty(), "Intento " + (i + 1));
        }

        Optional<Duration> retryAfter = limiter.tryConsume("login:1.2.3.4", POLICY);
        assertTrue(retryAfter.isPresent());
        assertEquals(10, retryAfter.get().toSeconds(), "Un intento se recupera cada 10 s");
    }

    @Test
    @DisplayName("Los intentos se recuperan de forma gradual")
    void refillsGradually() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("k", POLICY);
        }
        advance(Duration.ofSeconds(10));

        assertTrue(limiter.tryConsume("k", POLICY).isEmpty(), "Tras 10 s debe haber un intento disponible");
        assertTrue(limiter.tryConsume("k", POLICY).isPresent(), "Pero solo uno");
    }

    @Test
    @DisplayName("Nunca acumula más intentos que la capacidad")
    void neverExceedsCapacity() {
        advance(Duration.ofHours(1));

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryConsume("k", POLICY).isEmpty());
        }
        assertTrue(limiter.tryConsume("k", POLICY).isPresent());
    }

    @Test
    @DisplayName("Cada clave tiene su propio límite")
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("login:1.1.1.1", POLICY);
        }

        assertTrue(limiter.tryConsume("login:1.1.1.1", POLICY).isPresent());
        assertTrue(limiter.tryConsume("login:2.2.2.2", POLICY).isEmpty());
    }

    @Test
    @DisplayName("check no consume intentos y reset los restablece")
    void checkAndReset() {
        assertTrue(limiter.check("k").isEmpty(), "Una clave sin uso no está limitada");
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("k", POLICY);
        }
        assertTrue(limiter.check("k").isPresent());
        assertTrue(limiter.check("k").isPresent(), "check no cambia el estado");

        limiter.reset("k");
        assertTrue(limiter.check("k").isEmpty());
    }

    @Test
    @DisplayName("Con demasiadas claves descarta las que ya se recargaron por completo")
    void evictsRefilledBuckets() {
        for (int i = 0; i <= RateLimiter.MAX_BUCKETS; i++) {
            limiter.tryConsume("ip:" + i, POLICY);
        }
        advance(Duration.ofSeconds(30));

        limiter.tryConsume("new-key", POLICY);

        assertTrue(limiter.size() < 10, "Quedaron " + limiter.size() + " claves");
    }
}
