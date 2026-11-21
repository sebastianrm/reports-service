package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Movimiento de cuenta registrado (depósito, retiro, transferencia).
 *
 * <p>Topic Kafka: {@code yanki.account.movement.registered.v1}
 * <p>Proyección en {@code product_events}: {@code ACCOUNT_MOVEMENT}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountMovementRegisteredEvent {

    private String eventId;
    private String accountId;
    private String customerId;
    private String movementType;    // DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
    private String channel;         // BRANCH, ATM, MOBILE, WEB, API
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private BigDecimal commissionApplied;
    private String commissionType;  // EXCESS_TRANSACTIONS, MAINTENANCE, VIP_PENALTY
    private LocalDateTime occurredAt;
}