package com.yanki.reports.infrastructure.persistence.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB para la colección {@code processed_events} (idempotencia).
 *
 * <p>Clave única: {@code <topic>:<aggregateId>:<occurredAt>}.
 * Ejemplo: {@code yanki.card.consumption.registered.v1:CARD-123:2026-11-14T10:30:00}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_events")
public class ProcessedEventDocument {

    @Id
    private String id; // <topic>:<aggregateId>:<occurredAt>

    private LocalDateTime processedAt;
}