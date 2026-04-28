package com.openpoker.dto;

import java.util.UUID;

public record AuthResponse(String token, UUID id, String username) {
}
