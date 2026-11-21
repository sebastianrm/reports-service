package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Cuenta abierta.
 *
 * <p>Topic Kafka: {@code yanki.account.opened.v1}
 * <p>Proyección en {@code product_events}: {@code ACCOUNT_OPENED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOpenedEvent {

    private String eventId;
    private String accountId;
    private String customerId;
    private String productType;    // ACCOUNT
    private String subType;        // SAVINGS, CURRENT, FIXED_TERM
    private BigDecimal initialBalance;
    private String currency;
    private LocalDateTime occurredAt;
}