package com.openpoker.dto;

import java.util.UUID;

public record WebSocketParticipantResponse(
    UUID participantId, // 👈 Cambiamos a participantId
    String displayName,  // 👈 Nombre visible (usuario o invitado)
    String role,
    boolean isGuest      // 👈 Para saber si tiene cuenta o no
) {}