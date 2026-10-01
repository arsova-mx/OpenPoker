package com.openpoker.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Límites de intentos (app.rate-limit.*). Cada política es un token bucket: permite "capacity"
 * intentos seguidos y los recupera de forma gradual a lo largo de "period".
 * Se puede ajustar por variable de entorno, p. ej. APP_RATELIMIT_LOGIN_CAPACITY=20.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Policy login,
        Policy register,
        Policy guestJoin,
        Policy loginFailures
) {

    public RateLimitProperties {
        login = login != null ? login : new Policy(10, Duration.ofMinutes(1));
        register = register != null ? register : new Policy(5, Duration.ofMinutes(10));
        guestJoin = guestJoin != null ? guestJoin : new Policy(30, Duration.ofMinutes(1));
        loginFailures = loginFailures != null ? loginFailures : new Policy(5, Duration.ofMinutes(5));
    }

    public record Policy(int capacity, Duration period) {
        public Policy {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity debe ser al menos 1");
            }
            if (period == null || period.isZero() || period.isNegative()) {
                throw new IllegalArgumentException("period debe ser una duración positiva");
            }
        }
    }
}
