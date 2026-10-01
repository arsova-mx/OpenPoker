package com.openpoker.globalexception;

import java.time.Duration;

/** Se superó un límite de intentos (HTTP 429). */
public class TooManyRequestsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyRequestsException(Duration retryAfter) {
        this(Math.max(1, (long) Math.ceil(retryAfter.toMillis() / 1000.0)));
    }

    private TooManyRequestsException(long retryAfterSeconds) {
        super(message(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static String message(long retryAfterSeconds) {
        if (retryAfterSeconds < 60) {
            return "Demasiados intentos. Intenta de nuevo en " + retryAfterSeconds + " segundos.";
        }
        long minutes = (long) Math.ceil(retryAfterSeconds / 60.0);
        return "Demasiados intentos. Intenta de nuevo en " + minutes + (minutes == 1 ? " minuto." : " minutos.");
    }
}
