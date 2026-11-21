package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Consumo de tarjeta registrado.
 *
 * <p>Topic Kafka: {@code yanki.card.consumption.registered.v1}
 * <p>Proyección en {@code product_events}: {@code CARD_CONSUMPTION}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardConsumptionRegisteredEvent {

    private String eventId;
    private String cardId;
    private String customerId;
    private String cardType;        // CREDIT, DEBIT
    private String merchantCategory;
    private BigDecimal amount;
    private BigDecimal availableLimitAfter;
    private String currency;
    private LocalDateTime occurredAt;
}