package com.openpoker.dto;

import java.util.UUID;

public record VotingDeckResponse(UUID id, String name, String seriesType, String description) {
}
