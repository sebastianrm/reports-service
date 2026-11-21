package com.yanki.reports.domain.port.out;

import com.yanki.reports.domain.model.ProcessedEvent;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida para control de idempotencia de eventos procesados.
 *
 * <p>La clave única es {@code <topic>:<aggregateId>:<occurredAt>}.
 * Garantiza exactly-once lógico en la proyección de eventos.
 */
public interface ProcessedEventPort {

    /**
     * Verifica si un evento ya fue procesado.
     *
     * @param eventId clave compuesta {@code <topic>:<aggregateId>:<occurredAt>}
     * @return {@link Maybe} con el evento procesado si existe, vacío si no
     */
    Maybe<ProcessedEvent> findById(String eventId);

    /**
     * Registra un evento como procesado.
     *
     * @param processedEvent evento procesado a registrar
     * @return {@link Completable} que completa cuando se persiste
     */
    Completable save(ProcessedEvent processedEvent);

    /**
     * Verifica y registra atómicamente (compare-and-set).
     * Retorna true si el evento era nuevo y se registró, false si ya existía.
     *
     * @param processedEvent evento a registrar
     * @return true si se registró (nuevo), false si ya existía (duplicado)
     */
    Single<Boolean> tryRegister(ProcessedEvent processedEvent);
}