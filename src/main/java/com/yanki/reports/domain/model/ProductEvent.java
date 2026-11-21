package com.yanki.reports.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad pura de dominio que representa un evento de producto proyectado desde Kafka a MongoDB.
 *
 * <p>Cada evento se ingiere desde los topics de Kafka y se almacena en la colección {@code product_events}
 * para ser consultado por los endpoints de reportes. Los eventos son inmutables una vez ingeridos
 * y la idempotencia se garantiza mediante la clave {@code <topic>:<aggregateId>:<occurredAt>}.
 *
 * <p>Campos principales:
 * <ul>
 *   <li>{@code eventType}: tipo de evento (ej. CARD_CONSUMPTION, ACCOUNT_OPENED)</li>
 *   <li>{@code aggregateId}: identificador del agregado (accountId, cardId, creditId, walletId)</li>
 *   <li>{@code productType}: tipo de producto (ACCOUNT, CARD, CREDIT, WALLET, CUSTOMER)</li>
 *   <li>{@code occurredAt}: timestamp del evento original (del payload de Kafka)</li>
 *   <li>{@code ingestedAt}: timestamp de ingesta en el servicio de reportes</li>
 * </ul>
 *
 * <p>Índices MongoDB recomendados:
 * <ul>
 *   <li>{@code customerId + occurredAt} — consultas por cliente</li>
 *   <li>{@code aggregateId + occurredAt} — consultas por producto</li>
 *   <li>{@code eventType + occurredAt} — consultas por tipo de evento</li>
 *   <li>{@code productType + occurredAt} — consultas por tipo de producto</li>
 * </ul>
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {

    /**
     * Identificador único del documento en MongoDB (UUID).
     */
    private String id;

    /**
     * Tipo de evento (ej. CARD_CONSUMPTION, ACCOUNT_OPENED, CREDIT_PAYMENT).
     */
    private String eventType;

    /**
     * Identificador del agregado al que pertenece el evento (accountId, cardId, creditId, walletId, customerId).
     */
    private String aggregateId;

    /**
     * Identificador del cliente asociado al evento.
     */
    private String customerId;

    /**
     * Tipo de producto (ACCOUNT, CARD, CREDIT, WALLET, CUSTOMER).
     */
    private String productType;

    /**
     * Subtipo del producto (SAVINGS, CREDIT, PERSONAL_LOAN, etc.).
     */
    private String subType;

    /**
     * Monto del evento (si aplica). Puede ser nulo para eventos sin monto.
     */
    private BigDecimal amount;

    /**
     * Saldo posterior al evento (si aplica). Puede ser nulo para eventos sin saldo.
     */
    private BigDecimal balanceAfter;

    /**
     * Campos adicionales del payload original del evento de Kafka.
     */
    private Map<String, Object> metadata;

    /**
     * Timestamp del evento original (del payload de Kafka).
     */
    private LocalDateTime occurredAt;

    /**
     * Timestamp de ingesta en el servicio de reportes (momento de escritura en MongoDB).
     */
    private LocalDateTime ingestedAt;
}