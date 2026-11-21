package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Pago de crédito registrado.
 *
 * <p>Topic Kafka: {@code yanki.credit.payment.registered.v1}
 * <p>Proyección en {@code product_events}: {@code CREDIT_PAYMENT}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPaymentRegisteredEvent {

    private String eventId;
    private String creditId;
    private String customerId;
    private String paymentType;     // OWN_ACCOUNT, THIRD_PARTY, CASH
    private BigDecimal amount;
    private BigDecimal capitalPortion;
    private BigDecimal interestPortion;
    private BigDecimal pendingBalanceAfter;
    private int installmentNumber;
    private LocalDateTime occurredAt;
}