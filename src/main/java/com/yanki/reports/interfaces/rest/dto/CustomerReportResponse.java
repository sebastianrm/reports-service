package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.CustomerReport;
import com.yanki.reports.domain.model.ProductGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de respuesta para reporte completo de cliente (Vista 360°).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reporte completo de cliente con todos sus productos y eventos")
public class CustomerReportResponse {

    @Schema(description = "Identificador del cliente", example = "6af39f59b089e973a5ffe9b6")
    private String customerId;

    @Schema(description = "Lista de productos del cliente agrupados por aggregateId")
    private List<ProductGroupResponse> products;

    /**
     * Convierte modelo de dominio a DTO de respuesta.
     */
    public static CustomerReportResponse fromDomain(CustomerReport domain) {
        return CustomerReportResponse.builder()
                .customerId(domain.getCustomerId())
                .products(domain.getProducts().stream()
                        .map(ProductGroupResponse::fromDomain)
                        .collect(Collectors.toList()))
                .build();
    }
}