package com.openpoker.dto;

import java.util.UUID;

public record WebSocketParticipantResponse(UUID userId, String username, String role) {
}
