package com.openpoker.ratelimit;

import com.openpoker.globalexception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Cuenta los logins fallidos por username, sin importar la IP: frena la fuerza bruta distribuida
 * contra una misma cuenta. El bloqueo es temporal (app.rate-limit.login-failures) y un login correcto
 * reinicia el contador.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    /** Lanza 429 si la cuenta tiene demasiados intentos fallidos recientes. */
    public void ensureNotLocked(String username) {
        if (!properties.enabled() || username == null) {
            return;
        }
        rateLimiter.check(key(username)).ifPresent(retryAfter -> {
            throw new TooManyRequestsException(retryAfter);
        });
    }

    public void recordFailure(String username) {
        if (properties.enabled() && username != null) {
            rateLimiter.tryConsume(key(username), properties.loginFailures());
        }
    }

    public void recordSuccess(String username) {
        if (username != null) {
            rateLimiter.reset(key(username));
        }
    }

    private static String key(String username) {
        return "login-failures:" + username.toLowerCase(Locale.ROOT);
    }
}
