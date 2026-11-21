package com.yanki.reports.infrastructure.persistence.mapper;

import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.infrastructure.persistence.document.ProductSummaryDocument;

/**
 * Mapper estático para conversión bidireccional entre modelo de dominio e infraestructura.
 *
 * <p>Separa completamente el modelo puro de dominio ({@link ProductSummary}) del documento
 * MongoDB ({@link ProductSummaryDocument}) anotado con Spring Data.
 */
public final class ProductSummaryDocumentMapper {

    private ProductSummaryDocumentMapper() {
        // Utility class
    }

    /**
     * Convierte documento de infraestructura a modelo de dominio.
     *
     * @param document documento MongoDB (puede ser null)
     * @return modelo de dominio o null si el documento es null
     */
    public static ProductSummary toDomain(ProductSummaryDocument document) {
        if (document == null) {
            return null;
        }
        return ProductSummary.builder()
                .productId(document.getProductId())
                .productType(document.getProductType())
                .subType(document.getSubType())
                .customerId(document.getCustomerId())
                .status(document.getStatus())
                .currentBalance(document.getCurrentBalance())
                .creditLimit(document.getCreditLimit())
                .availableLimit(document.getAvailableLimit())
                .outstandingBalance(document.getOutstandingBalance())
                .currency(document.getCurrency())
                .openedAt(document.getOpenedAt())
                .closedAt(document.getClosedAt())
                .lastMovementAt(document.getLastMovementAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * Convierte modelo de dominio a documento de infraestructura.
     *
     * @param domain modelo de dominio (puede ser null)
     * @return documento MongoDB o null si el modelo es null
     */
    public static ProductSummaryDocument toDocument(ProductSummary domain) {
        if (domain == null) {
            return null;
        }
        return ProductSummaryDocument.builder()
                .productId(domain.getProductId())
                .productType(domain.getProductType())
                .subType(domain.getSubType())
                .customerId(domain.getCustomerId())
                .status(domain.getStatus())
                .currentBalance(domain.getCurrentBalance())
                .creditLimit(domain.getCreditLimit())
                .availableLimit(domain.getAvailableLimit())
                .outstandingBalance(domain.getOutstandingBalance())
                .currency(domain.getCurrency())
                .openedAt(domain.getOpenedAt())
                .closedAt(domain.getClosedAt())
                .lastMovementAt(domain.getLastMovementAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
