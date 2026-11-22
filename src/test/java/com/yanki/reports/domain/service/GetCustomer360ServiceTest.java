package com.yanki.reports.domain.service;

import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.out.CustomerSummaryPersistencePort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import com.yanki.reports.domain.port.out.ProductSummaryPersistencePort;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link GetCustomer360Service}.
 *
 * <p>Verifica que el servicio agrega correctamente los datos de los tres puertos
 * de persistencia (CustomerSummary, ProductSummary, ProductEvent) para construir
 * la vista 360° del cliente.
 */
@ExtendWith(MockitoExtension.class)
class GetCustomer360ServiceTest {

    @Mock
    private CustomerSummaryPersistencePort customerSummaryPort;

    @Mock
    private ProductSummaryPersistencePort productSummaryPort;

    @Mock
    private ProductEventPersistencePort productEventPort;

    private GetCustomer360Service service;

    @BeforeEach
    void setUp() {
        service = new GetCustomer360Service(
                customerSummaryPort,
                productSummaryPort,
                productEventPort
        );
    }

    @Test
    @DisplayName("Debe retornar Customer360 con resumen, productos y eventos cuando el cliente existe")
    void shouldReturnCustomer360WithAllData_whenCustomerExists() {
        // Arrange
        String customerId = "cust-001";

        CustomerSummary summary = CustomerSummary.builder()
                .customerId(customerId)
                .customerType("NATURAL")
                .profile("PERSONAL")
                .status("ACTIVE")
                .totalAccounts(2)
                .totalCards(1)
                .totalCredits(1)
                .totalWallets(1)
                .totalBalanceAccounts(new BigDecimal("15000.50"))
                .totalCreditLimit(new BigDecimal("50000.00"))
                .totalCreditDebt(new BigDecimal("12000.00"))
                .totalWalletBalance(new BigDecimal("500.00"))
                .hasOverdueDebt(false)
                .lastMovementAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0))
                .updatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 5))
                .build();

        ProductSummary account = ProductSummary.builder()
                .productId("ACC-001")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .customerId(customerId)
                .status("ACTIVE")
                .currentBalance(new BigDecimal("10000.00"))
                .currency("PEN")
                .openedAt(LocalDateTime.of(2023, 1, 1, 0, 0, 0))
                .build();

        ProductSummary card = ProductSummary.builder()
                .productId("CARD-001")
                .productType("CARD")
                .subType("CREDIT")
                .customerId(customerId)
                .status("ACTIVE")
                .creditLimit(new BigDecimal("50000.00"))
                .availableLimit(new BigDecimal("38000.00"))
                .currency("PEN")
                .openedAt(LocalDateTime.of(2023, 6, 1, 0, 0, 0))
                .build();

        ProductEvent event1 = ProductEvent.builder()
                .id("evt-001")
                .eventType("ACCOUNT_DEPOSIT")
                .aggregateId("ACC-001")
                .customerId(customerId)
                .productType("ACCOUNT")
                .amount(new BigDecimal("5000.00"))
                .occurredAt(LocalDateTime.of(2024, 6, 15, 10, 0, 0))
                .build();

        ProductEvent event2 = ProductEvent.builder()
                .id("evt-002")
                .eventType("CARD_CONSUMPTION")
                .aggregateId("CARD-001")
                .customerId(customerId)
                .productType("CARD")
                .amount(new BigDecimal("12000.00"))
                .occurredAt(LocalDateTime.of(2024, 6, 14, 15, 0, 0))
                .build();

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.just(summary));
        when(productSummaryPort.findByCustomerId(customerId))
                .thenReturn(Flowable.fromIterable(List.of(account, card)));
        when(productEventPort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.fromIterable(List.of(event1, event2)));

        // Act & Assert
        StepVerifier.create(
                RxJava3Adapter.maybeToMono(service.execute(customerId))
        )
        .expectNextMatches(customer360 ->
                customer360.getCustomerId().equals(customerId)
                        && customer360.getCustomerType().equals("NATURAL")
                        && customer360.getProfile().equals("PERSONAL")
                        && customer360.getStatus().equals("ACTIVE")
                        && customer360.getTotalAccounts() == 2
                        && customer360.getTotalCards() == 1
                        && customer360.getTotalCredits() == 1
                        && customer360.getTotalWallets() == 1
                        && customer360.getTotalBalanceAccounts().equals(new BigDecimal("15000.50"))
                        && customer360.getTotalCreditLimit().equals(new BigDecimal("50000.00"))
                        && customer360.getTotalCreditDebt().equals(new BigDecimal("12000.00"))
                        && customer360.getTotalWalletBalance().equals(new BigDecimal("500.00"))
                        && !customer360.getHasOverdueDebt()
                        && customer360.getProducts() != null
                        && customer360.getProducts().size() == 2
                        && customer360.getRecentEvents() != null
                        && customer360.getRecentEvents().size() == 2
        )
        .verifyComplete();
    }

    @Test
    @DisplayName("Debe retornar vacío cuando el cliente no existe en CustomerSummary")
    void shouldReturnEmpty_whenCustomerNotFound() {
        // Arrange
        String customerId = "non-existent";

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.<CustomerSummary>empty());

        // Act & Assert
        StepVerifier.create(
                RxJava3Adapter.maybeToMono(service.execute(customerId))
        )
        .verifyComplete();
    }

    @Test
    @DisplayName("Debe retornar Customer360 con listas vacías cuando no hay productos ni eventos")
    void shouldReturnEmptyLists_whenNoProductsOrEvents() {
        // Arrange
        String customerId = "cust-empty";

        CustomerSummary summary = CustomerSummary.builder()
                .customerId(customerId)
                .customerType("NATURAL")
                .profile("PERSONAL")
                .status("ACTIVE")
                .totalAccounts(0)
                .totalCards(0)
                .totalCredits(0)
                .totalWallets(0)
                .totalBalanceAccounts(BigDecimal.ZERO)
                .totalCreditLimit(BigDecimal.ZERO)
                .totalCreditDebt(BigDecimal.ZERO)
                .totalWalletBalance(BigDecimal.ZERO)
                .hasOverdueDebt(false)
                .updatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 5))
                .build();

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.just(summary));
        when(productSummaryPort.findByCustomerId(customerId))
                .thenReturn(Flowable.<ProductSummary>empty());
        when(productEventPort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.<ProductEvent>empty());

        // Act & Assert
        StepVerifier.create(
                RxJava3Adapter.maybeToMono(service.execute(customerId))
        )
        .expectNextMatches(customer360 ->
                customer360.getCustomerId().equals(customerId)
                        && customer360.getProducts() != null
                        && customer360.getProducts().isEmpty()
                        && customer360.getRecentEvents() != null
                        && customer360.getRecentEvents().isEmpty()
        )
        .verifyComplete();
    }

    @Test
    @DisplayName("Debe limitar los eventos recientes a 20 máximo")
    void shouldLimitRecentEventsTo20() {
        // Arrange
        String customerId = "cust-many-events";

        CustomerSummary summary = CustomerSummary.builder()
                .customerId(customerId)
                .customerType("NATURAL")
                .profile("PERSONAL")
                .status("ACTIVE")
                .totalAccounts(1)
                .totalCards(0)
                .totalCredits(0)
                .totalWallets(0)
                .totalBalanceAccounts(new BigDecimal("1000.00"))
                .totalCreditLimit(BigDecimal.ZERO)
                .totalCreditDebt(BigDecimal.ZERO)
                .totalWalletBalance(BigDecimal.ZERO)
                .hasOverdueDebt(false)
                .updatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 5))
                .build();

        // Create 25 events
        List<ProductEvent> manyEvents = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            manyEvents.add(ProductEvent.builder()
                    .id("evt-" + i)
                    .eventType("ACCOUNT_DEPOSIT")
                    .aggregateId("ACC-001")
                    .customerId(customerId)
                    .productType("ACCOUNT")
                    .amount(new BigDecimal(i * 100))
                    .occurredAt(LocalDateTime.of(2024, 6, 15, 10, i, 0))
                    .build());
        }

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.just(summary));
        when(productSummaryPort.findByCustomerId(customerId))
                .thenReturn(Flowable.<ProductSummary>empty());
        when(productEventPort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.fromIterable(manyEvents));

        // Act & Assert
        StepVerifier.create(
                RxJava3Adapter.maybeToMono(service.execute(customerId))
        )
        .expectNextMatches(customer360 ->
                customer360.getRecentEvents() != null
                        && customer360.getRecentEvents().size() == 20
        )
        .verifyComplete();
    }

    @Test
    @DisplayName("Debe incluir productos de diferentes tipos en la respuesta")
    void shouldIncludeProductsOfDifferentTypes() {
        // Arrange
        String customerId = "cust-multi";

        CustomerSummary summary = CustomerSummary.builder()
                .customerId(customerId)
                .customerType("JURIDICA")
                .profile("EMPRESARIAL")
                .status("ACTIVE")
                .totalAccounts(1)
                .totalCards(1)
                .totalCredits(1)
                .totalWallets(1)
                .totalBalanceAccounts(new BigDecimal("100000.00"))
                .totalCreditLimit(new BigDecimal("200000.00"))
                .totalCreditDebt(new BigDecimal("50000.00"))
                .totalWalletBalance(new BigDecimal("5000.00"))
                .hasOverdueDebt(false)
                .updatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 5))
                .build();

        ProductSummary account = ProductSummary.builder()
                .productId("ACC-001")
                .productType("ACCOUNT")
                .subType("CHECKING")
                .customerId(customerId)
                .status("ACTIVE")
                .currentBalance(new BigDecimal("100000.00"))
                .currency("PEN")
                .build();

        ProductSummary card = ProductSummary.builder()
                .productId("CARD-001")
                .productType("CARD")
                .subType("DEBIT")
                .customerId(customerId)
                .status("ACTIVE")
                .currency("PEN")
                .build();

        ProductSummary credit = ProductSummary.builder()
                .productId("CRD-001")
                .productType("CREDIT")
                .subType("PERSONAL_LOAN")
                .customerId(customerId)
                .status("ACTIVE")
                .outstandingBalance(new BigDecimal("50000.00"))
                .currency("PEN")
                .build();

        ProductSummary wallet = ProductSummary.builder()
                .productId("WLT-001")
                .productType("WALLET")
                .subType("WALLET")
                .customerId(customerId)
                .status("ACTIVE")
                .currentBalance(new BigDecimal("5000.00"))
                .currency("PEN")
                .build();

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.just(summary));
        when(productSummaryPort.findByCustomerId(customerId))
                .thenReturn(Flowable.fromIterable(List.of(account, card, credit, wallet)));
        when(productEventPort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.<ProductEvent>empty());

        // Act & Assert
        StepVerifier.create(
                RxJava3Adapter.maybeToMono(service.execute(customerId))
        )
        .expectNextMatches(customer360 ->
                customer360.getProducts() != null
                        && customer360.getProducts().size() == 4
                        && customer360.getCustomerType().equals("JURIDICA")
                        && customer360.getProfile().equals("EMPRESARIAL")
        )
        .verifyComplete();
    }

    @Test
    @DisplayName("Debe propagar error cuando CustomerSummaryPort falla")
    void shouldPropagateError_whenCustomerSummaryPortFails() {
        // Arrange
        String customerId = "cust-error";

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.error(new RuntimeException("Database connection failed")));

        // Act & Assert
        StepVerifier.create(
                RxJava3Adapter.maybeToMono(service.execute(customerId))
        )
        .expectError(RuntimeException.class)
        .verify();
    }

    @Test
    @DisplayName("Debe llamar a los puertos con el customerId correcto")
    void shouldCallPortsWithCorrectCustomerId() {
        // Arrange
        String customerId = "cust-verify";

        CustomerSummary summary = CustomerSummary.builder()
                .customerId(customerId)
                .customerType("NATURAL")
                .profile("PERSONAL")
                .status("ACTIVE")
                .totalAccounts(0)
                .totalCards(0)
                .totalCredits(0)
                .totalWallets(0)
                .totalBalanceAccounts(BigDecimal.ZERO)
                .totalCreditLimit(BigDecimal.ZERO)
                .totalCreditDebt(BigDecimal.ZERO)
                .totalWalletBalance(BigDecimal.ZERO)
                .hasOverdueDebt(false)
                .updatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 5))
                .build();

        when(customerSummaryPort.findByCustomerId(customerId))
                .thenReturn(Maybe.just(summary));
        when(productSummaryPort.findByCustomerId(customerId))
                .thenReturn(Flowable.<ProductSummary>empty());
        when(productEventPort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.<ProductEvent>empty());

        // Act
        RxJava3Adapter.maybeToMono(service.execute(customerId)).block();

        // Assert
        verify(customerSummaryPort).findByCustomerId(customerId);
        verify(productSummaryPort).findByCustomerId(customerId);
        verify(productEventPort).findByCustomerIdOrderByOccurredAtDesc(customerId);
    }
}
