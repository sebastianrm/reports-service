package com.yanki.reports.domain.port.out;

import io.reactivex.rxjava3.core.Single;
import java.util.Map;

/**
 * Puerto de salida para monitoreo de lag de consumidores Kafka.
 *
 * <p>Permite verificar el lag actual para decidir si el servicio debe
 * responder 503 (KAFKA_LAG_HIGH) en modo degradado.
 */
public interface KafkaLagPort {

    /**
     * Obtiene el lag máximo actual entre todos los topics suscritos.
     *
     * @return {@link Single} con el lag máximo
     */
    Single<Long> getMaxLag();

    /**
     * Obtiene el lag por topic.
     *
     * @return {@link Single} con mapa topic -> lag
     */
    Single<Map<String, Long>> getLagByTopic();

    /**
     * Verifica si el lag supera el umbral configurado.
     *
     * @param threshold umbral de lag para degradación
     * @return true si lag > threshold
     */
    Single<Boolean> isLagHigh(long threshold);
}