package com.yanki.reports.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO para rango de fechas en respuestas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rango de fechas")
public class DateRange {

    @Schema(description = "Fecha inicial (inclusive, ISO-8601)", example = "2026-01-01", format = "date")
    private LocalDate from;

    @Schema(description = "Fecha final (inclusive, ISO-8601)", example = "2026-12-31", format = "date")
    private LocalDate to;
}