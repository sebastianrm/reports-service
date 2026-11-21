package com.yanki.reports.domain.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Cliente registrado.
 *
 * <p>Topic Kafka: {@code yanki.customer.registered.v1}
 * <p>Proyección en {@code product_events}: {@code CUSTOMER_REGISTERED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisteredEvent {

    private String eventId;
    private String customerId;
    private String documentType;
    private String documentNumber;
    private String customerType; // PERSONAL, ENTERPRISE
    private String profile;      // STANDARD, VIP, PYME
    private String email;
    private String phone;
    private LocalDateTime occurredAt;
}