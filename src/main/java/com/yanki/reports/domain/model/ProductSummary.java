package com.yanki.reports.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad pura de dominio que representa el estado actual de un producto bancario.
 *
 * <p>Se materializa a partir de eventos Kafka (account.opened, account.closed, card.opened,
 * card.closed, credit.opened, wallet.opened, etc.) y se almacena en la colección {@code product_summary}.
 *
 * <p>Permite consultas rápidas del estado de un producto sin necesidad de llamar a otros microservicios.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummary {

    /**
     * Identificador único del producto (clave primaria).
     */
    private String productId;

    /**
     * Tipo de producto (ACCOUNT, CARD, CREDIT, WALLET).
     */
    private String productType;

    /**
     * Subtipo del producto (SAVINGS, CHECKING, FIXED_TERM, CREDIT_PERSONAL, DEBIT, WALLET).
     */
    private String subType;

    /**
     * Identificador del cliente propietario del producto.
     */
    private String customerId;

    /**
     * Estado del producto (OPEN, ACTIVE, CLOSED, BLOCKED, OVERDUE).
     */
    private String status;

    /**
     * Saldo actual (para cuentas y wallets).
     */
    private BigDecimal currentBalance;

    /**
     * Límite de crédito (para tarjetas de crédito).
     */
    private BigDecimal creditLimit;

    /**
     * Límite disponible (para tarjetas de crédito).
     */
    private BigDecimal availableLimit;

    /**
     * Saldo pendiente (para créditos).
     */
    private BigDecimal outstandingBalance;

    /**
     * Moneda del producto (PEN, USD, etc.).
     */
    private String currency;

    /**
     * Timestamp de apertura del producto.
     */
    private LocalDateTime openedAt;

    /**
     * Timestamp de cierre del producto (si aplica).
     */
    private LocalDateTime closedAt;

    /**
     * Timestamp del último movimiento del producto.
     */
    private LocalDateTime lastMovementAt;

    /**
     * Timestamp de última actualización del resumen.
     */
    private LocalDateTime updatedAt;
}
