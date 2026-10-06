package com.openpoker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Orígenes del frontend que pueden llamar a la API REST y abrir el WebSocket desde el navegador.
 * Se configura con ALLOWED_ORIGINS (separados por coma). Acepta orígenes exactos
 * ("https://openpoker.arsova.mx") y patrones ("https://*.pages.dev" para previews).
 * Las peticiones del mismo origen siempre se permiten.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    public boolean allowsAnyOrigin() {
        return allowedOrigins.contains("*");
    }
}
