package com.openpoker.dto;

import java.sql.Timestamp;
import java.util.UUID;

public record SessionResponse(UUID id, String sessionCode, String name, String hostUsername, String status, long
participantCount, Timestamp createdAt) {
}
