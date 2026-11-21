package com.yanki.reports.domain.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Cliente actualizado.
 *
 * <p>Topic Kafka: {@code yanki.customer.updated.v1}
 * <p>Proyección en {@code product_events}: {@code CUSTOMER_UPDATED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdatedEvent {

    private String eventId;
    private String customerId;
    private String documentType;
    private String documentNumber;
    private String customerType;
    private String profile;
    private String email;
    private String phone;
    private LocalDateTime occurredAt;
}
