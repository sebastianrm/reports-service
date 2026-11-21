package com.yanki.reports.interfaces.rest.dto;

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
 * DTO para grupo de producto en reporte de cliente.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Grupo de producto con sus eventos asociados")
public class ProductGroupResponse {

    @Schema(description = "ID del producto (accountId, cardId, creditId, walletId)", example = "ACC-123")
    private String aggregateId;

    @Schema(description = "Tipo de producto", example = "ACCOUNT", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET"})
    private String productType;

    @Schema(description = "Subtipo de producto", example = "SAVINGS", allowableValues = {"SAVINGS", "CURRENT", "FIXED_TERM", "CREDIT", "DEBIT", "PERSONAL_LOAN"})
    private String subType;

    @Schema(description = "Eventos asociados al producto")
    private List<ProductEventResponse> events;

    /**
     * Convierte modelo de dominio a DTO de respuesta.
     */
    public static ProductGroupResponse fromDomain(ProductGroup domain) {
        return ProductGroupResponse.builder()
                .aggregateId(domain.getAggregateId())
                .productType(domain.getProductType())
                .subType(domain.getSubType())
                .events(domain.getEvents().stream()
                        .map(ProductEventResponse::fromDomain)
                        .collect(Collectors.toList()))
                .build();
    }
}