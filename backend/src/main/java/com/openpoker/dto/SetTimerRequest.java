package com.openpoker.dto;

public record SetTimerRequest(
    Integer durationSeconds // ej: 30, 60, 120, o null/0 para "sin límite"
) {}
