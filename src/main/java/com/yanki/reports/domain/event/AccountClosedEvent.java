package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Cuenta cerrada.
 *
 * <p>Topic Kafka: {@code yanki.account.closed.v1}
 * <p>Proyección en {@code product_events}: {@code ACCOUNT_CLOSED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountClosedEvent {

    private String eventId;
    private String accountId;
    private String customerId;
    private String productType;
    private String subType;
    private BigDecimal finalBalance;
    private String currency;
    private String reason;
    private LocalDateTime occurredAt;
}
