package com.yanki.reports.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB para la colección {@code customer_summary}.
 *
 * <p>Esta clase es parte de la capa de INFRAESTRUCTURA y contiene anotaciones de Spring Data.
 * El modelo de dominio puro ({@link com.yanki.reports.domain.model.CustomerSummary}) NO tiene anotaciones.
 * La conversión se realiza en {@link com.yanki.reports.infrastructure.persistence.mapper.CustomerSummaryDocumentMapper}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_summary")
public class CustomerSummaryDocument {

    @Id
    private String customerId;

    private String customerType;

    private String profile;

    private String status;

    private Integer totalAccounts;

    private Integer totalCards;

    private Integer totalCredits;

    private Integer totalWallets;

    private BigDecimal totalBalanceAccounts;

    private BigDecimal totalCreditLimit;

    private BigDecimal totalCreditDebt;

    private BigDecimal totalWalletBalance;

    private Boolean hasOverdueDebt;

    private LocalDateTime lastMovementAt;

    private LocalDateTime updatedAt;
}
