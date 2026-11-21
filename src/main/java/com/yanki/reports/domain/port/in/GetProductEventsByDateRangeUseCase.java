package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.ProductEvent;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;

/**
 * Puerto de entrada (Use Case) para obtener eventos de producto por rango de fechas.
 *
 * <p>Retorna un flujo reactivo (Flowable) de eventos para auditoría y debugging.
 * El streaming evita cargar todos los eventos en memoria.
 */
public interface GetProductEventsByDateRangeUseCase {

    /**
     * Obtiene todos los eventos en un rango de fechas.
     *
     * @param from fecha inicial (inclusive)
     * @param to   fecha final (inclusive)
     * @return {@link Flowable} de eventos ordenados por occurredAt descendente
     */
    Flowable<ProductEvent> execute(LocalDate from, LocalDate to);
}