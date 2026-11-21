package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.CardMovementsReport;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada (Use Case) para obtener últimos movimientos de tarjeta.
 *
 * <p><b>Requerimiento obligatorio Parte II:</b> "Implementar un reporte con los últimos 10 movimientos
 * de la tarjeta de débito y de crédito".
 */
public interface GetCardMovementsUseCase {

    /**
     * Obtiene los últimos movimientos de una tarjeta.
     *
     * @param cardId identificador de la tarjeta
     * @param limit  número máximo de movimientos a retornar (default 10, máx 100)
     * @return {@link Single} con los movimientos de la tarjeta
     * @throws com.yanki.reports.domain.exception.ProductNotFoundException si no hay eventos para el cardId
     */
    Single<CardMovementsReport> execute(String cardId, int limit);
}