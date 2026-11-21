package com.yanki.reports.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad pura de dominio que representa el resumen 360° de un cliente.
 *
 * <p>Se materializa a partir de eventos Kafka (customer.registered, account.opened, card.opened,
 * credit.opened, wallet.opened, credit.overdue, etc.) y se almacena en la colección {@code customer_summary}.
 *
 * <p>Permite consultas rápidas de vista 360° sin necesidad de llamar a otros microservicios.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummary {

    /**
     * Identificador único del cliente (clave primaria).
     */
    private String customerId;

    /**
     * Tipo de cliente (NATURAL, JURIDICA).
     */
    private String customerType;

    /**
     * Perfil del cliente (PERSONAL, VIP, PYME, EMPRESARIAL).
     */
    private String profile;

    /**
     * Estado del cliente (ACTIVE, INACTIVE, BLOCKED).
     */
    private String status;

    /**
     * Total de cuentas bancarias activas.
     */
    private Integer totalAccounts;

    /**
     * Total de tarjetas activas.
     */
    private Integer totalCards;

    /**
     * Total de créditos activos.
     */
    private Integer totalCredits;

    /**
     * Total de wallets activas.
     */
    private Integer totalWallets;

    /**
     * Saldo total acumulado en todas las cuentas bancarias.
     */
    private BigDecimal totalBalanceAccounts;

    /**
     * Límite total de crédito disponible.
     */
    private BigDecimal totalCreditLimit;

    /**
     * Deuda total acumulada en créditos.
     */
    private BigDecimal totalCreditDebt;

    /**
     * Saldo total acumulado en wallets.
     */
    private BigDecimal totalWalletBalance;

    /**
     * Indica si el cliente tiene deudas vencidas.
     */
    private Boolean hasOverdueDebt;

    /**
     * Timestamp del último movimiento registrado para este cliente.
     */
    private LocalDateTime lastMovementAt;

    /**
     * Timestamp de última actualización del resumen.
     */
    private LocalDateTime updatedAt;
}
