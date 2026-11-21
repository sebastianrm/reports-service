package com.yanki.reports.domain.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Cliente eliminado.
 *
 * <p>Topic Kafka: {@code yanki.customer.deleted.v1}
 * <p>Proyección en {@code product_events}: {@code CUSTOMER_DELETED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDeletedEvent {

    private String eventId;
    private String customerId;
    private String reason;
    private LocalDateTime occurredAt;
}
