package com.openpoker.dto;

import java.sql.Timestamp;
import java.util.UUID;

public record SessionResponse(UUID id, String sessionCode, String name, String hostUsername,long
participantCount, Timestamp createdAt) {
}
