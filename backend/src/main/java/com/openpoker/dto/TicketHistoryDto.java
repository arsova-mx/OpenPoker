package com.openpoker.dto;

import java.time.Instant;
import java.util.UUID;
import com.openpoker.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketHistoryDto {
    private UUID id;
    private String title;
    private String description;
    private TicketStatus status;
    private Integer currentRound;
    private Instant finishedAt;
    private String estimatedValue; // El valor de estimatedCard si existe
}