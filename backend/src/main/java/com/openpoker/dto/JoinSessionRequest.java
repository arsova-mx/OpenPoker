package com.openpoker.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinSessionRequest(@NotBlank String sessionCode) {
}
