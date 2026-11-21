package com.yanki.reports.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB para la colección {@code product_events}.
 *
 * <p>Esta clase es parte de la capa de INFRAESTRUCTURA y contiene anotaciones de Spring Data.
 * El modelo de dominio puro ({@link com.yanki.reports.domain.model.ProductEvent}) NO tiene anotaciones.
 * La conversión se realiza en {@link com.yanki.reports.infrastructure.persistence.mapper.ProductEventDocumentMapper}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_events")
public class ProductEventDocument {

    @Id
    private String id;

    @Indexed
    private String eventType;

    @Indexed
    private String aggregateId;

    @Indexed
    private String customerId;

    @Indexed
    private String productType;

    private String subType;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private Map<String, Object> metadata;

    @Indexed
    private LocalDateTime occurredAt;

    private LocalDateTime ingestedAt;
}