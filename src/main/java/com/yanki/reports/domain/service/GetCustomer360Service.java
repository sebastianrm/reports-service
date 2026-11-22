package com.yanki.reports.domain.service;

import com.yanki.reports.domain.model.Customer360;
import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.in.GetCustomer360UseCase;
import com.yanki.reports.domain.port.out.CustomerSummaryPersistencePort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import com.yanki.reports.domain.port.out.ProductSummaryPersistencePort;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del caso de uso para obtener la vista 360° completa de un cliente.
 *
 * <p>Agrega datos de tres fuentes de proyección MongoDB:
 * <ul>
 *   <li>CustomerSummary: información general y totales consolidados</li>
 *   <li>ProductSummary: estado actual de cada producto</li>
 *   <li>ProductEvent: historial reciente de eventos</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetCustomer360Service implements GetCustomer360UseCase {

    private static final int MAX_RECENT_EVENTS = 20;

    private final CustomerSummaryPersistencePort customerSummaryPort;
    private final ProductSummaryPersistencePort productSummaryPort;
    private final ProductEventPersistencePort productEventPort;

    @Override
    public Maybe<Customer360> execute(String customerId) {
        log.info("Ejecutando consulta de vista 360° del cliente: customerId={}", customerId);

        try {
            return customerSummaryPort.findByCustomerId(customerId)
                    .flatMap(summary -> {
                        log.debug("Resumen de cliente encontrado: customerId={}", customerId);
                        return productSummaryPort.findByCustomerId(customerId)
                                .take(MAX_RECENT_EVENTS)
                                .toList()
                                .toMaybe()
                                .flatMap(products ->
                                        productEventPort.findByCustomerIdOrderByOccurredAtDesc(customerId)
                                                .take(MAX_RECENT_EVENTS)
                                                .toList()
                                                .toMaybe()
                                                .map(events ->
                                                        buildCustomer360(summary, products, events))
                                );
                    })
                    .doOnSuccess(customer360 ->
                            log.info("Vista 360° del cliente obtenida exitosamente: customerId={}, products={}, events={}",
                                    customerId, customer360.getProducts().size(), customer360.getRecentEvents().size()))
                    .doOnComplete(() ->
                            log.debug("Consulta de vista 360° completada: customerId={}", customerId))
                    .doOnError(e -> log.error("Error obteniendo vista 360° del cliente: customerId={}, error={}",
                            customerId, e.getMessage(), e));
        } catch (Exception ex) {
            log.error("Error inesperado en execute() de vista 360° del cliente: customerId={}", customerId, ex);
            throw ex;
        }
    }

    private Customer360 buildCustomer360(
            CustomerSummary summary,
            List<ProductSummary> products,
            List<ProductEvent> events) {

        return Customer360.builder()
                .customerId(summary.getCustomerId())
                .customerType(summary.getCustomerType())
                .profile(summary.getProfile())
                .status(summary.getStatus())
                .totalAccounts(summary.getTotalAccounts())
                .totalCards(summary.getTotalCards())
                .totalCredits(summary.getTotalCredits())
                .totalWallets(summary.getTotalWallets())
                .totalBalanceAccounts(summary.getTotalBalanceAccounts())
                .totalCreditLimit(summary.getTotalCreditLimit())
                .totalCreditDebt(summary.getTotalCreditDebt())
                .totalWalletBalance(summary.getTotalWalletBalance())
                .hasOverdueDebt(summary.getHasOverdueDebt())
                .lastMovementAt(summary.getLastMovementAt())
                .updatedAt(summary.getUpdatedAt())
                .products(products)
                .recentEvents(events)
                .build();
    }
}
