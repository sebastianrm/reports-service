package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.CardMovementsReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de respuesta para últimos movimientos de tarjeta.
 *
 * <p><b>Requerimiento obligatorio Parte II:</b> "Implementar un reporte con los últimos 10 movimientos
 * de la tarjeta de débito y de crédito".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Últimos movimientos de tarjeta (crédito o débito)")
public class CardMovementsResponse {

    @Schema(description = "Identificador de la tarjeta", example = "CARD-789")
    private String cardId;

    @Schema(description = "Tipo de producto", example = "CARD")
    private String productType;

    @Schema(description = "Subtipo de tarjeta", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
    private String subType;

    @Schema(description = "Lista de movimientos ordenados por fecha descendente (más reciente primero)")
    private List<ProductEventResponse> movements;

    /**
     * Convierte modelo de dominio a DTO de respuesta.
     */
    public static CardMovementsResponse fromDomain(CardMovementsReport domain) {
        return CardMovementsResponse.builder()
                .cardId(domain.getCardId())
                .productType(domain.getProductType())
                .subType(domain.getSubType())
                .movements(domain.getMovements().stream()
                        .map(ProductEventResponse::fromDomain)
                        .collect(Collectors.toList()))
                .build();
    }
}