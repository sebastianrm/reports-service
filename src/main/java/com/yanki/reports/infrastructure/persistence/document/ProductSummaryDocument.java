package com.yanki.reports.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB para la colección {@code product_summary}.
 *
 * <p>Esta clase es parte de la capa de INFRAESTRUCTURA y contiene anotaciones de Spring Data.
 * El modelo de dominio puro ({@link com.yanki.reports.domain.model.ProductSummary}) NO tiene anotaciones.
 * La conversión se realiza en {@link com.yanki.reports.infrastructure.persistence.mapper.ProductSummaryDocumentMapper}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_summary")
@CompoundIndexes({
        @CompoundIndex(name = "idx_customerId_productType", def = "{'customerId': 1, 'productType': 1}")
})
public class ProductSummaryDocument {

    @Id
    private String productId;

    @Indexed
    private String productType;

    private String subType;

    @Indexed
    private String customerId;

    private String status;

    private BigDecimal currentBalance;

    private BigDecimal creditLimit;

    private BigDecimal availableLimit;

    private BigDecimal outstandingBalance;

    private String currency;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private LocalDateTime lastMovementAt;

    private LocalDateTime updatedAt;
}
