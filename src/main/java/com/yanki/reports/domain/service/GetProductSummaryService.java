package com.yanki.reports.domain.service;

import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.in.GetProductSummaryUseCase;
import com.yanki.reports.domain.port.out.ProductSummaryPersistencePort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso para obtener el resumen de productos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProductSummaryService implements GetProductSummaryUseCase {

    private final ProductSummaryPersistencePort persistencePort;

    @Override
    public Maybe<ProductSummary> executeByProductId(String productId) {
        log.info("Ejecutando consulta de resumen de producto por ID: productId={}", productId);

        try {
            return persistencePort.findByProductId(productId)
                    .doOnSuccess(summary -> {
                        log.info("Resumen de producto obtenido exitosamente: productId={}, productType={}, status={}",
                                productId, summary.getProductType(), summary.getStatus());
                    })
                    .doOnComplete(() ->
                            log.debug("Consulta de resumen de producto completada: productId={}", productId)
                    )
                    .doOnError(e -> log.error("Error obteniendo resumen de producto: productId={}, error={}",
                            productId, e.getMessage(), e));
        } catch (Exception ex) {
            log.error("Error inesperado en executeByProductId(): productId={}", productId, ex);
            throw ex;
        }
    }

    @Override
    public Flowable<ProductSummary> executeByCustomerId(String customerId) {
        log.info("Ejecutando consulta de resúmenes de producto por cliente: customerId={}", customerId);

        try {
            return persistencePort.findByCustomerId(customerId)
                    .doOnSubscribe(s -> log.debug("Suscripción activa para productos del cliente: customerId={}", customerId))
                    .doOnComplete(() -> log.info("Consulta de productos del cliente completada: customerId={}", customerId))
                    .doOnError(e -> log.error("Error obteniendo productos del cliente: customerId={}, error={}",
                            customerId, e.getMessage(), e));
        } catch (Exception ex) {
            log.error("Error inesperado en executeByCustomerId(): customerId={}", customerId, ex);
            throw ex;
        }
    }
}
