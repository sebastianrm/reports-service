package com.yanki.reports.domain.port.out;

import com.yanki.reports.domain.model.DailyAggregate;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

import java.time.LocalDate;

/**
 * Puerto de salida para persistencia de agregados diarios.
 *
 * <p>Define el contrato para almacenar y consultar agregados diarios en la base de datos.
 * Las implementaciones usan Spring Data MongoDB Reactive internamente
 * pero exponen tipos reactivos de RxJava 3 hacia el dominio.
 */
public interface DailyAggregatePersistencePort {

    /**
     * Guarda o actualiza un agregado diario (upsert).
     *
     * @param aggregate agregado a persistir
     * @return agregado persistido
     */
    Single<DailyAggregate> save(DailyAggregate aggregate);

    /**
     * Busca un agregado por fecha y tipo de producto.
     *
     * @param date        fecha del día
     * @param productType tipo de producto
     * @return agregado encontrado o vacío
     */
    Maybe<DailyAggregate> findByDateAndProductType(LocalDate date, String productType);

    /**
     * Busca agregados por fecha.
     *
     * @param date fecha del día
     * @return flujo de agregados
     */
    Flowable<DailyAggregate> findByDate(LocalDate date);

    /**
     * Busca agregados por fecha y tipo de cliente.
     *
     * @param date         fecha del día
     * @param customerType tipo de cliente
     * @return flujo de agregados
     */
    Flowable<DailyAggregate> findByDateAndCustomerType(LocalDate date, String customerType);

    /**
     * Busca agregados por rango de fechas y tipo de producto, ordenados por fecha descendente.
     *
     * @param from        fecha inicial (inclusive)
     * @param to          fecha final (inclusive)
     * @param productType tipo de producto
     * @return flujo de agregados
     */
    Flowable<DailyAggregate> findByDateBetweenAndProductTypeOrderByDateDesc(
            LocalDate from, LocalDate to, String productType);
}
