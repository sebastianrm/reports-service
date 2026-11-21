package com.yanki.reports.infrastructure.persistence.mapper;

import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.infrastructure.persistence.document.DailyAggregateDocument;

/**
 * Mapper estático para conversión bidireccional entre modelo de dominio e infraestructura.
 *
 * <p>Separa completamente el modelo puro de dominio ({@link DailyAggregate}) del documento
 * MongoDB ({@link DailyAggregateDocument}) anotado con Spring Data.
 */
public final class DailyAggregateDocumentMapper {

    private DailyAggregateDocumentMapper() {
        // Utility class
    }

    /**
     * Convierte documento de infraestructura a modelo de dominio.
     *
     * @param document documento MongoDB (puede ser null)
     * @return modelo de dominio o null si el documento es null
     */
    public static DailyAggregate toDomain(DailyAggregateDocument document) {
        if (document == null) {
            return null;
        }
        return DailyAggregate.builder()
                .id(document.getId())
                .date(document.getDate())
                .productType(document.getProductType())
                .customerType(document.getCustomerType())
                .movementCount(document.getMovementCount())
                .totalAmount(document.getTotalAmount())
                .avgAmount(document.getAvgAmount())
                .uniqueCustomers(document.getUniqueCustomers())
                .uniqueProducts(document.getUniqueProducts())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * Convierte modelo de dominio a documento de infraestructura.
     *
     * @param domain modelo de dominio (puede ser null)
     * @return documento MongoDB o null si el modelo es null
     */
    public static DailyAggregateDocument toDocument(DailyAggregate domain) {
        if (domain == null) {
            return null;
        }
        return DailyAggregateDocument.builder()
                .id(domain.getId())
                .date(domain.getDate())
                .productType(domain.getProductType())
                .customerType(domain.getCustomerType())
                .movementCount(domain.getMovementCount())
                .totalAmount(domain.getTotalAmount())
                .avgAmount(domain.getAvgAmount())
                .uniqueCustomers(domain.getUniqueCustomers())
                .uniqueProducts(domain.getUniqueProducts())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
