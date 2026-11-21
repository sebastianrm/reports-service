package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Pago débito liquidado.
 *
 * <p>Topic Kafka: {@code yanki.debit.payment.settled.v1}
 * <p>Proyección en {@code product_events}: {@code DEBIT_PAYMENT_SETTLED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitPaymentSettledEvent {

    private String eventId;
    private String paymentId;
    private String accountId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private LocalDateTime occurredAt;
}
