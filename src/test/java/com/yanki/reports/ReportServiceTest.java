package com.yanki.reports;

import com.yanki.reports.domain.model.AggregatedReport;
import com.yanki.reports.domain.model.CardMovementsReport;
import com.yanki.reports.domain.model.CustomerReport;
import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.out.CachePort;
import com.yanki.reports.domain.port.out.CustomerSummaryPersistencePort;
import com.yanki.reports.domain.port.out.DailyAggregatePersistencePort;
import com.yanki.reports.domain.port.out.KafkaLagPort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import com.yanki.reports.domain.port.out.ProductSummaryPersistencePort;
import com.yanki.reports.domain.service.GenerateAggregatedReportService;
import com.yanki.reports.domain.service.GetCardMovementsService;
import com.yanki.reports.domain.service.GetCustomerReportService;
import com.yanki.reports.domain.service.GetCustomerSummaryService;
import com.yanki.reports.domain.service.GetDailyAggregatesService;
import com.yanki.reports.domain.service.GetProductEventsByDateRangeService;
import com.yanki.reports.domain.service.GetProductSummaryService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de los casos de uso (domain.service) del reports-service.
 *
 * <p>Los puertos de salida (ProductEventPersistencePort, CachePort, KafkaLagPort)
 * estan mockeados para demostrar que el dominio funciona 100% aislado
 * sin Spring, MongoDB ni Kafka.
 */
class ReportServiceTest {

    @Mock
    private ProductEventPersistencePort persistencePort;

    @Mock
    private CachePort cachePort;

    @Mock
    private KafkaLagPort kafkaLagPort;

    @Mock
    private CustomerSummaryPersistencePort customerSummaryPersistencePort;

    @Mock
    private ProductSummaryPersistencePort productSummaryPersistencePort;

    @Mock
    private DailyAggregatePersistencePort dailyAggregatePersistencePort;

