package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Tarjeta abierta.
 *
 * <p>Topic Kafka: {@code yanki.card.opened.v1}
 * <p>Proyección en {@code product_events}: {@code CARD_OPENED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardOpenedEvent {

    private String eventId;
    private String cardId;
    private String customerId;
    private String cardType;       // DEBIT, CREDIT
    private String accountId;
    private BigDecimal initialLimit;
    private String currency;
    private LocalDateTime occurredAt;
}
