package com.yanki.reports.infrastructure.messaging.consumer;

import com.yanki.reports.domain.event.AccountClosedEvent;
import com.yanki.reports.domain.event.AccountMovementRegisteredEvent;
import com.yanki.reports.domain.event.AccountOpenedEvent;
import com.yanki.reports.domain.event.CardClosedEvent;
import com.yanki.reports.domain.event.CardConsumptionRegisteredEvent;
import com.yanki.reports.domain.event.CardOpenedEvent;
import com.yanki.reports.domain.event.CreditOpenedEvent;
import com.yanki.reports.domain.event.CreditOverdueEvent;
import com.yanki.reports.domain.event.CreditPaymentRegisteredEvent;
import com.yanki.reports.domain.event.CustomerDeletedEvent;
import com.yanki.reports.domain.event.CustomerRegisteredEvent;
import com.yanki.reports.domain.event.CustomerUpdatedEvent;
import com.yanki.reports.domain.event.DebitPaymentSettledEvent;
import com.yanki.reports.domain.event.TransferRequestedEvent;
import com.yanki.reports.domain.event.TransferSettledEvent;
import com.yanki.reports.domain.event.WalletOpenedEvent;
import com.yanki.reports.domain.event.WalletTopUpRequestedEvent;
import com.yanki.reports.domain.event.WalletTopUpSettledEvent;
import com.yanki.reports.domain.event.WalletTransactionRegisteredEvent;
import com.yanki.reports.domain.model.ProcessedEvent;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.in.IngestEventUseCase;
import com.yanki.reports.domain.port.out.ProcessedEventPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Consumidor Kafka para eventos de dominio (20 topics).
 *
 * <p>Implementa idempotencia obligatoria usando {@link ProcessedEventPort} con clave
 * {@code <topic>:<aggregateId>:<occurredAt>}.
 *
 * <p>Transforma eventos de dominio a {@link ProductEvent} e invoca el puerto de entrada
 * {@link IngestEventUseCase} para persistir la proyección.
 *
 * <p>Topics consumidos (20):
 * <ul>
 *   <li>{@code yanki.customer.registered.v1}</li>
 *   <li>{@code yanki.customer.updated.v1}</li>
 *   <li>{@code yanki.customer.deleted.v1}</li>
 *   <li>{@code yanki.account.opened.v1}</li>
 *   <li>{@code yanki.account.closed.v1}</li>
 *   <li>{@code yanki.account.movement.registered.v1}</li>
 *   <li>{@code yanki.card.opened.v1}</li>
 *   <li>{@code yanki.card.closed.v1}</li>
 *   <li>{@code yanki.card.consumption.registered.v1}</li>
 *   <li>{@code yanki.debit.payment.settled.v1}</li>
 *   <li>{@code yanki.transfer.requested.v1}</li>
 *   <li>{@code yanki.transfer.settled.v1}</li>
 *   <li>{@code yanki.credit.opened.v1}</li>
 *   <li>{@code yanki.credit.payment.registered.v1}</li>
 *   <li>{@code yanki.credit.overdue.v1}</li>
 *   <li>{@code yanki.wallet.opened.v1}</li>
 *   <li>{@code yanki.wallet.transaction.registered.v1}</li>
 *   <li>{@code yanki.wallet.topup.requested.v1}</li>
 *   <li>{@code yanki.wallet.topup.settled.v1}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportsEventConsumer {

    private final ProcessedEventPort processedEventPort;
    private final IngestEventUseCase ingestEventUseCase;

    /**
     * Consume evento de cliente registrado.
     */
    @KafkaListener(topics = "yanki.customer.registered.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "customerRegisteredKafkaListenerContainerFactory")
    public void consumeCustomerRegistered(@Payload CustomerRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento customer.registered: customerId={}, topic={}", event.getCustomerId(), topic);
            processEvent(topic, event.getCustomerId(), event.getOccurredAt(), () ->
                    buildProductEvent("CUSTOMER_REGISTERED", "CUSTOMER", "CUSTOMER",
                            event.getCustomerId(), event.getCustomerId(), null, null, null, event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo customer.registered: customerId={}, topic={}, error={}",
                    event.getCustomerId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de cuenta abierta.
     */
    @KafkaListener(topics = "yanki.account.opened.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "accountOpenedKafkaListenerContainerFactory")
    public void consumeAccountOpened(@Payload AccountOpenedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento account.opened: accountId={}, customerId={}, topic={}",
                    event.getAccountId(), event.getCustomerId(), topic);
            processEvent(topic, event.getAccountId(), event.getOccurredAt(), () ->
                    buildProductEvent("ACCOUNT_OPENED", "ACCOUNT", event.getSubType(),
                            event.getAccountId(), event.getCustomerId(), event.getInitialBalance(), event.getInitialBalance(), null, event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo account.opened: accountId={}, customerId={}, topic={}, error={}",
                    event.getAccountId(), event.getCustomerId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de movimiento de cuenta.
     */
    @KafkaListener(topics = "yanki.account.movement.registered.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "accountMovementRegisteredKafkaListenerContainerFactory")
    public void consumeAccountMovement(@Payload AccountMovementRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento account.movement: accountId={}, amount={}, topic={}",
                    event.getAccountId(), event.getAmount(), topic);
            processEvent(topic, event.getAccountId(), event.getOccurredAt(), () ->
                    buildProductEvent("ACCOUNT_MOVEMENT", "ACCOUNT", "SAVINGS",
                            event.getAccountId(), event.getCustomerId(), event.getAmount(), event.getBalanceAfter(),
                            Map.of(
                                    "movementType", event.getMovementType(),
                                    "channel", event.getChannel(),
                                    "commissionApplied", event.getCommissionApplied(),
                                    "commissionType", event.getCommissionType()
                            ), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo account.movement: accountId={}, amount={}, topic={}, error={}",
                    event.getAccountId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de consumo de tarjeta.
     */
    @KafkaListener(topics = "yanki.card.consumption.registered.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "cardConsumptionRegisteredKafkaListenerContainerFactory")
    public void consumeCardConsumption(@Payload CardConsumptionRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento card.consumption: cardId={}, amount={}, topic={}",
                    event.getCardId(), event.getAmount(), topic);
            processEvent(topic, event.getCardId(), event.getOccurredAt(), () ->
                    buildProductEvent("CARD_CONSUMPTION", "CARD", event.getCardType(),
                            event.getCardId(), event.getCustomerId(), event.getAmount(), event.getAvailableLimitAfter(),
                            Map.of("merchantCategory", event.getMerchantCategory()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo card.consumption: cardId={}, amount={}, topic={}, error={}",
                    event.getCardId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de pago de crédito.
     */
    @KafkaListener(topics = "yanki.credit.payment.registered.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "creditPaymentRegisteredKafkaListenerContainerFactory")
    public void consumeCreditPayment(@Payload CreditPaymentRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento credit.payment: creditId={}, amount={}, topic={}",
                    event.getCreditId(), event.getAmount(), topic);
            processEvent(topic, event.getCreditId(), event.getOccurredAt(), () ->
                    buildProductEvent("CREDIT_PAYMENT", "CREDIT", "PERSONAL_LOAN",
                            event.getCreditId(), event.getCustomerId(), event.getAmount(), event.getPendingBalanceAfter(),
                            Map.of(
                                    "paymentType", event.getPaymentType(),
                                    "capitalPortion", event.getCapitalPortion(),
                                    "interestPortion", event.getInterestPortion(),
                                    "installmentNumber", event.getInstallmentNumber()
                            ), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo credit.payment: creditId={}, amount={}, topic={}, error={}",
                    event.getCreditId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de transacción de wallet.
     */
    @KafkaListener(topics = "yanki.wallet.transaction.registered.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "walletTransactionRegisteredKafkaListenerContainerFactory")
    public void consumeWalletTransaction(@Payload WalletTransactionRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento wallet.transaction: walletId={}, amount={}, topic={}",
                    event.getWalletId(), event.getAmount(), topic);
            processEvent(topic, event.getWalletId(), event.getOccurredAt(), () ->
                    buildProductEvent("WALLET_TRANSACTION", "WALLET", event.getTransactionType(),
                            event.getWalletId(), event.getCustomerId(), event.getAmount(), event.getBalanceAfter(),
                            Map.of("counterpartyWalletId", event.getCounterpartyWalletId()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo wallet.transaction: walletId={}, amount={}, topic={}, error={}",
                    event.getWalletId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    // ---------------------- Topics adicionales (14) ----------------------

    /**
     * Consume evento de cliente actualizado.
     */
    @KafkaListener(topics = "yanki.customer.updated.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "customerUpdatedKafkaListenerContainerFactory")
    public void consumeCustomerUpdated(@Payload CustomerUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento customer.updated: customerId={}, topic={}", event.getCustomerId(), topic);
            processEvent(topic, event.getCustomerId(), event.getOccurredAt(), () ->
                    buildProductEvent("CUSTOMER_UPDATED", "CUSTOMER", "CUSTOMER",
                            event.getCustomerId(), event.getCustomerId(), null, null, null, event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo customer.updated: customerId={}, topic={}, error={}",
                    event.getCustomerId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de cliente eliminado.
     */
    @KafkaListener(topics = "yanki.customer.deleted.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "customerDeletedKafkaListenerContainerFactory")
    public void consumeCustomerDeleted(@Payload CustomerDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento customer.deleted: customerId={}, topic={}", event.getCustomerId(), topic);
            processEvent(topic, event.getCustomerId(), event.getOccurredAt(), () ->
                    buildProductEvent("CUSTOMER_DELETED", "CUSTOMER", "CUSTOMER",
                            event.getCustomerId(), event.getCustomerId(), null, null, null, event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo customer.deleted: customerId={}, topic={}, error={}",
                    event.getCustomerId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de cuenta cerrada.
     */
    @KafkaListener(topics = "yanki.account.closed.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "accountClosedKafkaListenerContainerFactory")
    public void consumeAccountClosed(@Payload AccountClosedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento account.closed: accountId={}, topic={}", event.getAccountId(), topic);
            processEvent(topic, event.getAccountId(), event.getOccurredAt(), () ->
                    buildProductEvent("ACCOUNT_CLOSED", "ACCOUNT", event.getSubType(),
                            event.getAccountId(), event.getCustomerId(), event.getFinalBalance(), event.getFinalBalance(),
                            Map.of("reason", event.getReason()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo account.closed: accountId={}, topic={}, error={}",
                    event.getAccountId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de tarjeta abierta.
     */
    @KafkaListener(topics = "yanki.card.opened.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "cardOpenedKafkaListenerContainerFactory")
    public void consumeCardOpened(@Payload CardOpenedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento card.opened: cardId={}, customerId={}, topic={}",
                    event.getCardId(), event.getCustomerId(), topic);
            processEvent(topic, event.getCardId(), event.getOccurredAt(), () ->
                    buildProductEvent("CARD_OPENED", "CARD", event.getCardType(),
                            event.getCardId(), event.getCustomerId(), event.getInitialLimit(), event.getInitialLimit(),
                            Map.of("accountId", event.getAccountId()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo card.opened: cardId={}, customerId={}, topic={}, error={}",
                    event.getCardId(), event.getCustomerId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de tarjeta cerrada.
     */
    @KafkaListener(topics = "yanki.card.closed.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "cardClosedKafkaListenerContainerFactory")
    public void consumeCardClosed(@Payload CardClosedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento card.closed: cardId={}, topic={}", event.getCardId(), topic);
            processEvent(topic, event.getCardId(), event.getOccurredAt(), () ->
                    buildProductEvent("CARD_CLOSED", "CARD", event.getCardType(),
                            event.getCardId(), event.getCustomerId(), event.getFinalBalance(), event.getFinalBalance(),
                            Map.of("reason", event.getReason()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo card.closed: cardId={}, topic={}, error={}",
                    event.getCardId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de pago débito liquidado.
     */
    @KafkaListener(topics = "yanki.debit.payment.settled.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "debitPaymentSettledKafkaListenerContainerFactory")
    public void consumeDebitPaymentSettled(@Payload DebitPaymentSettledEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento debit.payment.settled: paymentId={}, amount={}, topic={}",
                    event.getPaymentId(), event.getAmount(), topic);
            processEvent(topic, event.getPaymentId(), event.getOccurredAt(), () ->
                    buildProductEvent("DEBIT_PAYMENT_SETTLED", "ACCOUNT", "SAVINGS",
                            event.getAccountId(), event.getCustomerId(), event.getAmount(), null,
                            Map.of("channel", event.getChannel()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo debit.payment.settled: paymentId={}, amount={}, topic={}, error={}",
                    event.getPaymentId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de transferencia solicitada.
     */
    @KafkaListener(topics = "yanki.transfer.requested.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "transferRequestedKafkaListenerContainerFactory")
    public void consumeTransferRequested(@Payload TransferRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento transfer.requested: transferId={}, amount={}, topic={}",
                    event.getTransferId(), event.getAmount(), topic);
            processEvent(topic, event.getTransferId(), event.getOccurredAt(), () ->
                    buildProductEvent("TRANSFER_REQUESTED", "ACCOUNT", "SAVINGS",
                            event.getSourceAccountId(), event.getCustomerId(), event.getAmount(), null,
                            Map.of(
                                    "destinationAccountId", event.getDestinationAccountId(),
                                    "transferType", event.getTransferType()
                            ), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo transfer.requested: transferId={}, amount={}, topic={}, error={}",
                    event.getTransferId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de transferencia liquidada.
     */
    @KafkaListener(topics = "yanki.transfer.settled.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "transferSettledKafkaListenerContainerFactory")
    public void consumeTransferSettled(@Payload TransferSettledEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento transfer.settled: transferId={}, amount={}, topic={}",
                    event.getTransferId(), event.getAmount(), topic);
            processEvent(topic, event.getTransferId(), event.getOccurredAt(), () ->
                    buildProductEvent("TRANSFER_SETTLED", "ACCOUNT", "SAVINGS",
                            event.getSourceAccountId(), event.getCustomerId(), event.getAmount(), null,
                            Map.of(
                                    "destinationAccountId", event.getDestinationAccountId(),
                                    "transferType", event.getTransferType()
                            ), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo transfer.settled: transferId={}, amount={}, topic={}, error={}",
                    event.getTransferId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de crédito abierto.
     */
    @KafkaListener(topics = "yanki.credit.opened.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "creditOpenedKafkaListenerContainerFactory")
    public void consumeCreditOpened(@Payload CreditOpenedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento credit.opened: creditId={}, amount={}, topic={}",
                    event.getCreditId(), event.getTotalAmount(), topic);
            processEvent(topic, event.getCreditId(), event.getOccurredAt(), () ->
                    buildProductEvent("CREDIT_OPENED", "CREDIT", event.getCreditType(),
                            event.getCreditId(), event.getCustomerId(), event.getTotalAmount(), event.getTotalAmount(),
                            Map.of(
                                    "interestRate", event.getInterestRate(),
                                    "numberOfInstallments", event.getNumberOfInstallments()
                            ), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo credit.opened: creditId={}, amount={}, topic={}, error={}",
                    event.getCreditId(), event.getTotalAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de crédito en mora.
     */
    @KafkaListener(topics = "yanki.credit.overdue.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "creditOverdueKafkaListenerContainerFactory")
    public void consumeCreditOverdue(@Payload CreditOverdueEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.warn("Consumiendo evento credit.overdue: creditId={}, overdueAmount={}, daysOverdue={}, topic={}",
                    event.getCreditId(), event.getOverdueAmount(), event.getDaysOverdue(), topic);
            processEvent(topic, event.getCreditId(), event.getOccurredAt(), () ->
                    buildProductEvent("CREDIT_OVERDUE", "CREDIT", event.getCreditType(),
                            event.getCreditId(), event.getCustomerId(), event.getOverdueAmount(), null,
                            Map.of(
                                    "daysOverdue", event.getDaysOverdue(),
                                    "installmentNumber", event.getInstallmentNumber()
                            ), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo credit.overdue: creditId={}, overdueAmount={}, topic={}, error={}",
                    event.getCreditId(), event.getOverdueAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de wallet abierta.
     */
    @KafkaListener(topics = "yanki.wallet.opened.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "walletOpenedKafkaListenerContainerFactory")
    public void consumeWalletOpened(@Payload WalletOpenedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento wallet.opened: walletId={}, customerId={}, topic={}",
                    event.getWalletId(), event.getCustomerId(), topic);
            processEvent(topic, event.getWalletId(), event.getOccurredAt(), () ->
                    buildProductEvent("WALLET_OPENED", "WALLET", "WALLET",
                            event.getWalletId(), event.getCustomerId(), event.getInitialBalance(), event.getInitialBalance(),
                            null, event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo wallet.opened: walletId={}, customerId={}, topic={}, error={}",
                    event.getWalletId(), event.getCustomerId(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de recarga wallet solicitada.
     */
    @KafkaListener(topics = "yanki.wallet.topup.requested.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "walletTopUpRequestedKafkaListenerContainerFactory")
    public void consumeWalletTopUpRequested(@Payload WalletTopUpRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento wallet.topup.requested: topUpId={}, amount={}, topic={}",
                    event.getTopUpId(), event.getAmount(), topic);
            processEvent(topic, event.getTopUpId(), event.getOccurredAt(), () ->
                    buildProductEvent("WALLET_TOPUP_REQUESTED", "WALLET", "TOPUP",
                            event.getWalletId(), event.getCustomerId(), event.getAmount(), null,
                            Map.of("sourceAccountId", event.getSourceAccountId()), event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo wallet.topup.requested: topUpId={}, amount={}, topic={}, error={}",
                    event.getTopUpId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Consume evento de recarga wallet liquidada.
     */
    @KafkaListener(topics = "yanki.wallet.topup.settled.v1", groupId = "${spring.kafka.consumer.group-id:reports-service}",
            containerFactory = "walletTopUpSettledKafkaListenerContainerFactory")
    public void consumeWalletTopUpSettled(@Payload WalletTopUpSettledEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {
        try {
            log.info("Consumiendo evento wallet.topup.settled: topUpId={}, amount={}, topic={}",
                    event.getTopUpId(), event.getAmount(), topic);
            processEvent(topic, event.getTopUpId(), event.getOccurredAt(), () ->
                    buildProductEvent("WALLET_TOPUP_SETTLED", "WALLET", "TOPUP",
                            event.getWalletId(), event.getCustomerId(), event.getAmount(), event.getBalanceAfter(),
                            null, event.getOccurredAt()),
                    ack);
        } catch (Exception ex) {
            log.error("Error de negocio consumiendo wallet.topup.settled: topUpId={}, amount={}, topic={}, error={}",
                    event.getTopUpId(), event.getAmount(), topic, ex.getMessage(), ex);
        }
    }

    /**
     * Procesa un evento genérico con idempotencia.
     *
     * @param topic       topic Kafka origen
     * @param aggregateId ID del agregado
     * @param occurredAt  timestamp del evento
     * @param eventBuilder builder para crear el ProductEvent
     * @param ack         acknowledgment manual de Kafka
     */
    private void processEvent(String topic, String aggregateId, LocalDateTime occurredAt,
            java.util.function.Supplier<ProductEvent> eventBuilder, Acknowledgment ack) {
        String eventId = buildEventId(topic, aggregateId, occurredAt);
        log.debug("Procesando evento: eventId={}, topic={}, aggregateId={}", eventId, topic, aggregateId);

        try {
            // Idempotencia: intentar registrar atómicamente
            processedEventPort.tryRegister(ProcessedEvent.builder()
                            .id(eventId)
                            .processedAt(LocalDateTime.now())
                            .build())
                    .flatMapCompletable(isNew -> {
                        if (!isNew) {
                            // Duplicado: solo log y ack
                            log.info("Evento duplicado descartado (idempotencia): eventId={}, topic={}", eventId, topic);
                            ack.acknowledge();
                            return Completable.complete();
                        }
                        // Nuevo: persistir proyección
                        log.debug("Evento nuevo detectado. Persistiendo proyección: eventId={}", eventId);
                        ProductEvent productEvent = eventBuilder.get();
                        return ingestEventUseCase.execute(productEvent)
                                .ignoreElement()
                                .doOnComplete(() -> {
                                    log.info("Evento ingerido y ack completado: eventId={}, topic={}", eventId, topic);
                                    ack.acknowledge();
                                })
                                .doOnError(e -> {
                                    log.error("Error ingiriendo evento en proyección: eventId={}, error={}",
                                            eventId, e.getMessage(), e);
                                });
                    })
                    .doOnError(e -> {
                        log.error("Error procesando evento de Kafka: eventId={}, error={}. No se hace ack para permitir reintento.",
                                eventId, e.getMessage(), e);
                        // NO hacer ack en error para permitir reintento (dead letter queue manejará venenosos)
                    })
                    .subscribe(
                            () -> log.debug("Suscripción de evento completada: eventId={}", eventId),
                            e -> log.error("Error en suscripción de evento: eventId={}, error={}", eventId, e.getMessage(), e)
                    );
        } catch (Exception ex) {
            log.error("Error inesperado en processEvent(): eventId={}, topic={}, aggregateId={}",
                    eventId, topic, aggregateId, ex);
        }
    }

    /**
     * Construye la clave de idempotencia: {@code <topic>:<aggregateId>:<occurredAt>}.
     * Si {@code occurredAt} es null, usa el momento actual como fallback.
     */
    private String buildEventId(String topic, String aggregateId, LocalDateTime occurredAt) {
        LocalDateTime timestamp = occurredAt != null ? occurredAt : LocalDateTime.now();
        return String.format("%s:%s:%s", topic, aggregateId, timestamp.toString());
    }

    /**
     * Construye un ProductEvent con campos comunes.
     */
    private ProductEvent buildProductEvent(String eventType, String productType, String subType,
            String aggregateId, String customerId, java.math.BigDecimal amount,
            java.math.BigDecimal balanceAfter, Map<String, Object> metadata, LocalDateTime occurredAt) {
        return ProductEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .customerId(customerId)
                .productType(productType)
                .subType(subType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .metadata(metadata)
                .occurredAt(occurredAt)
                .ingestedAt(LocalDateTime.now())
                .build();
    }
}