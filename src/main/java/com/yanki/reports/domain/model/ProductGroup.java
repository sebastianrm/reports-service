package com.yanki.reports.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de dominio para grupo de producto en reporte de cliente.
 *
 * <p>Agrupa eventos por aggregateId + productType + subType.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductGroup {

    private String aggregateId;
    private String productType;
    private String subType;
    private List<ProductEvent> events;
}