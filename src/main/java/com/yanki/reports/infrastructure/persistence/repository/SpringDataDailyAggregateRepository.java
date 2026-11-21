package com.yanki.reports.infrastructure.persistence.repository;

import com.yanki.reports.infrastructure.persistence.document.DailyAggregateDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Repositorio Spring Data Reactivo para {@link DailyAggregateDocument}.
 *
 * <p>Define métodos de consulta derivados (sin {@code @Query}) para la colección {@code daily_aggregates}.
 * Esta interfaz es parte de la INFRAESTRUCTURA y usa tipos Reactor ({@code Mono}, {@code Flux}).
 * La conversión a tipos RxJava 3 del dominio se hace en el adaptador.
 */
public interface SpringDataDailyAggregateRepository extends ReactiveMongoRepository<DailyAggregateDocument, String> {

    /**
     * Busca agregado por fecha y tipo de producto.
     */
    Mono<DailyAggregateDocument> findByDateAndProductType(LocalDate date, String productType);

    /**
     * Busca agregados por fecha.
     */
    Flux<DailyAggregateDocument> findByDate(LocalDate date);

    /**
     * Busca agregados por fecha y tipo de cliente.
     */
    Flux<DailyAggregateDocument> findByDateAndCustomerType(LocalDate date, String customerType);

    /**
     * Busca agregados por rango de fechas y tipo de producto.
     */
    Flux<DailyAggregateDocument> findByDateBetweenAndProductTypeOrderByDateDesc(
            LocalDate from, LocalDate to, String productType);
}
