package com.yanki.reports.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB para la colección {@code daily_aggregates}.
 *
 * <p>Esta clase es parte de la capa de INFRAESTRUCTURA y contiene anotaciones de Spring Data.
 * El modelo de dominio puro ({@link com.yanki.reports.domain.model.DailyAggregate}) NO tiene anotaciones.
 * La conversión se realiza en {@link com.yanki.reports.infrastructure.persistence.mapper.DailyAggregateDocumentMapper}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "daily_aggregates")
@CompoundIndexes({
        @CompoundIndex(name = "idx_date_productType", def = "{'date': 1, 'productType': 1}"),
        @CompoundIndex(name = "idx_date_customerType", def = "{'date': 1, 'customerType': 1}")
})
public class DailyAggregateDocument {

    @Id
    private String id;

    private LocalDate date;

    private String productType;

    private String customerType;

    private Integer movementCount;

    private BigDecimal totalAmount;

    private BigDecimal avgAmount;

    private Integer uniqueCustomers;

    private Integer uniqueProducts;

    private LocalDateTime updatedAt;
}
