package com.yanki.reports.infrastructure.persistence.repository;

import com.yanki.reports.infrastructure.persistence.document.ProductSummaryDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data Reactivo para {@link ProductSummaryDocument}.
 *
 * <p>Define métodos de consulta derivados (sin {@code @Query}) para la colección {@code product_summary}.
 * Esta interfaz es parte de la INFRAESTRUCTURA y usa tipos Reactor ({@code Mono}, {@code Flux}).
 * La conversión a tipos RxJava 3 del dominio se hace en el adaptador.
 */
public interface SpringDataProductSummaryRepository extends ReactiveMongoRepository<ProductSummaryDocument, String> {

    /**
     * Busca resumen por productId.
     */
    Mono<ProductSummaryDocument> findByProductId(String productId);

    /**
     * Busca resúmenes por customerId.
     */
    Flux<ProductSummaryDocument> findByCustomerId(String customerId);

    /**
     * Busca resúmenes por customerId y productType.
     */
    Flux<ProductSummaryDocument> findByCustomerIdAndProductType(String customerId, String productType);
}
