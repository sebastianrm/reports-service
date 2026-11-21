package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Puerto de entrada para obtener el resumen 360° de un cliente.
 */
public interface GetCustomerSummaryUseCase {

    /**
     * Obtiene el resumen 360° de un cliente por su ID.
     *
     * @param customerId identificador del cliente
     * @return resumen del cliente o vacío si no existe
     */
    Maybe<CustomerSummary> execute(String customerId);
}
