package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Transferencia solicitada.
 *
 * <p>Topic Kafka: {@code yanki.transfer.requested.v1}
 * <p>Proyección en {@code product_events}: {@code TRANSFER_REQUESTED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestedEvent {

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
