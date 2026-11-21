package com.yanki.reports.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de dominio para reporte agregado.
 *
 * <p>Representa métricas consolidadas de eventos para análisis ejecutivo y KPIs.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedReport {

    private long count;
    private BigDecimal sumAmount;
    private BigDecimal averageAmount;
    private String type;
    private LocalDate from;
    private LocalDate to;
    private String customerType;
}