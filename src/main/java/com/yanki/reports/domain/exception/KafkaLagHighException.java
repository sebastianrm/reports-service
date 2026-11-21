package com.yanki.reports.domain.exception;

/**
 * Excepción lanzada cuando el lag del consumidor Kafka supera el umbral operativo.
 *
 * <p>Indica que la proyección de eventos puede estar inconsistente o desactualizada.
 * Se traduce a HTTP 503 con código {@code KAFKA_LAG_HIGH}.
 */
public class KafkaLagHighException extends BusinessException {

    /**
     * Constructor con lag actual y umbral configurado.
     *
     * @param currentLag   lag actual del consumidor
     * @param threshold    umbral configurado para degradación
     */
    public KafkaLagHighException(long currentLag, long threshold) {
        super("KAFKA_LAG_HIGH",
                "Proyección degradada: consumer lag (" + currentLag + ") supera umbral (" + threshold + ")");
    }
}