    private GenerateAggregatedReportService generateAggregatedReportService;
    private GetCardMovementsService getCardMovementsService;
    private GetCustomerReportService getCustomerReportService;
    private GetProductEventsByDateRangeService getProductEventsByDateRangeService;
    private GetCustomerSummaryService getCustomerSummaryService;
    private GetProductSummaryService getProductSummaryService;
    private GetDailyAggregatesService getDailyAggregatesService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        generateAggregatedReportService = new GenerateAggregatedReportService(persistencePort, cachePort, kafkaLagPort);
        getCardMovementsService = new GetCardMovementsService(persistencePort, cachePort, kafkaLagPort);
        getCustomerReportService = new GetCustomerReportService(persistencePort, cachePort, kafkaLagPort);
        getProductEventsByDateRangeService = new GetProductEventsByDateRangeService(persistencePort, kafkaLagPort);
        getCustomerSummaryService = new GetCustomerSummaryService(customerSummaryPersistencePort);
        getProductSummaryService = new GetProductSummaryService(productSummaryPersistencePort);
        getDailyAggregatesService = new GetDailyAggregatesService(dailyAggregatePersistencePort);
    }

    // ---------------------- Aggregated Report ----------------------

    @Test
    void testGetAggregatedReport_HappyPath() {
        when(cachePort.get(anyString(), eq(AggregatedReport.class))).thenReturn(Maybe.empty());
        when(kafkaLagPort.isLagHigh(1000L)).thenReturn(Single.just(false));
        ProductEvent event = createEvent("ACCOUNT", "NATURAL", BigDecimal.valueOf(1000));
        when(persistencePort.findByProductTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                eq("ACCOUNT"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flowable.just(event));
        when(cachePort.put(anyString(), any(AggregatedReport.class), any(java.time.Duration.class)))
                .thenReturn(Single.just(MockFixtures.createAggregatedReport()));

        TestObserver<AggregatedReport> testObserver = generateAggregatedReportService
                .execute("ACCOUNT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "NATURAL")
                .test();

        testObserver.assertComplete();
        testObserver.assertValue(report -> report.getCount() == 1
                && report.getSumAmount().compareTo(BigDecimal.valueOf(1000)) == 0
                && report.getAverageAmount().compareTo(BigDecimal.valueOf(1000)) == 0);
    }

    @Test
    void testGetAggregatedReport_FromAfterTo() {
        TestObserver<AggregatedReport> testObserver = generateAggregatedReportService
                .execute("ACCOUNT", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), "NATURAL")
                .test();

        testObserver.assertError(com.yanki.reports.domain.exception.InvalidDateRangeException.class);
    }

    // ---------------------- Card Movements ----------------------

    @Test
    void testGetCardMovements_Success() {
        String cardId = "card789";
        when(cachePort.get(anyString(), eq(CardMovementsReport.class))).thenReturn(Maybe.empty());
        when(kafkaLagPort.isLagHigh(1000L)).thenReturn(Single.just(false));
        when(persistencePort.existsByAggregateId(cardId)).thenReturn(Single.just(true));
        ProductEvent event1 = createCardEvent(cardId, BigDecimal.valueOf(150));
        ProductEvent event2 = createCardEvent(cardId, BigDecimal.valueOf(200));
        when(persistencePort.findByAggregateIdOrderByOccurredAtDesc(cardId))
                .thenReturn(Flowable.just(event1, event2));
        when(cachePort.put(anyString(), any(CardMovementsReport.class), any(java.time.Duration.class)))
                .thenReturn(Single.just(CardMovementsReport.builder().cardId("card789").productType("CARD").subType("CREDIT").movements(List.of()).build()));

        TestObserver<CardMovementsReport> testObserver = getCardMovementsService
                .execute(cardId, 10)
                .test();

        testObserver.assertComplete();
        testObserver.assertValue(report -> report.getCardId().equals(cardId)
                && report.getProductType().equals("CARD")
                && report.getMovements().size() == 2);
    }

    @Test
    void testGetCardMovements_NotFound() {
        String cardId = "nonexistent";
        when(cachePort.get(anyString(), eq(CardMovementsReport.class))).thenReturn(Maybe.empty());
        when(kafkaLagPort.isLagHigh(1000L)).thenReturn(Single.just(false));
        when(persistencePort.existsByAggregateId(cardId)).thenReturn(Single.just(false));

        TestObserver<CardMovementsReport> testObserver = getCardMovementsService
                .execute(cardId, 10)
                .test();

        testObserver.assertError(com.yanki.reports.domain.exception.ProductNotFoundException.class);
    }

    // ---------------------- Customer Report ----------------------

    @Test
    void testGetCustomerReport_Success() {
        String customerId = "cust-001";
        when(cachePort.get(anyString(), eq(CustomerReport.class))).thenReturn(Maybe.empty());
        when(kafkaLagPort.isLagHigh(1000L)).thenReturn(Single.just(false));
        ProductEvent event1 = createEvent("ACCOUNT", "SAVINGS", BigDecimal.valueOf(1000));
        event1.setAggregateId("acc123");
        event1.setCustomerId(customerId);
        ProductEvent event2 = createEvent("CARD", "CREDIT", BigDecimal.valueOf(500));
        event2.setAggregateId("card456");
        event2.setCustomerId(customerId);
        when(persistencePort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.just(event1, event2));
        when(cachePort.put(anyString(), any(CustomerReport.class), any(java.time.Duration.class)))
                .thenReturn(Single.just(CustomerReport.builder().customerId("cust-001").products(List.of()).build()));

        TestObserver<CustomerReport> testObserver = getCustomerReportService
                .execute(customerId)
                .test();

        testObserver.assertComplete();
        testObserver.assertValue(report -> report.getCustomerId().equals(customerId)
                && report.getProducts().size() == 2);
    }

    @Test
    void testGetCustomerReport_Empty() {
        String customerId = "nonexistent";
        when(cachePort.get(anyString(), eq(CustomerReport.class))).thenReturn(Maybe.empty());
        when(kafkaLagPort.isLagHigh(1000L)).thenReturn(Single.just(false));
        when(persistencePort.findByCustomerIdOrderByOccurredAtDesc(customerId))
                .thenReturn(Flowable.empty());
        when(cachePort.put(anyString(), any(CustomerReport.class), any(java.time.Duration.class)))
                .thenReturn(Single.just(CustomerReport.builder().customerId("nonexistent").products(List.of()).build()));

        TestObserver<CustomerReport> testObserver = getCustomerReportService
                .execute(customerId)
                .test();

        testObserver.assertComplete();
        testObserver.assertValue(report -> report.getCustomerId().equals(customerId)
                && report.getProducts().isEmpty());
    }

    // ---------------------- Product Events by Date Range ----------------------

    @Test
    void testGetProductEventsByDateRange_Success() {
        when(kafkaLagPort.isLagHigh(1000L)).thenReturn(Single.just(false));
        ProductEvent event = createEvent("ACCOUNT", "SAVINGS", BigDecimal.valueOf(1000));
        when(persistencePort.findByOccurredAtBetweenOrderByOccurredAtDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flowable.just(event));

        getProductEventsByDateRangeService
                .execute(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
                .test()
                .assertComplete()
                .assertValue(e -> e.getEventType().equals("ACCOUNT_OPENED"));
    }

    @Test
    void testGetProductEventsByDateRange_FromAfterTo() {
        getProductEventsByDateRangeService
                .execute(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1))
                .test()
                .assertError(com.yanki.reports.domain.exception.InvalidDateRangeException.class);
    }

    // ---------------------- Customer Summary ----------------------

    @Test
    void testGetCustomerSummary_Success() {
        String customerId = "6af39f59b089e973a5ffe9b6";
        CustomerSummary summary = MockFixtures.createCustomerSummary();
        when(customerSummaryPersistencePort.findByCustomerId(customerId)).thenReturn(Maybe.just(summary));

        TestObserver<CustomerSummary> testObserver = getCustomerSummaryService
                .execute(customerId)
                .test();

        testObserver.assertComplete();
        testObserver.assertValue(s -> s.getCustomerId().equals(customerId)
                && s.getTotalAccounts() == 3
                && s.getTotalCards() == 2
                && s.getStatus().equals("ACTIVE"));
    }

    @Test
    void testGetCustomerSummary_NotFound() {
        String customerId = "nonexistent";
        when(customerSummaryPersistencePort.findByCustomerId(customerId)).thenReturn(Maybe.empty());

        TestObserver<CustomerSummary> testObserver = getCustomerSummaryService
                .execute(customerId)
                .test();

        testObserver.assertComplete();
        testObserver.assertNoValues();
    }

    // ---------------------- Product Summary ----------------------

    @Test
    void testGetProductSummary_ByProductId_Success() {
        String productId = "ACC-12345";
        ProductSummary summary = MockFixtures.createProductSummary();
        when(productSummaryPersistencePort.findByProductId(productId)).thenReturn(Maybe.just(summary));

        TestObserver<ProductSummary> testObserver = getProductSummaryService
                .executeByProductId(productId)
                .test();

        testObserver.assertComplete();
        testObserver.assertValue(s -> s.getProductId().equals(productId)
                && s.getProductType().equals("ACCOUNT")
                && s.getSubType().equals("SAVINGS")
                && s.getStatus().equals("ACTIVE"));
    }

    @Test
    void testGetProductSummary_ByProductId_NotFound() {
        String productId = "nonexistent";
        when(productSummaryPersistencePort.findByProductId(productId)).thenReturn(Maybe.empty());

        TestObserver<ProductSummary> testObserver = getProductSummaryService
                .executeByProductId(productId)
                .test();

        testObserver.assertComplete();
        testObserver.assertNoValues();
    }

    @Test
    void testGetProductSummary_ByCustomerId_Success() {
        String customerId = "6af39f59b089e973a5ffe9b6";
        ProductSummary summary1 = MockFixtures.createProductSummary();
        ProductSummary summary2 = ProductSummary.builder()
                .productId("CARD-789")
                .productType("CARD")
                .subType("CREDIT")
                .customerId(customerId)
                .status("ACTIVE")
                .creditLimit(BigDecimal.valueOf(10000.00))
                .availableLimit(BigDecimal.valueOf(7500.00))
                .currency("PEN")
                .openedAt(LocalDateTime.of(2024, 3, 10, 10, 0))
                .updatedAt(LocalDateTime.now())
                .build();
        when(productSummaryPersistencePort.findByCustomerId(customerId))
                .thenReturn(Flowable.just(summary1, summary2));

        TestSubscriber<ProductSummary> testSubscriber = getProductSummaryService
                .executeByCustomerId(customerId)
                .test();

        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(2);
    }

    @Test
    void testGetProductSummary_ByCustomerId_Empty() {
        String customerId = "nonexistent";
        when(productSummaryPersistencePort.findByCustomerId(customerId)).thenReturn(Flowable.empty());

        TestSubscriber<ProductSummary> testSubscriber = getProductSummaryService
                .executeByCustomerId(customerId)
                .test();

        testSubscriber.assertComplete();
        testSubscriber.assertNoValues();
    }

    // ---------------------- Daily Aggregates ----------------------

    @Test
    void testGetDailyAggregates_ByDate_Success() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        DailyAggregate agg = MockFixtures.createDailyAggregate();
        when(dailyAggregatePersistencePort.findByDate(date)).thenReturn(Flowable.just(agg));

        TestSubscriber<DailyAggregate> testSubscriber = getDailyAggregatesService
                .executeByDate(date)
                .test();

        testSubscriber.assertComplete();
        testSubscriber.assertValue(a -> a.getDate().equals(date)
                && a.getProductType().equals("ACCOUNT")
                && a.getMovementCount() == 120);
    }

    @Test
    void testGetDailyAggregates_ByDate_Empty() {
        LocalDate date = LocalDate.of(2020, 1, 1);
        when(dailyAggregatePersistencePort.findByDate(date)).thenReturn(Flowable.empty());

        TestSubscriber<DailyAggregate> testSubscriber = getDailyAggregatesService
                .executeByDate(date)
                .test();

        testSubscriber.assertComplete();
        testSubscriber.assertNoValues();
    }

    @Test
    void testGetDailyAggregates_ByDateRange_WithProductType() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        DailyAggregate agg1 = DailyAggregate.builder()
                .id("2026-06-15:ACCOUNT:NATURAL")
                .date(LocalDate.of(2026, 6, 15))
                .productType("ACCOUNT")
                .customerType("NATURAL")
                .movementCount(120)
                .totalAmount(BigDecimal.valueOf(24000.50))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(45)
                .uniqueProducts(30)
                .updatedAt(LocalDateTime.now())
                .build();
        DailyAggregate agg2 = DailyAggregate.builder()
                .id("2026-06-16:ACCOUNT:NATURAL")
                .date(LocalDate.of(2026, 6, 16))
                .productType("ACCOUNT")
                .customerType("NATURAL")
                .movementCount(95)
                .totalAmount(BigDecimal.valueOf(19000.00))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(40)
                .uniqueProducts(25)
                .updatedAt(LocalDateTime.now())
                .build();
        when(dailyAggregatePersistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(
                from, to, "ACCOUNT")).thenReturn(Flowable.just(agg1, agg2));

        TestSubscriber<DailyAggregate> testSubscriber = getDailyAggregatesService
                .executeByDateRange(from, to, "ACCOUNT")
                .test();

        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(2);
    }

    @Test
    void testGetDailyAggregates_ByDateRange_AllProductTypes() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        DailyAggregate accountAgg = DailyAggregate.builder()
                .id("2026-06-15:ACCOUNT:NATURAL")
                .date(LocalDate.of(2026, 6, 15))
                .productType("ACCOUNT")
                .customerType("NATURAL")
                .movementCount(120)
                .totalAmount(BigDecimal.valueOf(24000.50))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(45)
                .uniqueProducts(30)
                .updatedAt(LocalDateTime.now())
                .build();
        DailyAggregate cardAgg = DailyAggregate.builder()
                .id("2026-06-15:CARD:NATURAL")
                .date(LocalDate.of(2026, 6, 15))
                .productType("CARD")
                .customerType("NATURAL")
                .movementCount(80)
                .totalAmount(BigDecimal.valueOf(16000.00))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(30)
                .uniqueProducts(20)
                .updatedAt(LocalDateTime.now())
                .build();
        when(dailyAggregatePersistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(
                from, to, "ACCOUNT")).thenReturn(Flowable.just(accountAgg));
        when(dailyAggregatePersistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(
                from, to, "CARD")).thenReturn(Flowable.just(cardAgg));
        when(dailyAggregatePersistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(
                from, to, "CREDIT")).thenReturn(Flowable.empty());
        when(dailyAggregatePersistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(
                from, to, "WALLET")).thenReturn(Flowable.empty());

        TestSubscriber<DailyAggregate> testSubscriber = getDailyAggregatesService
                .executeByDateRange(from, to, null)
                .test();

        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(2);
    }

    // ---------------------- Helpers ----------------------

    private ProductEvent createEvent(String productType, String subType, BigDecimal amount) {
        return ProductEvent.builder()
                .id("event-001")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("acc123")
                .customerId("cust-001")
                .productType(productType)
                .subType(subType)
                .amount(amount)
                .balanceAfter(amount)
                .metadata(Collections.emptyMap())
                .occurredAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .ingestedAt(LocalDateTime.now())
                .build();
    }

    private ProductEvent createCardEvent(String cardId, BigDecimal amount) {
        return ProductEvent.builder()
                .id("mov-001")
                .eventType("CARD_CONSUMPTION")
                .aggregateId(cardId)
                .customerId("cust-001")
                .productType("CARD")
                .subType("CREDIT")
                .amount(amount)
                .balanceAfter(BigDecimal.valueOf(5000).subtract(amount))
                .metadata(Collections.emptyMap())
                .occurredAt(LocalDateTime.of(2026, 11, 10, 12, 0))
                .ingestedAt(LocalDateTime.now())
                .build();
    }
}
