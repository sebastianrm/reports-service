package com.yanki.reports.infrastructure.persistence.mapper;

import com.yanki.reports.domain.model.ProcessedEvent;
import com.yanki.reports.infrastructure.persistence.document.ProcessedEventDocument;

/**
 * Mapper estático para conversión entre ProcessedEvent de dominio y documento MongoDB.
 */
public final class ProcessedEventDocumentMapper {

    private ProcessedEventDocumentMapper() {
        // Utility class
    }

    /**
     * Convierte documento a modelo de dominio.
     */
    public static ProcessedEvent toDomain(ProcessedEventDocument document) {
        if (document == null) {
            return null;
        }
        return ProcessedEvent.builder()
                .id(document.getId())
                .processedAt(document.getProcessedAt())
                .build();
    }

    /**
     * Convierte modelo de dominio a documento.
     */
    public static ProcessedEventDocument toDocument(ProcessedEvent domain) {
        if (domain == null) {
            return null;
        }
        return ProcessedEventDocument.builder()
                .id(domain.getId())
                .processedAt(domain.getProcessedAt())
                .build();
    }
}