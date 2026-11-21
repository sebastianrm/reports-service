package com.yanki.reports.infrastructure.persistence.repository;

import com.yanki.reports.infrastructure.persistence.document.ProductEventDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repositorio Spring Data Reactivo para {@link ProductEventDocument}.
 *
 * <p>Define métodos de consulta derivados (sin {@code @Query}) para la colección {@code product_events}.
 * Esta interfaz es parte de la INFRAESTRUCTURA y usa tipos Reactor ({@code Mono}, {@code Flux}).
 * La conversión a tipos RxJava 3 del dominio se hace en el adaptador.
 */
public interface SpringDataProductEventRepository extends ReactiveMongoRepository<ProductEventDocument, String> {

    /**
     * Busca eventos por aggregateId ordenados por occurredAt descendente.
     */
    Flux<ProductEventDocument> findByAggregateIdOrderByOccurredAtDesc(String aggregateId);

    /**
     * Busca eventos por productType y rango de fechas, ordenados por occurredAt descendente.
     */
    Flux<ProductEventDocument> findByProductTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            String productType, LocalDateTime from, LocalDateTime to);

    /**
     * Busca eventos en rango de fechas, ordenados por occurredAt descendente.
     */
    Flux<ProductEventDocument> findByOccurredAtBetweenOrderByOccurredAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * Busca eventos por customerId ordenados por occurredAt descendente.
     */
    Flux<ProductEventDocument> findByCustomerIdOrderByOccurredAtDesc(String customerId);

    /**
     * Verifica si existe algún evento para un aggregateId.
     */
    Mono<Boolean> existsByAggregateId(String aggregateId);
}