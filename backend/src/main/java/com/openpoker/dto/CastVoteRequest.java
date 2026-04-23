package com.openpoker.dto;

import jakarta.validation.constraints.NotBlank;

public record CastVoteRequest(@NotBlank String cardValue) {
}
