package com.yanki.reports.infrastructure.persistence.mapper;

import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.infrastructure.persistence.document.CustomerSummaryDocument;

/**
 * Mapper estático para conversión bidireccional entre modelo de dominio e infraestructura.
 *
 * <p>Separa completamente el modelo puro de dominio ({@link CustomerSummary}) del documento
 * MongoDB ({@link CustomerSummaryDocument}) anotado con Spring Data.
 */
public final class CustomerSummaryDocumentMapper {

    private CustomerSummaryDocumentMapper() {
        // Utility class
    }

    /**
     * Convierte documento de infraestructura a modelo de dominio.
     *
     * @param document documento MongoDB (puede ser null)
     * @return modelo de dominio o null si el documento es null
     */
    public static CustomerSummary toDomain(CustomerSummaryDocument document) {
        if (document == null) {
            return null;
        }
        return CustomerSummary.builder()
                .customerId(document.getCustomerId())
                .customerType(document.getCustomerType())
                .profile(document.getProfile())
                .status(document.getStatus())
                .totalAccounts(document.getTotalAccounts())
                .totalCards(document.getTotalCards())
                .totalCredits(document.getTotalCredits())
                .totalWallets(document.getTotalWallets())
                .totalBalanceAccounts(document.getTotalBalanceAccounts())
                .totalCreditLimit(document.getTotalCreditLimit())
                .totalCreditDebt(document.getTotalCreditDebt())
                .totalWalletBalance(document.getTotalWalletBalance())
                .hasOverdueDebt(document.getHasOverdueDebt())
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
    public static CustomerSummaryDocument toDocument(CustomerSummary domain) {
        if (domain == null) {
            return null;
        }
        return CustomerSummaryDocument.builder()
                .customerId(domain.getCustomerId())
                .customerType(domain.getCustomerType())
                .profile(domain.getProfile())
                .status(domain.getStatus())
                .totalAccounts(domain.getTotalAccounts())
                .totalCards(domain.getTotalCards())
                .totalCredits(domain.getTotalCredits())
                .totalWallets(domain.getTotalWallets())
                .totalBalanceAccounts(domain.getTotalBalanceAccounts())
                .totalCreditLimit(domain.getTotalCreditLimit())
                .totalCreditDebt(domain.getTotalCreditDebt())
                .totalWalletBalance(domain.getTotalWalletBalance())
                .hasOverdueDebt(domain.getHasOverdueDebt())
                .lastMovementAt(domain.getLastMovementAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
