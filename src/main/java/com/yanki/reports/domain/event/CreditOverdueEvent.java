package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Crédito en mora.
 *
 * <p>Topic Kafka: {@code yanki.credit.overdue.v1}
 * <p>Proyección en {@code product_events}: {@code CREDIT_OVERDUE}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditOverdueEvent {

    private String eventId;
    private String creditId;
    private String customerId;
    private String creditType;
    private BigDecimal overdueAmount;
    private Integer daysOverdue;
    private Integer installmentNumber;
    private LocalDateTime occurredAt;
}
