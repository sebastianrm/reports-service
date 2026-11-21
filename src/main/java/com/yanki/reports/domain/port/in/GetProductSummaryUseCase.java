package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.ProductSummary;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Puerto de entrada para obtener el resumen de productos.
 */
public interface GetProductSummaryUseCase {

    /**
     * Obtiene el resumen de un producto por su ID.
     *
     * @param productId identificador del producto
     * @return resumen del producto o vacío si no existe
     */
    Maybe<ProductSummary> executeByProductId(String productId);

    /**
     * Obtiene los resúmenes de todos los productos de un cliente.
     *
     * @param customerId identificador del cliente
     * @return flujo de resúmenes de productos
     */
    Flowable<ProductSummary> executeByCustomerId(String customerId);
}
