package com.openpoker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestJoinRequest(
        @NotBlank(message = "Escribe un nombre para mostrar")
        @Size(min = 2, max = 30, message = "El nombre debe tener entre 2 y 30 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N} ._-]+$",
                message = "El nombre solo puede tener letras, números, espacios, punto, guion o guion bajo")
        String guestName
) {
}
