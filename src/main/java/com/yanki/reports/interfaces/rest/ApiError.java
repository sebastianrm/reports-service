package com.yanki.reports.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato unificado de respuesta de error para la API.
 *
 * <p>Estandariza todos los errores HTTP devueltos por el servicio.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de error estandarizada")
public class ApiError {

    @Schema(description = "Timestamp del error", example = "2026-11-14T18:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Código HTTP", example = "404")
    private int status;

    @Schema(description = "Texto del error HTTP", example = "Not Found")
    private String error;

    @Schema(description = "Código de error técnico", example = "PRODUCT_NOT_FOUND")
    private String code;

    @Schema(description = "Mensaje descriptivo para el cliente", example = "No report data found for cardId CARD-123")
    private String message;

    @Schema(description = "Ruta del endpoint que generó el error", example = "/api/reports/movements/cards/CARD-123")
    private String path;

    @Schema(description = "Detalles adicionales (lista vacía por defecto)")
    private List<String> details;
}