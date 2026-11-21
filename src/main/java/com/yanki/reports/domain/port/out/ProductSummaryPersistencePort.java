package com.yanki.reports.domain.port.out;

import com.yanki.reports.domain.model.ProductSummary;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida para persistencia del resumen de productos.
 *
 * <p>Define el contrato para almacenar y consultar resúmenes de producto en la base de datos.
 * Las implementaciones usan Spring Data MongoDB Reactive internamente
 * pero exponen tipos reactivos de RxJava 3 hacia el dominio.
 */
public interface ProductSummaryPersistencePort {

    /**
     * Guarda o actualiza un resumen de producto (upsert).
     *
     * @param summary resumen a persistir
     * @return resumen persistido
     */
    Single<ProductSummary> save(ProductSummary summary);

    /**
     * Busca un resumen por productId.
     *
     * @param productId identificador del producto
     * @return resumen encontrado o vacío
     */
    Maybe<ProductSummary> findByProductId(String productId);

    /**
     * Busca resúmenes por customerId.
     *
     * @param customerId identificador del cliente
     * @return flujo de resúmenes
     */
    Flowable<ProductSummary> findByCustomerId(String customerId);

    /**
     * Busca resúmenes por customerId y productType.
     *
     * @param customerId  identificador del cliente
     * @param productType tipo de producto
     * @return flujo de resúmenes
     */
    Flowable<ProductSummary> findByCustomerIdAndProductType(String customerId, String productType);
}
