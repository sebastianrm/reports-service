package com.yanki.reports.domain.service;

import com.yanki.reports.domain.exception.KafkaLagHighException;
import com.yanki.reports.domain.model.CustomerReport;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductGroup;
import com.yanki.reports.domain.port.in.GetCustomerReportUseCase;
import com.yanki.reports.domain.port.out.CachePort;
import com.yanki.reports.domain.port.out.KafkaLagPort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso para obtener reporte completo de cliente (Vista 360°).
 *
 * <p>Agrega todos los productos asociados a un cliente con sus eventos,
 * organizados por aggregateId + productType + subType.
 * Retorna products = [] si el cliente no tiene eventos (no 404).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetCustomerReportService implements GetCustomerReportUseCase {

    private final ProductEventPersistencePort persistencePort;
    private final CachePort cachePort;
    private final KafkaLagPort kafkaLagPort;

    private static final long LAG_THRESHOLD = 1000L;

    @Override
    public Single<CustomerReport> execute(String customerId) {
        log.info("Ejecutando reporte de cliente (Vista 360°): customerId={}", customerId);

        try {
            // Verificar lag de Kafka
            return kafkaLagPort.isLagHigh(LAG_THRESHOLD)
                    .flatMap(isHigh -> {
                        if (isHigh) {
                            log.warn("Lag de Kafka alto, superando umbral de {} eventos. Reporte de cliente {} rechazado.",
                                    LAG_THRESHOLD, customerId);
                            return Single.error(new KafkaLagHighException(LAG_THRESHOLD, LAG_THRESHOLD));
                        }
                        log.debug("Lag de Kafka dentro de límites. Procediendo con reporte de cliente {}.", customerId);
                        return getCustomerReport(customerId);
                    });
        } catch (Exception ex) {
            log.error("Error inesperado en execute() de reporte de cliente: customerId={}", customerId, ex);
            throw ex;
        }
    }

    private Single<CustomerReport> getCustomerReport(String customerId) {
        String cacheKey = "yanki:reports:cache:customer:" + customerId;
        log.debug("Buscando reporte de cliente en caché: key={}", cacheKey);

        return cachePort.get(cacheKey, CustomerReport.class)
                .doOnSuccess(cached -> log.info("Cache HIT para reporte de cliente: customerId={}", customerId))
                .switchIfEmpty(queryAndCache(customerId, cacheKey))
                .doOnSuccess(report -> {
                    log.info("Reporte de cliente generado exitosamente: customerId={}, products={}, totalEvents={}",
                            customerId, report.getProducts().size(),
                            report.getProducts().stream().mapToInt(p -> p.getEvents().size()).sum());
                })
                .doOnError(e -> log.error("Error generando reporte de cliente: customerId={}, error={}",
                        customerId, e.getMessage(), e));
    }

    private Single<CustomerReport> queryAndCache(String customerId, String cacheKey) {
        log.info("Cache MISS para reporte de cliente. Consultando MongoDB: customerId={}", customerId);

        return persistencePort.findByCustomerIdOrderByOccurredAtDesc(customerId)
                .toList()
                .map(events -> {
                    log.debug("MongoDB retornó {} eventos para cliente {}", events.size(), customerId);
                    // Filtrar eventos de tipo CUSTOMER y agrupar por aggregateId
                    Map<String, List<ProductEvent>> groupedByProduct = events.stream()
                            .filter(event -> event.getAggregateId() != null
                                    && !"CUSTOMER".equalsIgnoreCase(event.getProductType()))
                            .collect(Collectors.groupingBy(
                                    ProductEvent::getAggregateId,
                                    Collectors.collectingAndThen(
                                            Collectors.toList(),
                                            list -> {
                                                // Asegurar productType y subType consistentes
                                                ProductEvent first = list.get(0);
                                                list.forEach(e -> {
                                                    if (e.getProductType() == null) {
                                                        e.setProductType(first.getProductType());
                                                    }
                                                    if (e.getSubType() == null) {
                                                        e.setSubType(first.getSubType());
                                                    }
                                                });
                                                // Ordenar eventos por occurredAt descendente dentro del producto
                                                list.sort(Comparator.comparing(ProductEvent::getOccurredAt).reversed());
                                                return list;
                                            }
                                    )
                            ));

                    List<ProductGroup> products = groupedByProduct.entrySet().stream()
                            .map(entry -> ProductGroup.builder()
                                    .aggregateId(entry.getKey())
                                    .productType(entry.getValue().get(0).getProductType())
                                    .subType(entry.getValue().get(0).getSubType())
                                    .events(entry.getValue())
                                    .build())
                            .collect(Collectors.toList());

                    log.debug("Cliente {} agrupado en {} productos", customerId, products.size());
                    return CustomerReport.builder()
                            .customerId(customerId)
                            .products(products)
                            .build();
                })
                .flatMap(report -> {
                    log.debug("Cachear reporte de cliente en Redis: key={}, TTL=5min", cacheKey);
                    return cachePort.put(cacheKey, report, Duration.ofMinutes(5))
                            .flatMap(v -> {
                                log.debug("Reporte de cliente cacheado exitosamente: key={}", cacheKey);
                                return Single.just(report);
                            });
                });
    }
}