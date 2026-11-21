package com.yanki.reports.domain.port.out;

import com.yanki.reports.domain.model.ProductEvent;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de salida para persistencia de eventos de producto.
 *
 * <p>Define el contrato para almacenar y consultar eventos en la base de datos.
 * Las implementaciones usan Spring Data MongoDB Reactive internamente
 * pero exponen tipos reactivos de RxJava 3 hacia el dominio.
 */
public interface ProductEventPersistencePort {

    /**
     * Guarda un evento de producto.
     *
     * @param event evento a persistir
     * @return evento persistido
     */
    Single<ProductEvent> save(ProductEvent event);

    /**
     * Guarda múltiples eventos en lote.
     *
     * @param events lista de eventos a persistir
     * @return lista de eventos persistidos
     */
    Single<List<ProductEvent>> saveAll(List<ProductEvent> events);

    /**
     * Busca un evento por su ID.
     *
     * @param id identificador del evento
     * @return evento encontrado o vacío
     */
    Maybe<ProductEvent> findById(String id);

    /**
     * Busca eventos por aggregateId ordenados por occurredAt descendente.
     *
     * @param aggregateId identificador del agregado (productId, cardId, etc.)
     * @return flujo de eventos
     */
    Flowable<ProductEvent> findByAggregateIdOrderByOccurredAtDesc(String aggregateId);

    /**
     * Busca eventos por productType y rango de fechas, ordenados por occurredAt descendente.
     *
     * @param productType tipo de producto
     * @param from        fecha inicial (inclusive)
     * @param to          fecha final (inclusive)
     * @return flujo de eventos
     */
    Flowable<ProductEvent> findByProductTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            String productType, LocalDateTime from, LocalDateTime to);

    /**
     * Busca todos los eventos en un rango de fechas, ordenados por occurredAt descendente.
     *
     * @param from fecha inicial (inclusive)
     * @param to   fecha final (inclusive)
     * @return flujo de eventos
     */
    Flowable<ProductEvent> findByOccurredAtBetweenOrderByOccurredAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * Busca eventos por customerId ordenados por occurredAt descendente.
     *
     * @param customerId identificador del cliente
     * @return flujo de eventos
     */
    Flowable<ProductEvent> findByCustomerIdOrderByOccurredAtDesc(String customerId);

    /**
     * Verifica si existe algún evento para un aggregateId.
     *
     * @param aggregateId identificador del agregado
     * @return true si existe al menos un evento
     */
    Single<Boolean> existsByAggregateId(String aggregateId);
}