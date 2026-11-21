package com.yanki.reports.domain.service;

import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.in.IngestEventUseCase;
import com.yanki.reports.domain.port.out.CustomerSummaryPersistencePort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Implementación del caso de uso para ingesta de eventos.
 *
 * <p>Simplemente delega al puerto de persistencia. La idempotencia ya fue validada
 * por el consumidor Kafka antes de invocar este caso de uso.
 *
 * <p>Además, actualiza el resumen materializado del cliente (CustomerSummary)
 * para que los endpoints de resumen 360° devuelvan datos actualizados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestEventService implements IngestEventUseCase {

    private final ProductEventPersistencePort persistencePort;
    private final CustomerSummaryPersistencePort customerSummaryPort;

    @Override
    public Single<ProductEvent> execute(ProductEvent event) {
        log.info("Ingestando evento en proyección: eventType={}, aggregateId={}, customerId={}",
                event.getEventType(), event.getAggregateId(), event.getCustomerId());

        try {
            return persistencePort.save(event)
                    .flatMap(saved -> {
                        log.info("Evento persistido exitosamente en proyección: id={}, eventType={}, aggregateId={}",
                                saved.getId(), saved.getEventType(), saved.getAggregateId());
                        // Actualizar el resumen materializado del cliente
                        return updateCustomerSummary(event, saved)
                                .onErrorResumeNext(e -> {
                                    log.warn("Error actualizando CustomerSummary (no crítico): customerId={}, error={}",
                                            event.getCustomerId(), e.getMessage());
                                    return Single.just(saved);
                                });
                    })
                    .doOnError(e -> log.error("Error persistiendo evento en proyección: eventType={}, aggregateId={}, error={}",
                            event.getEventType(), event.getAggregateId(), e.getMessage(), e));
        } catch (Exception ex) {
            log.error("Error inesperado en execute() de ingesta de evento: eventType={}, aggregateId={}",
                    event.getEventType(), event.getAggregateId(), ex);
            throw ex;
        }
    }

    /**
     * Actualiza el resumen materializado del cliente tras un nuevo evento.
     *
     * <p>Lee el resumen existente (si existe) y lo actualiza incrementando los contadores
     * y saldos correspondientes según el tipo de producto y evento.
     *
     * @param event  el evento que fue persistido
     * @param saved  el evento persistido que se retorna al final
     * @return el evento persistido para mantener la cadena reactiva
     */
    private Single<ProductEvent> updateCustomerSummary(ProductEvent event, ProductEvent saved) {
        String customerId = event.getCustomerId();
        String productType = event.getProductType();
        String eventType = event.getEventType();

        if (customerId == null || productType == null) {
            log.debug("Evento sin customerId o productType - omitiendo actualización de CustomerSummary");
            return Single.just(saved);
        }

        return customerSummaryPort.findByCustomerId(customerId)
                .switchIfEmpty(Single.just(CustomerSummary.builder()
                        .customerId(customerId)
                        .totalAccounts(0)
                        .totalCards(0)
                        .totalCredits(0)
                        .totalWallets(0)
                        .totalBalanceAccounts(BigDecimal.ZERO)
                        .totalCreditLimit(BigDecimal.ZERO)
                        .totalCreditDebt(BigDecimal.ZERO)
                        .totalWalletBalance(BigDecimal.ZERO)
                        .hasOverdueDebt(false)
                        .status("ACTIVE")
                        .build()))
                .map(existing -> applyEventToSummary(existing, event))
                .flatMap(updated -> customerSummaryPort.save(updated)
                        .map(summary -> {
                            log.info("CustomerSummary actualizado: customerId={}, accounts={}, cards={}, credits={}, wallets={}",
                                    customerId, summary.getTotalAccounts(), summary.getTotalCards(),
                                    summary.getTotalCredits(), summary.getTotalWallets());
                            return saved;
                        }));
    }

    /**
     * Aplica un evento al resumen del cliente, actualizando contadores y saldos.
     *
     * @param summary el resumen actual del cliente
     * @param event   el evento a aplicar
     * @return el resumen actualizado
     */
    private CustomerSummary applyEventToSummary(CustomerSummary summary, ProductEvent event) {
        String productType = event.getProductType();
        String eventType = event.getEventType();
        LocalDateTime now = LocalDateTime.now();

        // Inicializar contadores si son null
        if (summary.getTotalAccounts() == null) summary.setTotalAccounts(0);
        if (summary.getTotalCards() == null) summary.setTotalCards(0);
        if (summary.getTotalCredits() == null) summary.setTotalCredits(0);
        if (summary.getTotalWallets() == null) summary.setTotalWallets(0);
        if (summary.getTotalBalanceAccounts() == null) summary.setTotalBalanceAccounts(BigDecimal.ZERO);
        if (summary.getTotalCreditLimit() == null) summary.setTotalCreditLimit(BigDecimal.ZERO);
        if (summary.getTotalCreditDebt() == null) summary.setTotalCreditDebt(BigDecimal.ZERO);
        if (summary.getTotalWalletBalance() == null) summary.setTotalWalletBalance(BigDecimal.ZERO);
        if (summary.getHasOverdueDebt() == null) summary.setHasOverdueDebt(false);

        switch (productType) {
            case "CUSTOMER":
                // Eventos de cliente: inicializar o actualizar estado
                if ("CUSTOMER_REGISTERED".equals(eventType)) {
                    summary.setStatus("ACTIVE");
                } else if ("CUSTOMER_DELETED".equals(eventType)) {
                    summary.setStatus("INACTIVE");
                }
                break;

            case "ACCOUNT":
                if (isOpenedEvent(eventType)) {
                    summary.setTotalAccounts(summary.getTotalAccounts() + 1);
                    if (event.getBalanceAfter() != null) {
                        summary.setTotalBalanceAccounts(
                                summary.getTotalBalanceAccounts().add(event.getBalanceAfter()));
                    }
                } else if (isClosedEvent(eventType)) {
                    summary.setTotalAccounts(Math.max(0, summary.getTotalAccounts() - 1));
                    if (event.getBalanceAfter() != null) {
                        summary.setTotalBalanceAccounts(
                                summary.getTotalBalanceAccounts().subtract(event.getBalanceAfter()));
                    }
                } else if ("ACCOUNT_MOVEMENT".equals(eventType) || "DEBIT_PAYMENT_SETTLED".equals(eventType)) {
                    // Los movimientos ya reflejan el saldo en balanceAfter
                    if (event.getBalanceAfter() != null && event.getAmount() != null) {
                        // Ajustar el saldo total según el monto del movimiento
                        summary.setTotalBalanceAccounts(
                                summary.getTotalBalanceAccounts().add(event.getAmount()));
                    }
                } else if ("TRANSFER_REQUESTED".equals(eventType) || "TRANSFER_SETTLED".equals(eventType)) {
                    if (event.getAmount() != null) {
                        // Las transferencias restan del saldo
                        summary.setTotalBalanceAccounts(
                                summary.getTotalBalanceAccounts().subtract(event.getAmount()));
                    }
                }
                break;

            case "CARD":
                if (isOpenedEvent(eventType)) {
                    summary.setTotalCards(summary.getTotalCards() + 1);
                    if (event.getBalanceAfter() != null) {
                        summary.setTotalCreditLimit(
                                summary.getTotalCreditLimit().add(event.getBalanceAfter()));
                    }
                } else if (isClosedEvent(eventType)) {
                    summary.setTotalCards(Math.max(0, summary.getTotalCards() - 1));
                } else if ("CARD_CONSUMPTION".equals(eventType)) {
                    if (event.getAmount() != null) {
                        summary.setTotalCreditDebt(
                                summary.getTotalCreditDebt().add(event.getAmount()));
                    }
                }
                break;

            case "CREDIT":
                if (isOpenedEvent(eventType)) {
                    summary.setTotalCredits(summary.getTotalCredits() + 1);
                    if (event.getAmount() != null) {
                        summary.setTotalCreditDebt(
                                summary.getTotalCreditDebt().add(event.getAmount()));
                        summary.setTotalCreditLimit(
                                summary.getTotalCreditLimit().add(event.getAmount()));
                    }
                } else if ("CREDIT_PAYMENT".equals(eventType)) {
                    if (event.getAmount() != null) {
                        summary.setTotalCreditDebt(
                                summary.getTotalCreditDebt().subtract(event.getAmount()));
                    }
                } else if ("CREDIT_OVERDUE".equals(eventType)) {
                    summary.setHasOverdueDebt(true);
                }
                break;

            case "WALLET":
                if (isOpenedEvent(eventType)) {
                    summary.setTotalWallets(summary.getTotalWallets() + 1);
                    if (event.getBalanceAfter() != null) {
                        summary.setTotalWalletBalance(
                                summary.getTotalWalletBalance().add(event.getBalanceAfter()));
                    }
                } else if ("WALLET_TRANSACTION".equals(eventType)) {
                    if (event.getBalanceAfter() != null && event.getAmount() != null) {
                        summary.setTotalWalletBalance(
                                summary.getTotalWalletBalance().add(event.getAmount()));
                    }
                } else if ("WALLET_TOPUP_REQUESTED".equals(eventType) || "WALLET_TOPUP_SETTLED".equals(eventType)) {
                    if (event.getAmount() != null) {
                        summary.setTotalWalletBalance(
                                summary.getTotalWalletBalance().add(event.getAmount()));
                    }
                }
                break;

            default:
                log.debug("ProductType no reconocido para CustomerSummary: {}", productType);
                break;
        }

        summary.setLastMovementAt(now);
        summary.setUpdatedAt(now);
        return summary;
    }

    /**
     * Determina si un eventType corresponde a la apertura de un producto.
     */
    private boolean isOpenedEvent(String eventType) {
        return eventType != null && (eventType.endsWith("_OPENED") || eventType.endsWith("_REGISTERED"));
    }

    /**
     * Determina si un eventType corresponde al cierre de un producto.
     */
    private boolean isClosedEvent(String eventType) {
        return eventType != null && eventType.endsWith("_CLOSED");
    }
}