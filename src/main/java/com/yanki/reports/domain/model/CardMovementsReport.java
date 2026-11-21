package com.yanki.reports.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de dominio para reporte de movimientos de tarjeta.
 *
 * <p>Incluye el subtipo de tarjeta (CREDIT/DEBIT) y la lista de movimientos
 * ordenados por occurredAt descendente (más reciente primero).
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CardMovementsReport {

    private String cardId;
    private String productType; // CARD
    private String subType;     // CREDIT, DEBIT
    private List<ProductEvent> movements;
}