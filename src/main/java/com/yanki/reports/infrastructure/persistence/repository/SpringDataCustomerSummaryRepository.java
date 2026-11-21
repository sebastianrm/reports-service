package com.yanki.reports.infrastructure.persistence.repository;

import com.yanki.reports.infrastructure.persistence.document.CustomerSummaryDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data Reactivo para {@link CustomerSummaryDocument}.
 *
 * <p>Define métodos de consulta derivados (sin {@code @Query}) para la colección {@code customer_summary}.
 * Esta interfaz es parte de la INFRAESTRUCTURA y usa tipos Reactor ({@code Mono}, {@code Flux}).
 * La conversión a tipos RxJava 3 del dominio se hace en el adaptador.
 */
public interface SpringDataCustomerSummaryRepository extends ReactiveMongoRepository<CustomerSummaryDocument, String> {

    /**
     * Busca resumen por customerId.
     */
    Mono<CustomerSummaryDocument> findByCustomerId(String customerId);
}
