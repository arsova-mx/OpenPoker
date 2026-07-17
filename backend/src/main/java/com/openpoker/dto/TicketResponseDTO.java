package com.openpoker.dto;

import java.util.UUID;

public record TicketResponseDTO(
    UUID id, 
    String title, 
    String description, 
    UUID gameSessionId // Solo enviamos el ID de la sesión, no el objeto entero
) {

}
