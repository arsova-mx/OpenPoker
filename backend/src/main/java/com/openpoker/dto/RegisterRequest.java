package com.openpoker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$",
            message = "El usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo")
    String username,
    @Email @NotBlank String email,
    @NotBlank String password
){}
