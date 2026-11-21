package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.CustomerSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para el resumen 360° del cliente.
 *
 * <p>Contiene totales consolidados de todos los productos bancarios de un cliente
 * y su estado financiero general.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resumen 360° consolidado del cliente con totales de productos y saldos")
public class CustomerSummaryResponse {

    @Schema(description = "Identificador único del cliente", example = "6af39f59b089e973a5ffe9b6", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Schema(description = "Tipo de cliente", example = "NATURAL", allowableValues = {"NATURAL", "JURIDICA"})
    private String customerType;

    @Schema(description = "Perfil del cliente según segmentación bancaria", example = "PERSONAL", allowableValues = {"PERSONAL", "VIP", "PYME", "EMPRESARIAL"})
    private String profile;

    @Schema(description = "Estado actual del cliente", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "BLOCKED"})
    private String status;

    @Schema(description = "Total de cuentas bancarias activas", example = "3")
    private Integer totalAccounts;

    @Schema(description = "Total de tarjetas activas (débito y crédito)", example = "2")
    private Integer totalCards;

    @Schema(description = "Total de créditos activos", example = "1")
    private Integer totalCredits;

    @Schema(description = "Total de wallets digitales activas", example = "1")
    private Integer totalWallets;

    @Schema(description = "Saldo total acumulado en todas las cuentas bancarias (en moneda local)", example = "15000.50")
    private BigDecimal totalBalanceAccounts;

    @Schema(description = "Límite total de crédito disponible entre todos los créditos", example = "50000.00")
    private BigDecimal totalCreditLimit;

    @Schema(description = "Deuda total acumulada en todos los créditos", example = "12000.00")
    private BigDecimal totalCreditDebt;

    @Schema(description = "Saldo total acumulado en todas las wallets digitales", example = "500.00")
    private BigDecimal totalWalletBalance;

    @Schema(description = "Indica si el cliente tiene deudas vencidas en algún crédito", example = "false")
    private Boolean hasOverdueDebt;

    @Schema(description = "Timestamp del último movimiento registrado para este cliente (ISO-8601)", example = "2024-06-15T10:30:00", format = "date-time")
    private LocalDateTime lastMovementAt;

    @Schema(description = "Timestamp de última actualización del resumen (ISO-8601)", example = "2024-06-15T10:30:05", format = "date-time")
    private LocalDateTime updatedAt;

    public static CustomerSummaryResponse fromDomain(CustomerSummary summary) {
        return CustomerSummaryResponse.builder()
                .customerId(summary.getCustomerId())
                .customerType(summary.getCustomerType())
                .profile(summary.getProfile())
                .status(summary.getStatus())
                .totalAccounts(summary.getTotalAccounts())
                .totalCards(summary.getTotalCards())
                .totalCredits(summary.getTotalCredits())
                .totalWallets(summary.getTotalWallets())
                .totalBalanceAccounts(summary.getTotalBalanceAccounts())
                .totalCreditLimit(summary.getTotalCreditLimit())
                .totalCreditDebt(summary.getTotalCreditDebt())
                .totalWalletBalance(summary.getTotalWalletBalance())
                .hasOverdueDebt(summary.getHasOverdueDebt())
                .lastMovementAt(summary.getLastMovementAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }
}
