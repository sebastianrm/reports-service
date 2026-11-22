package com.yanki.reports.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad pura de dominio que representa la vista 360° completa de un cliente.
 *
 * <p>Agrega datos de tres fuentes de proyección MongoDB materializadas por eventos Kafka:
 * <ul>
 *   <li>{@link CustomerSummary}: información general del cliente y totales consolidados</li>
 *   <li>{@link ProductSummary}: estado actual de cada producto (cuentas, tarjetas, créditos, wallets)</li>
 *   <li>{@link ProductEvent}: historial reciente de eventos del cliente</li>
 * </ul>
 *
 * <p>Permite una consulta única para obtener toda la información de un cliente
 * sin necesidad de llamar a otros microservicios.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer360 {

    /**
     * Identificador único del cliente.
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

    /**
     * Lista de productos (cuentas, tarjetas, créditos, wallets) del cliente con su estado actual.
     */
    private List<ProductSummary> products;

    /**
     * Lista de eventos recientes del cliente (ordenados por occurredAt descendente).
     */
    private List<ProductEvent> recentEvents;
}
