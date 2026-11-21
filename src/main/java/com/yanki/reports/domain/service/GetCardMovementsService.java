package com.yanki.reports.domain.service;

import com.yanki.reports.domain.exception.KafkaLagHighException;
import com.yanki.reports.domain.exception.ProductNotFoundException;
import com.yanki.reports.domain.model.CardMovementsReport;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.in.GetCardMovementsUseCase;
import com.yanki.reports.domain.port.out.CachePort;
import com.yanki.reports.domain.port.out.KafkaLagPort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso para obtener últimos movimientos de tarjeta.
 *
 * <p><b>Requerimiento obligatorio Parte II:</b> "Implementar un reporte con los últimos 10 movimientos
 * de la tarjeta de débito y de crédito".
 *
 * <p>Consulta optimizada por aggregateId + productType=CARD, ordenado por occurredAt DESC,
 * limitado a N movimientos (default 10, máx 100).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetCardMovementsService implements GetCardMovementsUseCase {

    private final ProductEventPersistencePort persistencePort;
    private final CachePort cachePort;
    private final KafkaLagPort kafkaLagPort;

    private static final long LAG_THRESHOLD = 1000L;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    @Override
    public Single<CardMovementsReport> execute(String cardId, int limit) {
        log.info("Ejecutando consulta de movimientos de tarjeta: cardId={}, limit={}", cardId, limit);

        try {
            // Validar y normalizar limit
            int validatedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
            if (validatedLimit != limit) {
                log.warn("Limit ajustado para tarjeta {}: de {} a {} (rango válido: 1-{})",
                        cardId, limit, validatedLimit, MAX_LIMIT);
            }

            // Verificar lag de Kafka
            return kafkaLagPort.isLagHigh(LAG_THRESHOLD)
                    .flatMap(isHigh -> {
                        if (isHigh) {
                            log.warn("Lag de Kafka alto, superando umbral de {} eventos. Consulta de tarjeta {} rechazada.",
                                    LAG_THRESHOLD, cardId);
                            return Single.error(new KafkaLagHighException(LAG_THRESHOLD, LAG_THRESHOLD));
                        }
                        log.debug("Lag de Kafka dentro de límites. Procediendo con consulta de tarjeta {}.", cardId);
                        return getCardMovements(cardId, validatedLimit);
                    });
        } catch (Exception ex) {
            log.error("Error inesperado en execute() de movimientos de tarjeta: cardId={}, limit={}",
                    cardId, limit, ex);
            throw ex;
        }
    }

    private Single<CardMovementsReport> getCardMovements(String cardId, int limit) {
        String cacheKey = "yanki:reports:cache:card_movements:" + cardId + ":" + limit;
        log.debug("Buscando movimientos de tarjeta en caché: key={}", cacheKey);

        return cachePort.get(cacheKey, CardMovementsReport.class)
                .doOnSuccess(cached -> log.info("Cache HIT para movimientos de tarjeta: cardId={}", cardId))
                .switchIfEmpty(queryAndCache(cardId, limit, cacheKey))
                .doOnSuccess(report -> {
                    log.info("Movimientos de tarjeta obtenidos exitosamente: cardId={}, count={}",
                            cardId, report.getMovements().size());
                })
                .doOnError(e -> log.error("Error obteniendo movimientos de tarjeta: cardId={}, error={}",
                        cardId, e.getMessage(), e));
    }

    private Single<CardMovementsReport> queryAndCache(String cardId, int limit, String cacheKey) {
        log.info("Cache MISS para movimientos de tarjeta. Consultando MongoDB: cardId={}, limit={}", cardId, limit);

        return persistencePort.existsByAggregateId(cardId)
                .flatMap(exists -> {
                    if (!exists) {
                        log.warn("Tarjeta no encontrada en la proyección: cardId={}", cardId);
                        return Single.error(new ProductNotFoundException("cardId", cardId));
                    }
                    log.debug("Tarjeta {} existe. Consultando últimos {} movimientos.", cardId, limit);
                    return persistencePort.findByAggregateIdOrderByOccurredAtDesc(cardId)
                            .take(limit)
                            .toList()
                            .map(events -> {
                                log.debug("MongoDB retornó {} eventos para tarjeta {}", events.size(), cardId);
                                String productType = events.isEmpty() ? "CARD" : events.get(0).getProductType();
                                String subType = events.isEmpty() ? "CREDIT" : events.get(0).getSubType();

                                return CardMovementsReport.builder()
                                        .cardId(cardId)
                                        .productType(productType)
                                        .subType(subType)
                                        .movements(events)
                                        .build();
                            });
                })
                .flatMap(report -> {
                    log.debug("Cachear movimientos de tarjeta en Redis: key={}, TTL=2min", cacheKey);
                    return cachePort.put(cacheKey, report, Duration.ofMinutes(2))
                            .flatMap(v -> {
                                log.debug("Movimientos de tarjeta cacheados exitosamente: key={}", cacheKey);
                                return Single.just(report);
                            });
                });
    }
}