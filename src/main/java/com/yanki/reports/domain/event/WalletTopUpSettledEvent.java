package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Recarga wallet liquidada.
 *
 * <p>Topic Kafka: {@code yanki.wallet.topup.settled.v1}
 * <p>Proyección en {@code product_events}: {@code WALLET_TOPUP_SETTLED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopUpSettledEvent {

    private String eventId;
    private String topUpId;
    private String walletId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private LocalDateTime occurredAt;
}
