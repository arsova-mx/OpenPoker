package com.openpoker.dto;

import java.util.UUID;

/** Datos de un usuario visibles para otros usuarios: sin email, teléfono ni empresa. */
public record PublicUserResponse(UUID id, String username) {
}
