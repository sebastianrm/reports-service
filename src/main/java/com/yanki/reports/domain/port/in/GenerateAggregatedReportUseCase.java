package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.AggregatedReport;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDate;

/**
 * Puerto de entrada (Use Case) para generar reporte agregado por tipo de producto.
 *
 * <p>Define el contrato para obtener métricas consolidadas de eventos para un tipo de producto
 * específico dentro de un rango de fechas y tipo de cliente.
 */
public interface GenerateAggregatedReportUseCase {

    /**
     * Genera un reporte agregado.
     *
     * @param productType   tipo de producto (ACCOUNT, CARD, CREDIT, WALLET)
     * @param from          fecha inicial del rango (inclusive)
     * @param to            fecha final del rango (inclusive)
     * @param customerType  tipo de cliente (NATURAL, JURIDICA)
     * @return {@link Single} con el reporte agregado
     */
    Single<AggregatedReport> execute(String productType, LocalDate from, LocalDate to, String customerType);
}