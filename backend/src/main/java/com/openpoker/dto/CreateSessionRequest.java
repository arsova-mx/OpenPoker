package com.openpoker.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(@NotBlank String name) {
}
