package com.yanki.reports;

import com.yanki.reports.domain.model.AggregatedReport;
import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Fixtures compartidos para las pruebas unitarias del reports-service.
 */
public class MockFixtures {

    /**
     * Crea un evento de producto de ejemplo para pruebas.
     */
    public static ProductEvent createProductEvent() {
        return ProductEvent.builder()
                .id("event-001")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("prod-123")
                .customerId("cust-456")
                .productType("ACCOUNT")
                .subType("PERSONAL")
                .amount(BigDecimal.valueOf(1000))
                .balanceAfter(BigDecimal.valueOf(1000))
                .metadata(Collections.emptyMap())
                .occurredAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .ingestedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un reporte agregado de ejemplo para pruebas.
     */
    public static AggregatedReport createAggregatedReport() {
        return AggregatedReport.builder()
                .count(1)
                .sumAmount(BigDecimal.valueOf(1000))
                .averageAmount(BigDecimal.valueOf(1000))
                .type("ACCOUNT")
                .customerType("PERSONAL")
                .from(LocalDate.of(2026, 1, 1))
                .to(LocalDate.of(2026, 12, 31))
                .build();
    }

    /**
     * Crea un resumen de cliente de ejemplo para pruebas.
     */
    public static CustomerSummary createCustomerSummary() {
        return CustomerSummary.builder()
                .customerId("6af39f59b089e973a5ffe9b6")
                .customerType("NATURAL")
                .profile("PERSONAL")
                .status("ACTIVE")
                .totalAccounts(3)
                .totalCards(2)
                .totalCredits(1)
                .totalWallets(1)
                .totalBalanceAccounts(BigDecimal.valueOf(15000.50))
                .totalCreditLimit(BigDecimal.valueOf(50000.00))
                .totalCreditDebt(BigDecimal.valueOf(12000.00))
                .totalWalletBalance(BigDecimal.valueOf(500.00))
                .hasOverdueDebt(false)
                .lastMovementAt(LocalDateTime.of(2026, 6, 15, 10, 30))
                .updatedAt(LocalDateTime.of(2026, 6, 15, 10, 30, 5))
                .build();
    }

    /**
     * Crea un resumen de producto de ejemplo para pruebas.
     */
    public static ProductSummary createProductSummary() {
        return ProductSummary.builder()
                .productId("ACC-12345")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .customerId("6af39f59b089e973a5ffe9b6")
                .status("ACTIVE")
                .currentBalance(BigDecimal.valueOf(5000.00))
                .creditLimit(null)
                .availableLimit(null)
                .outstandingBalance(null)
                .currency("PEN")
                .openedAt(LocalDateTime.of(2024, 1, 15, 9, 0))
                .closedAt(null)
                .lastMovementAt(LocalDateTime.of(2026, 6, 15, 14, 30))
                .updatedAt(LocalDateTime.of(2026, 6, 15, 14, 30, 5))
                .build();
    }

    /**
     * Crea un agregado diario de ejemplo para pruebas.
     */
    public static DailyAggregate createDailyAggregate() {
        return DailyAggregate.builder()
                .id("2026-06-15:ACCOUNT:NATURAL")
                .date(LocalDate.of(2026, 6, 15))
                .productType("ACCOUNT")
                .customerType("NATURAL")
                .movementCount(120)
                .totalAmount(BigDecimal.valueOf(24000.50))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(45)
                .uniqueProducts(30)
                .updatedAt(LocalDateTime.of(2026, 6, 15, 23, 59))
                .build();
    }
}
