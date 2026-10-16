package com.openpoker.dto;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.openpoker.entity.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionHistorySummaryDto {
    private UUID sessionId;
    private String sessionCode;
    private String name;
    private SessionStatus status;
    private Timestamp createdAt;
    private Instant closedAt;
    private boolean isHost;
    private long totalTickets;
    private long estimatedTickets;
}