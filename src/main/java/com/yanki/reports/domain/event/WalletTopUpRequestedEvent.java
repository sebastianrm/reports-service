package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Recarga wallet solicitada.
 *
 * <p>Topic Kafka: {@code yanki.wallet.topup.requested.v1}
 * <p>Proyección en {@code product_events}: {@code WALLET_TOPUP_REQUESTED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopUpRequestedEvent {

    private String eventId;
    private String topUpId;
    private String walletId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String sourceAccountId;
    private LocalDateTime occurredAt;
}
