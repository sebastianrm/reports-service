package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Crédito abierto.
 *
 * <p>Topic Kafka: {@code yanki.credit.opened.v1}
 * <p>Proyección en {@code product_events}: {@code CREDIT_OPENED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditOpenedEvent {

    private String eventId;
    private String creditId;
    private String customerId;
    private String creditType;       // PERSONAL_LOAN, MORTGAGE, VEHICLE
    private BigDecimal totalAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private String currency;
    private LocalDateTime occurredAt;
}
