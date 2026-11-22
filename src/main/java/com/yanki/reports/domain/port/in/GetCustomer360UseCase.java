package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.Customer360;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Puerto de entrada para obtener la vista 360° completa de un cliente.
 *
 * <p>Agrega datos de CustomerSummary, ProductSummary y ProductEvent
 * para proporcionar una visión completa del cliente en una sola consulta.
 */
public interface GetCustomer360UseCase {

    /**
     * Obtiene la vista 360° completa de un cliente por su ID.
     *
     * @param customerId identificador del cliente
     * @return vista 360° del cliente o vacío si no existe
     */
    Maybe<Customer360> execute(String customerId);
}
