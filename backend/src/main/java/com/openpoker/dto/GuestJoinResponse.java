package com.openpoker.dto;

import java.util.UUID;

/**
 * Resultado de unirse como invitado. El guestToken se envía como "Authorization: Bearer ..." en el
 * CONNECT de STOMP; identifica al invitado solo dentro de esta sala y no sirve para la API REST.
 */
public record GuestJoinResponse(
        SessionResponse session,
        UUID participantId,
        String displayName,
        String guestToken
) {
}
