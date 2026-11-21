package com.yanki.reports.infrastructure.messaging.adapter;

import com.yanki.reports.domain.port.out.KafkaLagPort;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador de salida para monitoreo de lag de consumidores Kafka.
 *
 * <p>Usa AdminClient para obtener offsets del consumer group y calcular lag.
 * Permite al dominio decidir si responder 503 (KAFKA_LAG_HIGH).
 *
 * <p>Nota: Implementación simplificada que retorna lag=0 por defecto.
 * Para implementación completa, usar AdminClient con API compatible con la versión de Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaLagAdapter implements KafkaLagPort {

    private final KafkaAdmin kafkaAdmin;

    @Value("${spring.kafka.consumer.group-id:reports-service}")
    private String consumerGroupId;

    @Override
    public Single<Long> getMaxLag() {
        log.debug("Obteniendo lag máximo de Kafka para consumer group: {}", consumerGroupId);
        return getLagByTopic()
                .map(lagByTopic -> {
                    long maxLag = lagByTopic.values().stream()
                            .max(Long::compareTo)
                            .orElse(0L);
                    log.info("Lag máximo de Kafka: maxLag={}, consumerGroup={}", maxLag, consumerGroupId);
                    return maxLag;
                })
                .doOnError(e -> log.error("Error obteniendo lag máximo de Kafka: error={}", e.getMessage(), e));
    }

    @Override
    public Single<Map<String, Long>> getLagByTopic() {
        return Single.fromCallable(() -> {
            try {
                log.debug("Consultando lag por topic para consumer group: {}", consumerGroupId);
                // Implementación simplificada: retorna lag 0
                // TODO: Implementar con AdminClient compatible con versión de Kafka
                Map<String, Long> lagByTopic = new HashMap<>();
                lagByTopic.put("yanki.customer.registered.v1", 0L);
                lagByTopic.put("yanki.account.movement.registered.v1", 0L);
                lagByTopic.put("yanki.card.consumption.registered.v1", 0L);
                lagByTopic.put("yanki.credit.payment.registered.v1", 0L);
                lagByTopic.put("yanki.wallet.transaction.registered.v1", 0L);

                log.debug("Lag por topic (simulado): {}", lagByTopic);
                return lagByTopic;

            } catch (Exception e) {
                log.error("Error obteniendo lag de Kafka para consumer group {}: error={}",
                        consumerGroupId, e.getMessage(), e);
                return Collections.singletonMap("error", Long.MAX_VALUE);
            }
        });
    }

    @Override
    public Single<Boolean> isLagHigh(long threshold) {
        log.debug("Verificando si lag de Kafka supera umbral: threshold={}", threshold);
        return getMaxLag()
                .map(maxLag -> {
                    boolean isHigh = maxLag > threshold;
                    if (isHigh) {
                        log.warn("Lag de Kafka ALTO: maxLag={} supera umbral threshold={}", maxLag, threshold);
                    } else {
                        log.debug("Lag de Kafka dentro de límites: maxLag={} <= threshold={}", maxLag, threshold);
                    }
                    return isHigh;
                })
                .doOnError(e -> log.error("Error verificando lag de Kafka: threshold={}, error={}",
                        threshold, e.getMessage(), e));
    }
}