package com.openpoker.dto;

import java.util.UUID;

public interface SessionTicketCountsProjection {
    UUID getSessionId();
    long getTotalTickets();
    long getEstimatedTickets();
}