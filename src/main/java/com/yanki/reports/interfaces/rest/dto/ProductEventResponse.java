package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.ProductEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta para evento de producto proyectado.
 *
 * <p>Representa un evento proyectado desde Kafka a MongoDB, utilizado en las respuestas
 * de los endpoints de reportes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evento de producto proyectado desde Kafka a MongoDB")
public class ProductEventResponse {

    @Schema(description = "Identificador único del evento (UUID)", example = "evt-001")
    private String id;

    @Schema(description = "Tipo de evento", example = "ACCOUNT_OPENED", allowableValues = {
            "CUSTOMER_REGISTERED", "CUSTOMER_UPDATED", "CUSTOMER_DELETED",
            "ACCOUNT_OPENED", "ACCOUNT_CLOSED", "ACCOUNT_MOVEMENT",
            "CARD_OPENED", "CARD_CLOSED", "CARD_CONSUMPTION",
            "DEBIT_PAYMENT_SETTLED",
            "TRANSFER_REQUESTED", "TRANSFER_SETTLED",
            "CREDIT_OPENED", "CREDIT_PAYMENT", "CREDIT_OVERDUE",
            "WALLET_OPENED", "WALLET_TRANSACTION", "WALLET_TOPUP_REQUESTED", "WALLET_TOPUP_SETTLED"
    })
    private String eventType;

    @Schema(description = "ID del agregado (accountId, cardId, creditId, walletId, customerId)", example = "ACC-123")
    private String aggregateId;

    @Schema(description = "ID del cliente asociado", example = "CUST-456")
    private String customerId;

    @Schema(description = "Tipo de producto", example = "ACCOUNT", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET", "CUSTOMER"})
    private String productType;

    @Schema(description = "Subtipo de producto", example = "SAVINGS", allowableValues = {"SAVINGS", "CURRENT", "FIXED_TERM", "CREDIT", "DEBIT", "PERSONAL_LOAN"})
    private String subType;

    @Schema(description = "Monto del evento (si aplica)", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Saldo posterior al evento (si aplica)", example = "1000.00")
    private BigDecimal balanceAfter;

    @Schema(description = "Campos adicionales del payload original del evento Kafka (extensible, varía por tipo de evento)")
    private Map<String, Object> metadata;

    @Schema(description = "Timestamp del evento original (del payload de Kafka, ISO-8601)", example = "2026-06-15T10:00:00", format = "date-time")
    private LocalDateTime occurredAt;

    @Schema(description = "Timestamp de ingesta en el servicio de reportes (ISO-8601)", example = "2026-06-15T10:00:05", format = "date-time")
    private LocalDateTime ingestedAt;

    /**
     * Convierte modelo de dominio a DTO de respuesta.
     */
    public static ProductEventResponse fromDomain(ProductEvent domain) {
        return ProductEventResponse.builder()
                .id(domain.getId())
                .eventType(domain.getEventType())
                .aggregateId(domain.getAggregateId())
                .customerId(domain.getCustomerId())
                .productType(domain.getProductType())
                .subType(domain.getSubType())
                .amount(domain.getAmount())
                .balanceAfter(domain.getBalanceAfter())
                .metadata(domain.getMetadata())
                .occurredAt(domain.getOccurredAt())
                .ingestedAt(domain.getIngestedAt())
                .build();
    }
}