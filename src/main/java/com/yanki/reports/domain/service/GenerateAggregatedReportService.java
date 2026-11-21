package com.yanki.reports.domain.service;

import com.yanki.reports.domain.exception.InvalidDateRangeException;
import com.yanki.reports.domain.exception.KafkaLagHighException;
import com.yanki.reports.domain.model.AggregatedReport;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.in.GenerateAggregatedReportUseCase;
import com.yanki.reports.domain.port.out.CachePort;
import com.yanki.reports.domain.port.out.KafkaLagPort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso para generar reporte agregado.
 *
 * <p>Orquesta la consulta a persistencia, verifica lag de Kafka, usa caché Redis
 * y aplica la lógica de agregación de métricas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateAggregatedReportService implements GenerateAggregatedReportUseCase {

    private final ProductEventPersistencePort persistencePort;
    private final CachePort cachePort;
    private final KafkaLagPort kafkaLagPort;

    private static final long LAG_THRESHOLD = 1000L;

    @Override
    public Single<AggregatedReport> execute(String productType, LocalDate from, LocalDate to, String customerType) {
        log.info("Ejecutando reporte agregado: productType={}, from={}, to={}, customerType={}",
                productType, from, to, customerType);

        try {
            // Validar rango de fechas
            if (from.isAfter(to)) {
                log.warn("Rango de fechas inválido: from={} es posterior a to={}", from, to);
                return Single.error(new InvalidDateRangeException(
                        "Fecha inicial (from) debe ser anterior o igual a fecha final (to)"));
            }

            // Validar productType
            if (!isValidProductType(productType)) {
                log.warn("Tipo de producto no soportado: {}", productType);
                return Single.error(new InvalidDateRangeException(
                        "Tipo de producto no soportado: " + productType + ". Valores: ACCOUNT, CARD, CREDIT, WALLET"));
            }

            // Validar customerType
            if (!isValidCustomerType(customerType)) {
                log.warn("Tipo de cliente no válido: {}", customerType);
                return Single.error(new InvalidDateRangeException(
                        "Tipo de cliente no válido: " + customerType + ". Valores: NATURAL, JURIDICA"));
            }

            // Verificar lag de Kafka antes de consultar
            return kafkaLagPort.isLagHigh(LAG_THRESHOLD)
                    .flatMap(isHigh -> {
                        if (isHigh) {
                            log.warn("Lag de Kafka alto, superando umbral de {} eventos. Reporte agregado rechazado.", LAG_THRESHOLD);
                            return Single.error(new KafkaLagHighException(LAG_THRESHOLD, LAG_THRESHOLD));
                        }
                        log.debug("Lag de Kafka dentro de límites. Procediendo con generación de reporte agregado.");
                        return generateReport(productType, from, to, customerType);
                    });
        } catch (Exception ex) {
            log.error("Error inesperado en execute() del reporte agregado: productType={}, from={}, to={}",
                    productType, from, to, ex);
            throw ex;
        }
    }

    private Single<AggregatedReport> generateReport(String productType, LocalDate from, LocalDate to, String customerType) {
        String cacheKey = buildCacheKey(productType, from, to, customerType);
        log.debug("Generando reporte agregado. Cache key: {}", cacheKey);

        // Intentar obtener del caché
        return cachePort.get(cacheKey, AggregatedReport.class)
                .doOnSuccess(cached -> log.info("Cache HIT para reporte agregado: key={}", cacheKey))
                .switchIfEmpty(
                        // Cache MISS: consultar MongoDB y cachear
                        queryAndCache(productType, from, to, customerType, cacheKey)
                )
                .doOnSuccess(report -> {
                    log.info("Reporte agregado generado exitosamente: type={}, count={}, sumAmount={}, averageAmount={}",
                            productType, report.getCount(), report.getSumAmount(), report.getAverageAmount());
                })
                .doOnError(e -> log.error("Error generando reporte agregado: productType={}, from={}, to={}, error={}",
                        productType, from, to, e.getMessage(), e));
    }

    private Single<AggregatedReport> queryAndCache(String productType, LocalDate from, LocalDate to,
            String customerType, String cacheKey) {
        log.info("Cache MISS para reporte agregado. Consultando MongoDB: key={}", cacheKey);
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59);

        return persistencePort.findByProductTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                        productType, fromDateTime, toDateTime)
                .filter(event -> customerType.equalsIgnoreCase(event.getSubType()))
                .toList()
                .map(events -> {
                    log.debug("MongoDB retornó {} eventos para agregación", events.size());
                    long count = events.size();
                    BigDecimal sum = events.stream()
                            .map(ProductEvent::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal average = count > 0
                            ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    log.debug("Agregación calculada: count={}, sum={}, average={}", count, sum, average);
                    return AggregatedReport.builder()
                            .count(count)
                            .sumAmount(sum)
                            .averageAmount(average)
                            .type(productType)
                            .from(from)
                            .to(to)
                            .customerType(customerType)
                            .build();
                })
                .flatMap(report -> {
                    log.debug("Cachear reporte agregado en Redis: key={}, TTL=5min", cacheKey);
                    return cachePort.put(cacheKey, report, java.time.Duration.ofMinutes(5))
                            .flatMap(v -> {
                                log.debug("Reporte agregado cacheado exitosamente: key={}", cacheKey);
                                return Single.just(report);
                            });
                });
    }

    private String buildCacheKey(String productType, LocalDate from, LocalDate to, String customerType) {
        return String.format("yanki:reports:cache:aggregated:%s:%s:%s:%s",
                productType, from, to, customerType);
    }

    private boolean isValidProductType(String productType) {
        return switch (productType.toUpperCase()) {
            case "ACCOUNT", "CARD", "CREDIT", "WALLET" -> true;
            default -> false;
        };
    }

    private boolean isValidCustomerType(String customerType) {
        return switch (customerType.toUpperCase()) {
            case "NATURAL", "JURIDICA" -> true;
            default -> false;
        };
    }
}