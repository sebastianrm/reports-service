package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.AggregatedReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de respuesta para reporte agregado.
 *
 * <p>Adaptador de salida REST: convierte el modelo de dominio a formato de respuesta HTTP.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reporte agregado por tipo de producto y rango de fechas")
public class AggregatedReportResponse {

    @Schema(description = "Cantidad total de eventos en el rango", example = "120")
    private long count;

    @Schema(description = "Suma total de montos de todos los eventos", example = "24000.50")
    private BigDecimal sumAmount;

    @Schema(description = "Promedio de montos (redondeado a 2 decimales)", example = "200.00")
    private BigDecimal averageAmount;

    @Schema(description = "Tipo de producto consultado", example = "ACCOUNT", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET"})
    private String type;

    @Schema(description = "Rango de fechas consultado")
    private DateRange dateRange;

    @Schema(description = "Tipo de cliente filtrado", example = "NATURAL", allowableValues = {"NATURAL", "JURIDICA"})
    private String customerType;

    /**
     * Convierte modelo de dominio a DTO de respuesta.
     */
    public static AggregatedReportResponse fromDomain(AggregatedReport domain) {
        return AggregatedReportResponse.builder()
                .count(domain.getCount())
                .sumAmount(domain.getSumAmount())
                .averageAmount(domain.getAverageAmount())
                .type(domain.getType())
                .dateRange(DateRange.builder().from(domain.getFrom()).to(domain.getTo()).build())
                .customerType(domain.getCustomerType())
                .build();
    }
}