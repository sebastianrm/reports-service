package com.yanki.reports.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad pura de dominio para control de idempotencia de eventos Kafka procesados.
 *
 * <p>La clave única es la combinación {@code <topic>:<aggregateId>:<occurredAt>} que garantiza
 * que un mismo evento no se procese más de una vez, incluso si se reintenta el consumo
 * o se hace backfill (reprocesamiento desde offsets tempranos).
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    /**
     * Clave compuesta única: {@code <topic>:<aggregateId>:<occurredAt>}.
     * Ejemplo: {@code yanki.card.consumption.registered.v1:CARD-123:2026-11-14T10:30:00}
     */
    private String id;

    /**
     * Timestamp del momento en que se procesó y registró el evento.
     */
    private LocalDateTime processedAt;
}