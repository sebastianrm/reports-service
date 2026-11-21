package com.yanki.reports.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de dominio: Wallet abierta.
 *
 * <p>Topic Kafka: {@code yanki.wallet.opened.v1}
 * <p>Proyección en {@code product_events}: {@code WALLET_OPENED}
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletOpenedEvent {

    private String eventId;
    private String walletId;
    private String customerId;
    private BigDecimal initialBalance;
    private String currency;
    private LocalDateTime occurredAt;
}
