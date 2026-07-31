package com.openpoker.dto;

import java.time.Instant;

public record TimerStatusDTO(
    Integer durationSeconds,
    Instant timerExpiresAt,
    boolean isExpired
) {
    // Helper estático para construir el DTO calculando isExpired de una vez
    public static TimerStatusDTO of(Integer durationSeconds, Instant timerExpiresAt) {
        boolean expired = timerExpiresAt != null && Instant.now().isAfter(timerExpiresAt);
        return new TimerStatusDTO(durationSeconds, timerExpiresAt, expired);
    }
}
