package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Tarjeta cerrada.
 *
 * <p>Topic Kafka: {@code yanki.card.closed.v1}
 * <p>Proyección en {@code product_events}: {@code CARD_CLOSED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardClosedEvent {

    private String eventId;
    private String cardId;
    private String customerId;
    private String cardType;
    private BigDecimal finalBalance;
    private String reason;
    private LocalDateTime occurredAt;
}
