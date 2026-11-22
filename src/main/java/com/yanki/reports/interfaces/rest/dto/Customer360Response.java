package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.Customer360;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para la vista 360° completa del cliente.
 *
 * <p>Combina el resumen consolidado del cliente con la lista detallada de productos
 * y los eventos recientes, todo obtenido de proyecciones MongoDB materializadas por Kafka.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vista 360° completa del cliente: resumen, productos y eventos recientes")
public class Customer360Response {

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

    @Schema(description = "Lista de productos del cliente con su estado actual")
    private List<ProductSummaryResponse> products;

    @Schema(description = "Lista de eventos recientes del cliente (ordenados por fecha descendente)")
    private List<ProductEventResponse> recentEvents;

    public static Customer360Response fromDomain(Customer360 customer360) {
        return builder()
                .customerId(customer360.getCustomerId())
                .customerType(customer360.getCustomerType())
                .profile(customer360.getProfile())
                .status(customer360.getStatus())
                .totalAccounts(customer360.getTotalAccounts())
                .totalCards(customer360.getTotalCards())
                .totalCredits(customer360.getTotalCredits())
                .totalWallets(customer360.getTotalWallets())
                .totalBalanceAccounts(customer360.getTotalBalanceAccounts())
                .totalCreditLimit(customer360.getTotalCreditLimit())
                .totalCreditDebt(customer360.getTotalCreditDebt())
                .totalWalletBalance(customer360.getTotalWalletBalance())
                .hasOverdueDebt(customer360.getHasOverdueDebt())
                .lastMovementAt(customer360.getLastMovementAt())
                .updatedAt(customer360.getUpdatedAt())
                .products(customer360.getProducts() != null
                        ? customer360.getProducts().stream()
                                .map(ProductSummaryResponse::fromDomain)
                                .toList()
                        : null)
                .recentEvents(customer360.getRecentEvents() != null
                        ? customer360.getRecentEvents().stream()
                                .map(ProductEventResponse::fromDomain)
                                .toList()
                        : null)
                .build();
    }
}
