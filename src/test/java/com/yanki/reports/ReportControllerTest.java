package com.yanki.reports;

import com.yanki.reports.domain.exception.InvalidDateRangeException;
import com.yanki.reports.domain.exception.ProductNotFoundException;
import com.yanki.reports.domain.model.AggregatedReport;
import com.yanki.reports.domain.model.CardMovementsReport;
import com.yanki.reports.domain.model.CustomerReport;
import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductGroup;
import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.in.GenerateAggregatedReportUseCase;
import com.yanki.reports.domain.port.in.GetCardMovementsUseCase;
import com.yanki.reports.domain.port.in.GetCustomer360UseCase;
import com.yanki.reports.domain.port.in.GetCustomerReportUseCase;
import com.yanki.reports.domain.port.in.GetCustomerSummaryUseCase;
import com.yanki.reports.domain.port.in.GetDailyAggregatesUseCase;
import com.yanki.reports.domain.port.in.GetProductEventsByDateRangeUseCase;
import com.yanki.reports.domain.port.in.GetProductSummaryUseCase;
import com.yanki.reports.interfaces.rest.ReportController;
import com.yanki.reports.interfaces.rest.dto.AggregatedReportResponse;
import com.yanki.reports.interfaces.rest.dto.CardMovementsResponse;
import com.yanki.reports.interfaces.rest.dto.CustomerReportResponse;
import com.yanki.reports.interfaces.rest.dto.CustomerSummaryResponse;
import com.yanki.reports.interfaces.rest.dto.DailyAggregateResponse;
import com.yanki.reports.interfaces.rest.dto.ProductEventResponse;
import com.yanki.reports.interfaces.rest.dto.ProductGroupResponse;
import com.yanki.reports.interfaces.rest.dto.ProductSummaryResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private GenerateAggregatedReportUseCase generateAggregatedReportUseCase;
    private GetProductEventsByDateRangeUseCase getProductEventsByDateRangeUseCase;
    private GetCustomerReportUseCase getCustomerReportUseCase;
    private GetCardMovementsUseCase getCardMovementsUseCase;
    private GetCustomerSummaryUseCase getCustomerSummaryUseCase;
    private GetCustomer360UseCase getCustomer360UseCase;
    private GetProductSummaryUseCase getProductSummaryUseCase;
    private GetDailyAggregatesUseCase getDailyAggregatesUseCase;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        generateAggregatedReportUseCase = Mockito.mock(GenerateAggregatedReportUseCase.class);
        getProductEventsByDateRangeUseCase = Mockito.mock(GetProductEventsByDateRangeUseCase.class);
        getCustomerReportUseCase = Mockito.mock(GetCustomerReportUseCase.class);
        getCardMovementsUseCase = Mockito.mock(GetCardMovementsUseCase.class);
        getCustomerSummaryUseCase = Mockito.mock(GetCustomerSummaryUseCase.class);
        getCustomer360UseCase = Mockito.mock(GetCustomer360UseCase.class);
        getProductSummaryUseCase = Mockito.mock(GetProductSummaryUseCase.class);
        getDailyAggregatesUseCase = Mockito.mock(GetDailyAggregatesUseCase.class);

        ReportController controller = new ReportController(
                generateAggregatedReportUseCase,
                getProductEventsByDateRangeUseCase,
                getCustomerReportUseCase,
                getCardMovementsUseCase,
                getCustomerSummaryUseCase,
                getCustomer360UseCase,
                getProductSummaryUseCase,
                getDailyAggregatesUseCase
        );
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void testGetAggregatedReport_Success() {
        AggregatedReport domain = AggregatedReport.builder()
                .count(2)
                .sumAmount(BigDecimal.valueOf(6000))
                .averageAmount(BigDecimal.valueOf(3000))
                .type("ACCOUNT")
                .from(LocalDate.of(2026, 1, 1))
                .to(LocalDate.of(2026, 12, 31))
                .customerType("NATURAL")
                .build();

        when(generateAggregatedReportUseCase.execute(anyString(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(Single.just(domain));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/aggregated")
                        .queryParam("type", "ACCOUNT")
                        .queryParam("from", "2026-01-01")
                        .queryParam("to", "2026-12-31")
                        .queryParam("customerType", "NATURAL")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2)
                .jsonPath("$.sumAmount").isEqualTo(6000)
                .jsonPath("$.averageAmount").isEqualTo(3000)
                .jsonPath("$.type").isEqualTo("ACCOUNT")
                .jsonPath("$.dateRange.from[0]").isEqualTo(2026)
                .jsonPath("$.dateRange.from[1]").isEqualTo(1)
                .jsonPath("$.dateRange.from[2]").isEqualTo(1)
                .jsonPath("$.dateRange.to[0]").isEqualTo(2026)
                .jsonPath("$.dateRange.to[1]").isEqualTo(12)
                .jsonPath("$.dateRange.to[2]").isEqualTo(31)
                .jsonPath("$.customerType").isEqualTo("NATURAL");
    }

    @Test
    void testGetProducts_Success() {
        ProductEvent domain = ProductEvent.builder()
                .id("event1")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("acc123")
                .customerId("cust1")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .amount(BigDecimal.valueOf(1000))
                .balanceAfter(BigDecimal.valueOf(1000))
                .occurredAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .ingestedAt(LocalDateTime.of(2026, 6, 15, 10, 0, 0))
                .build();

        when(getProductEventsByDateRangeUseCase.execute(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Flowable.just(domain));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/products")
                        .queryParam("from", "2026-01-01")
                        .queryParam("to", "2026-12-31")
                        .build())
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
                .expectBodyList(ProductEventResponse.class)
                .hasSize(1)
                .consumeWith(result -> {
                    var list = result.getResponseBody();
                    assert list != null && list.size() == 1;
                    var item = list.get(0);
                    assert "event1".equals(item.getId());
                    assert "ACCOUNT_OPENED".equals(item.getEventType());
                    assert "acc123".equals(item.getAggregateId());
                    assert "cust1".equals(item.getCustomerId());
                    assert "ACCOUNT".equals(item.getProductType());
                    assert "SAVINGS".equals(item.getSubType());
                    assert BigDecimal.valueOf(1000).compareTo(item.getAmount()) == 0;
                });
    }

    @Test
    void testGetCustomerReport_Success() {
        ProductEvent event1 = ProductEvent.builder()
                .id("event1")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("acc123")
                .customerId("6af39f59b089e973a5ffe9b6")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .amount(BigDecimal.valueOf(1000))
                .balanceAfter(BigDecimal.valueOf(1000))
                .occurredAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .ingestedAt(LocalDateTime.of(2026, 6, 15, 10, 0, 0))
                .build();

        ProductEvent event2 = ProductEvent.builder()
                .id("event2")
                .eventType("CARD_OPENED")
                .aggregateId("card456")
                .customerId("6af39f59b089e973a5ffe9b6")
                .productType("CARD")
                .subType("CREDIT")
                .amount(BigDecimal.valueOf(500))
                .balanceAfter(BigDecimal.valueOf(500))
                .occurredAt(LocalDateTime.of(2026, 7, 20, 14, 0))
                .ingestedAt(LocalDateTime.of(2026, 7, 20, 14, 0, 0))
                .build();

        CustomerReport domain = CustomerReport.builder()
                .customerId("6af39f59b089e973a5ffe9b6")
                .products(List.of(
                        ProductGroup.builder()
                                .aggregateId("acc123")
                                .productType("ACCOUNT")
                                .subType("SAVINGS")
                                .events(List.of(event1))
                                .build(),
                        ProductGroup.builder()
                                .aggregateId("card456")
                                .productType("CARD")
                                .subType("CREDIT")
                                .events(List.of(event2))
                                .build()
                ))
                .build();

        when(getCustomerReportUseCase.execute("6af39f59b089e973a5ffe9b6"))
                .thenReturn(Single.just(domain));

        webTestClient.get()
                .uri("/api/reports/customers/6af39f59b089e973a5ffe9b6")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.customerId").isEqualTo("6af39f59b089e973a5ffe9b6")
                .jsonPath("$.products.length()").isEqualTo(2)
                .jsonPath("$.products[0].aggregateId").isEqualTo("acc123")
                .jsonPath("$.products[0].productType").isEqualTo("ACCOUNT")
                .jsonPath("$.products[0].subType").isEqualTo("SAVINGS")
                .jsonPath("$.products[0].events.length()").isEqualTo(1)
                .jsonPath("$.products[1].aggregateId").isEqualTo("card456")
                .jsonPath("$.products[1].productType").isEqualTo("CARD")
                .jsonPath("$.products[1].subType").isEqualTo("CREDIT");
    }

    @Test
    void testGetCardMovements_Success() {
        ProductEvent movement1 = ProductEvent.builder()
                .id("mov1")
                .eventType("CARD_CONSUMPTION")
                .aggregateId("card789")
                .customerId("cust1")
                .productType("CARD")
                .subType("CREDIT")
                .amount(BigDecimal.valueOf(150))
                .balanceAfter(BigDecimal.valueOf(4850))
                .occurredAt(LocalDateTime.of(2026, 11, 10, 12, 0))
                .ingestedAt(LocalDateTime.of(2026, 11, 10, 12, 0, 0))
                .build();

        ProductEvent movement2 = ProductEvent.builder()
                .id("mov2")
                .eventType("CARD_CONSUMPTION")
                .aggregateId("card789")
                .customerId("cust1")
                .productType("CARD")
                .subType("CREDIT")
                .amount(BigDecimal.valueOf(200))
                .balanceAfter(BigDecimal.valueOf(5050))
                .occurredAt(LocalDateTime.of(2026, 11, 9, 15, 0))
                .ingestedAt(LocalDateTime.of(2026, 11, 9, 15, 0, 0))
                .build();

        CardMovementsReport domain = CardMovementsReport.builder()
                .cardId("card789")
                .productType("CARD")
                .subType("CREDIT")
                .movements(List.of(movement1, movement2))
                .build();

        when(getCardMovementsUseCase.execute("card789", 10))
                .thenReturn(Single.just(domain));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/movements/cards/card789")
                        .queryParam("limit", 10)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.cardId").isEqualTo("card789")
                .jsonPath("$.productType").isEqualTo("CARD")
                .jsonPath("$.subType").isEqualTo("CREDIT")
                .jsonPath("$.movements.length()").isEqualTo(2)
                .jsonPath("$.movements[0].eventType").isEqualTo("CARD_CONSUMPTION")
                .jsonPath("$.movements[0].amount").isEqualTo(150);
    }

    // ---------------------- Customer Summary (CP-123) ----------------------

    @Test
    void testGetCustomerSummary_Success() {
        CustomerSummary domain = CustomerSummary.builder()
                .customerId("6af39f59b089e973a5ffe9b6")
                .customerType("NATURAL")
                .profile("PERSONAL")
                .status("ACTIVE")
                .totalAccounts(3)
                .totalCards(2)
                .totalCredits(1)
                .totalWallets(1)
                .totalBalanceAccounts(BigDecimal.valueOf(15000.50))
                .totalCreditLimit(BigDecimal.valueOf(50000.00))
                .totalCreditDebt(BigDecimal.valueOf(12000.00))
                .totalWalletBalance(BigDecimal.valueOf(500.00))
                .hasOverdueDebt(false)
                .lastMovementAt(LocalDateTime.of(2026, 6, 15, 10, 30))
                .updatedAt(LocalDateTime.of(2026, 6, 15, 10, 30, 5))
                .build();

        when(getCustomerSummaryUseCase.execute("6af39f59b089e973a5ffe9b6"))
                .thenReturn(Maybe.just(domain));

        webTestClient.get()
                .uri("/api/reports/customer-summary/6af39f59b089e973a5ffe9b6")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.customerId").isEqualTo("6af39f59b089e973a5ffe9b6")
                .jsonPath("$.customerType").isEqualTo("NATURAL")
                .jsonPath("$.profile").isEqualTo("PERSONAL")
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.totalAccounts").isEqualTo(3)
                .jsonPath("$.totalCards").isEqualTo(2)
                .jsonPath("$.totalCredits").isEqualTo(1)
                .jsonPath("$.totalWallets").isEqualTo(1)
                .jsonPath("$.totalBalanceAccounts").isEqualTo(15000.50)
                .jsonPath("$.hasOverdueDebt").isEqualTo(false);
    }

    @Test
    void testGetCustomerSummary_NotFound() {
        when(getCustomerSummaryUseCase.execute("nonexistent"))
                .thenReturn(Maybe.empty());

        // maybeToMono con Maybe.empty() devuelve Mono.empty() -> HTTP 200 con cuerpo vacio
        webTestClient.get()
                .uri("/api/reports/customer-summary/nonexistent")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    // ---------------------- Product Summary (CP-123) ----------------------

    @Test
    void testGetProductSummary_Success() {
        ProductSummary domain = ProductSummary.builder()
                .productId("ACC-12345")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .customerId("6af39f59b089e973a5ffe9b6")
                .status("ACTIVE")
                .currentBalance(BigDecimal.valueOf(5000.00))
                .currency("PEN")
                .openedAt(LocalDateTime.of(2024, 1, 15, 9, 0))
                .lastMovementAt(LocalDateTime.of(2026, 6, 15, 14, 30))
                .updatedAt(LocalDateTime.of(2026, 6, 15, 14, 30, 5))
                .build();

        when(getProductSummaryUseCase.executeByProductId("ACC-12345"))
                .thenReturn(Maybe.just(domain));

        webTestClient.get()
                .uri("/api/reports/products/ACC-12345")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.productId").isEqualTo("ACC-12345")
                .jsonPath("$.productType").isEqualTo("ACCOUNT")
                .jsonPath("$.subType").isEqualTo("SAVINGS")
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.currentBalance").isEqualTo(5000.00)
                .jsonPath("$.currency").isEqualTo("PEN");
    }

    @Test
    void testGetProductSummary_NotFound() {
        when(getProductSummaryUseCase.executeByProductId("nonexistent"))
                .thenReturn(Maybe.empty());

        // maybeToMono con Maybe.empty() devuelve Mono.empty() -> HTTP 200 con cuerpo vacio
        webTestClient.get()
                .uri("/api/reports/products/nonexistent")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    // ---------------------- Daily Aggregates (CP-123) ----------------------

    @Test
    void testGetDailyAggregates_Success() {
        DailyAggregate domain = DailyAggregate.builder()
                .id("2026-06-15:ACCOUNT:NATURAL")
                .date(LocalDate.of(2026, 6, 15))
                .productType("ACCOUNT")
                .customerType("NATURAL")
                .movementCount(120)
                .totalAmount(BigDecimal.valueOf(24000.50))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(45)
                .uniqueProducts(30)
                .updatedAt(LocalDateTime.of(2026, 6, 15, 23, 59))
                .build();

        when(getDailyAggregatesUseCase.executeByDate(LocalDate.of(2026, 6, 15)))
                .thenReturn(Flowable.just(domain));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/daily-aggregates")
                        .queryParam("date", "2026-06-15")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DailyAggregateResponse.class)
                .hasSize(1)
                .consumeWith(result -> {
                    var list = result.getResponseBody();
                    assert list != null && list.size() == 1;
                    var item = list.get(0);
                    assert "2026-06-15:ACCOUNT:NATURAL".equals(item.getId());
                    assert "ACCOUNT".equals(item.getProductType());
                    assert Integer.valueOf(120).equals(item.getMovementCount());
                    assert BigDecimal.valueOf(24000.50).compareTo(item.getTotalAmount()) == 0;
                });
    }

    @Test
    void testGetDailyAggregates_WithProductType() {
        // Nota: el controlador actualmente llama a executeByDate(date) ignorando productType.
        // Este test verifica el comportamiento actual: se ignora el parametro productType.
        DailyAggregate domain = DailyAggregate.builder()
                .id("2026-06-15:CARD:NATURAL")
                .date(LocalDate.of(2026, 6, 15))
                .productType("CARD")
                .customerType("NATURAL")
                .movementCount(80)
                .totalAmount(BigDecimal.valueOf(16000.00))
                .avgAmount(BigDecimal.valueOf(200.00))
                .uniqueCustomers(30)
                .uniqueProducts(20)
                .updatedAt(LocalDateTime.of(2026, 6, 15, 23, 59))
                .build();

        when(getDailyAggregatesUseCase.executeByDate(LocalDate.of(2026, 6, 15)))
                .thenReturn(Flowable.just(domain));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/daily-aggregates")
                        .queryParam("date", "2026-06-15")
                        .queryParam("productType", "CARD")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DailyAggregateResponse.class)
                .hasSize(1)
                .consumeWith(result -> {
                    var list = result.getResponseBody();
                    assert list != null && list.size() == 1;
                    assert "CARD".equals(list.get(0).getProductType());
                });
    }

    @Test
    void testGetDailyAggregates_Empty() {
        when(getDailyAggregatesUseCase.executeByDate(LocalDate.of(2020, 1, 1)))
                .thenReturn(Flowable.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/daily-aggregates")
                        .queryParam("date", "2020-01-01")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DailyAggregateResponse.class)
                .hasSize(0);
    }

    // ---------------------- CP-124: Rango de fechas inválido ----------------------

    @Test
    void testGetAggregatedReport_FromAfterTo() {
        // Nota: el controlador no tiene GlobalExceptionHandler para InvalidDateRangeException.
        // La excepcion se traduce a HTTP 500 (comportamiento actual).
        // Se requiere un GlobalExceptionHandler para mapear a 400.
        when(generateAggregatedReportUseCase.execute(anyString(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(Single.error(new InvalidDateRangeException("Fecha inicial debe ser anterior o igual a fecha final")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/aggregated")
                        .queryParam("type", "ACCOUNT")
                        .queryParam("from", "2026-12-31")
                        .queryParam("to", "2026-01-01")
                        .queryParam("customerType", "NATURAL")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testGetProducts_FromAfterTo() {
        // Nota: el controlador no tiene GlobalExceptionHandler para InvalidDateRangeException.
        // La excepcion se traduce a HTTP 500 (comportamiento actual).
        when(getProductEventsByDateRangeUseCase.execute(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Flowable.error(new InvalidDateRangeException("Fecha inicial debe ser anterior o igual a fecha final")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/products")
                        .queryParam("from", "2026-12-31")
                        .queryParam("to", "2026-01-01")
                        .build())
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    // ---------------------- CP-125: Reporte sin datos ----------------------

    @Test
    void testGetAggregatedReport_NoData() {
        AggregatedReport empty = AggregatedReport.builder()
                .count(0)
                .sumAmount(BigDecimal.ZERO)
                .averageAmount(BigDecimal.ZERO)
                .type("ACCOUNT")
                .from(LocalDate.of(2020, 1, 1))
                .to(LocalDate.of(2020, 12, 31))
                .customerType("NATURAL")
                .build();

        when(generateAggregatedReportUseCase.execute(anyString(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(Single.just(empty));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/aggregated")
                        .queryParam("type", "ACCOUNT")
                        .queryParam("from", "2020-01-01")
                        .queryParam("to", "2020-12-31")
                        .queryParam("customerType", "NATURAL")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(0)
                .jsonPath("$.sumAmount").isEqualTo(0)
                .jsonPath("$.averageAmount").isEqualTo(0);
    }

    @Test
    void testGetProducts_EmptyStream() {
        when(getProductEventsByDateRangeUseCase.execute(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Flowable.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/products")
                        .queryParam("from", "2020-01-01")
                        .queryParam("to", "2020-01-31")
                        .build())
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductEventResponse.class)
                .hasSize(0);
    }

    @Test
    void testGetCustomerReport_NoEvents() {
        CustomerReport empty = CustomerReport.builder()
                .customerId("CLIENTE_SIN_EVENTOS_123")
                .products(List.of())
                .build();

        when(getCustomerReportUseCase.execute("CLIENTE_SIN_EVENTOS_123"))
                .thenReturn(Single.just(empty));

        webTestClient.get()
                .uri("/api/reports/customers/CLIENTE_SIN_EVENTOS_123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.customerId").isEqualTo("CLIENTE_SIN_EVENTOS_123")
                .jsonPath("$.products.length()").isEqualTo(0);
    }

    @Test
    void testGetCardMovements_NotFound() {
        // Nota: el controlador no tiene GlobalExceptionHandler para ProductNotFoundException.
        // La excepcion se traduce a HTTP 500 (comportamiento actual).
        // Se requiere un GlobalExceptionHandler para mapear a 404.
        when(getCardMovementsUseCase.execute("CARD_INEXISTENTE_999", 10))
                .thenReturn(Single.error(new ProductNotFoundException("cardId", "CARD_INEXISTENTE_999")));

        webTestClient.get()
                .uri("/api/reports/movements/cards/CARD_INEXISTENTE_999")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}