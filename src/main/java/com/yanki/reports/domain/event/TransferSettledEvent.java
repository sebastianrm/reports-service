package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Transferencia liquidada.
 *
 * <p>Topic Kafka: {@code yanki.transfer.settled.v1}
 * <p>Proyección en {@code product_events}: {@code TRANSFER_SETTLED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSettledEvent {

    private String eventId;
    private String transferId;
    private String sourceAccountId;
    private String destinationAccountId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String transferType;
    private LocalDateTime occurredAt;
}
