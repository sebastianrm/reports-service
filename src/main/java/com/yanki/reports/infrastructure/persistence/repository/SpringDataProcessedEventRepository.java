package com.yanki.reports.infrastructure.persistence.repository;

import com.yanki.reports.infrastructure.persistence.document.ProcessedEventDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio Spring Data Reactivo para {@link ProcessedEventDocument} (idempotencia).
 *
 * <p>Colección {@code processed_events} con clave única {@code <topic>:<aggregateId>:<occurredAt>}.
 */
public interface SpringDataProcessedEventRepository extends ReactiveMongoRepository<ProcessedEventDocument, String> {

    /**
     * Busca un evento procesado por su ID compuesto.
     */
    Mono<ProcessedEventDocument> findById(String id);

    /**
     * Verifica si existe un evento procesado (para idempotencia).
     */
    Mono<Boolean> existsById(String id);
}