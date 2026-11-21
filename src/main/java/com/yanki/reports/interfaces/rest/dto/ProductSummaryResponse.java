package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.ProductSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para el resumen de un producto bancario.
 *
 * <p>Contiene el estado actual, saldos, límites y timestamps de un producto
 * específico (cuenta, tarjeta, crédito o wallet).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resumen del estado actual de un producto bancario")
public class ProductSummaryResponse {

    @Schema(description = "Identificador único del producto", example = "ACC-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productId;

    @Schema(description = "Tipo de producto bancario", example = "ACCOUNT", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET"})
    private String productType;

    @Schema(description = "Subtipo del producto según categoría", example = "SAVINGS", allowableValues = {"SAVINGS", "CURRENT", "FIXED_TERM", "CREDIT", "DEBIT", "PERSONAL_LOAN", "WALLET"})
    private String subType;

    @Schema(description = "Identificador del cliente propietario del producto", example = "6af39f59b089e973a5ffe9b6")
    private String customerId;

    @Schema(description = "Estado actual del producto", example = "ACTIVE", allowableValues = {"OPEN", "ACTIVE", "CLOSED", "BLOCKED", "OVERDUE"})
    private String status;

    @Schema(description = "Saldo actual del producto (aplica para cuentas y wallets)", example = "5000.00")
    private BigDecimal currentBalance;

    @Schema(description = "Límite total de crédito (aplica para tarjetas de crédito)", example = "10000.00")
    private BigDecimal creditLimit;

    @Schema(description = "Límite disponible para uso (aplica para tarjetas de crédito)", example = "7500.00")
    private BigDecimal availableLimit;

    @Schema(description = "Saldo pendiente de pago (aplica para créditos)", example = "8000.00")
    private BigDecimal outstandingBalance;

    @Schema(description = "Moneda del producto", example = "PEN", allowableValues = {"PEN", "USD"})
    private String currency;

    @Schema(description = "Timestamp de apertura del producto (ISO-8601)", example = "2024-01-15T09:00:00", format = "date-time")
    private LocalDateTime openedAt;

    @Schema(description = "Timestamp de cierre del producto si está cerrado (ISO-8601, null si está activo)", example = "null", format = "date-time")
    private LocalDateTime closedAt;

    @Schema(description = "Timestamp del último movimiento del producto (ISO-8601)", example = "2024-06-15T14:30:00", format = "date-time")
    private LocalDateTime lastMovementAt;

    @Schema(description = "Timestamp de última actualización del resumen (ISO-8601)", example = "2024-06-15T14:30:05", format = "date-time")
    private LocalDateTime updatedAt;

    public static ProductSummaryResponse fromDomain(ProductSummary summary) {
        return ProductSummaryResponse.builder()
                .productId(summary.getProductId())
                .productType(summary.getProductType())
                .subType(summary.getSubType())
                .customerId(summary.getCustomerId())
                .status(summary.getStatus())
                .currentBalance(summary.getCurrentBalance())
                .creditLimit(summary.getCreditLimit())
                .availableLimit(summary.getAvailableLimit())
                .outstandingBalance(summary.getOutstandingBalance())
                .currency(summary.getCurrency())
                .openedAt(summary.getOpenedAt())
                .closedAt(summary.getClosedAt())
                .lastMovementAt(summary.getLastMovementAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }
}
