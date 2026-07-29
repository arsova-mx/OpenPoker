package com.openpoker.dto;

import java.time.Instant;

public record TimerStatusDTO(
    Integer durationSeconds,
    Instant timerExpiresAt,
    boolean isExpired
) {}
