package com.openpoker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportEstimationRequest {
    private String personalAccessToken; // Token con permisos de repo / issues para comentar
    private String customComment;       // Opcional por si el usuario quiere editar el texto antes de enviar
}