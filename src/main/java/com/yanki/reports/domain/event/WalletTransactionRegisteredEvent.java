package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Transacción de wallet registrada (P2P, top-up, retiro).
 *
 * <p>Topic Kafka: {@code yanki.wallet.transaction.registered.v1}
 * <p>Proyección en {@code product_events}: {@code WALLET_TRANSACTION}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionRegisteredEvent {

    private String eventId;
    private String walletId;
    private String customerId;
    private String transactionType; // TOPUP, P2P_SEND, P2P_RECEIVE, WITHDRAWAL
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String counterpartyWalletId; // Para P2P
    private String currency;
    private LocalDateTime occurredAt;
}