package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.DailyAggregate;
import io.reactivex.rxjava3.core.Flowable;

import java.time.LocalDate;

/**
 * Puerto de entrada para obtener agregados diarios.
 */
public interface GetDailyAggregatesUseCase {

    /**
     * Obtiene los agregados diarios de una fecha específica.
     *
     * @param date fecha del día
     * @return flujo de agregados
     */
    Flowable<DailyAggregate> executeByDate(LocalDate date);

    /**
     * Obtiene los agregados diarios por rango de fechas y tipo de producto.
     *
     * @param from        fecha inicial (inclusive)
     * @param to          fecha final (inclusive)
     * @param productType tipo de producto (opcional, null para todos)
     * @return flujo de agregados
     */
    Flowable<DailyAggregate> executeByDateRange(LocalDate from, LocalDate to, String productType);
}
