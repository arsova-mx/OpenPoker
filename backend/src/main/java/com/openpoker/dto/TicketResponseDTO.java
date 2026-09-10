package com.openpoker.dto;

import java.util.UUID;

import com.openpoker.entity.TicketStatus;

public record TicketResponseDTO(
    UUID id, 
    String title, 
    String description, 
    UUID gameSessionId, // Solo enviamos el ID de la sesión, no el objeto entero
    TicketStatus status
) {

}
