package com.openpoker.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Token buckets en memoria, uno por clave (p. ej. "login:203.0.113.5").
 *
 * Vive en la memoria de esta instancia, igual que el broker STOMP y el registro de sesiones WebSocket:
 * con varias instancias del backend cada una aplicaría su propio límite.
 */
@Component
public class RateLimiter {

    /** Por encima de este número de claves se descartan los buckets que ya se recargaron por completo. */
    static final int MAX_BUCKETS = 50_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final LongSupplier nanoTime;

    public RateLimiter() {
        this(System::nanoTime);
    }

    RateLimiter(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * Consume un intento. Devuelve vacío si se permite, o cuánto falta para el siguiente intento.
     */
    public Optional<Duration> tryConsume(String key, RateLimitProperties.Policy policy) {
        evictIfNeeded();
        return buckets.computeIfAbsent(key, k -> new Bucket(policy, nanoTime.getAsLong()))
                .tryConsume(nanoTime.getAsLong());
    }

    /**
     * Como tryConsume pero sin consumir: indica si hay al menos un intento disponible.
     */
    public Optional<Duration> check(String key) {
        Bucket bucket = buckets.get(key);
        return bucket == null ? Optional.empty() : bucket.check(nanoTime.getAsLong());
    }

    public void reset(String key) {
        buckets.remove(key);
    }

    /** Solo para tests. */
    void clear() {
        buckets.clear();
    }

    int size() {
        return buckets.size();
    }

    private void evictIfNeeded() {
        if (buckets.size() > MAX_BUCKETS) {
            long now = nanoTime.getAsLong();
            buckets.entrySet().removeIf(entry -> entry.getValue().isFull(now));
        }
    }

    private static final class Bucket {
        private final RateLimitProperties.Policy policy;
        private final double tokensPerNano;
        private double tokens;
        private long lastRefill;

        Bucket(RateLimitProperties.Policy policy, long now) {
            this.policy = policy;
            this.tokensPerNano = policy.capacity() / (double) policy.period().toNanos();
            this.tokens = policy.capacity();
            this.lastRefill = now;
        }

        synchronized Optional<Duration> tryConsume(long now) {
            refill(now);
            if (tokens >= 1) {
                tokens -= 1;
                return Optional.empty();
            }
            return Optional.of(waitFor(1 - tokens));
        }

        synchronized Optional<Duration> check(long now) {
            refill(now);
            return tokens >= 1 ? Optional.empty() : Optional.of(waitFor(1 - tokens));
        }

        synchronized boolean isFull(long now) {
            refill(now);
            return tokens >= policy.capacity();
        }

        private void refill(long now) {
            long elapsed = Math.max(0, now - lastRefill);
            tokens = Math.min(policy.capacity(), tokens + elapsed * tokensPerNano);
            lastRefill = now;
        }

        private Duration waitFor(double missingTokens) {
            return Duration.ofNanos((long) Math.ceil(missingTokens / tokensPerNano));
        }
    }
}
