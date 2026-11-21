package com.yanki.reports.infrastructure.persistence.mapper;

import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.infrastructure.persistence.document.ProductEventDocument;
import java.util.Map;
import java.util.Objects;

/**
 * Mapper estático para conversión bidireccional entre modelo de dominio e infraestructura.
 *
 * <p>Separa completamente el modelo puro de dominio ({@link ProductEvent}) del documento
 * MongoDB ({@link ProductEventDocument}) anotado con Spring Data.
 */
public final class ProductEventDocumentMapper {

    private ProductEventDocumentMapper() {
        // Utility class
    }

    /**
     * Convierte documento de infraestructura a modelo de dominio.
     *
     * @param document documento MongoDB (puede ser null)
     * @return modelo de dominio o null si el documento es null
     */
    public static ProductEvent toDomain(ProductEventDocument document) {
        if (document == null) {
            return null;
        }
        return ProductEvent.builder()
                .id(document.getId())
                .eventType(document.getEventType())
                .aggregateId(document.getAggregateId())
                .customerId(document.getCustomerId())
                .productType(document.getProductType())
                .subType(document.getSubType())
                .amount(document.getAmount())
                .balanceAfter(document.getBalanceAfter())
                .metadata(document.getMetadata())
                .occurredAt(document.getOccurredAt())
                .ingestedAt(document.getIngestedAt())
                .build();
    }

    /**
     * Convierte modelo de dominio a documento de infraestructura.
     *
     * @param domain modelo de dominio (puede ser null)
     * @return documento MongoDB o null si el modelo es null
     */
    public static ProductEventDocument toDocument(ProductEvent domain) {
        if (domain == null) {
            return null;
        }
        return ProductEventDocument.builder()
                .id(domain.getId())
                .eventType(domain.getEventType())
                .aggregateId(domain.getAggregateId())
                .customerId(domain.getCustomerId())
                .productType(domain.getProductType())
                .subType(domain.getSubType())
                .amount(domain.getAmount())
                .balanceAfter(domain.getBalanceAfter())
                .metadata(Objects.requireNonNullElseGet(domain.getMetadata(), Map::of))
                .occurredAt(domain.getOccurredAt())
                .ingestedAt(domain.getIngestedAt())
                .build();
    }
}