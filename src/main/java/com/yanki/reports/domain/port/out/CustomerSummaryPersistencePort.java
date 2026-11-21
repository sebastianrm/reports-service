package com.yanki.reports.domain.port.out;

import com.yanki.reports.domain.model.CustomerSummary;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida para persistencia del resumen 360° del cliente.
 *
 * <p>Define el contrato para almacenar y consultar resúmenes de cliente en la base de datos.
 * Las implementaciones usan Spring Data MongoDB Reactive internamente
 * pero exponen tipos reactivos de RxJava 3 hacia el dominio.
 */
public interface CustomerSummaryPersistencePort {

    /**
     * Guarda o actualiza un resumen de cliente (upsert).
     *
     * @param summary resumen a persistir
     * @return resumen persistido
     */
    Single<CustomerSummary> save(CustomerSummary summary);

    /**
     * Busca un resumen por customerId.
     *
     * @param customerId identificador del cliente
     * @return resumen encontrado o vacío
     */
    Maybe<CustomerSummary> findByCustomerId(String customerId);
}
