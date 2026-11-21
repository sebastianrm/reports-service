package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.ProductEvent;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada (Use Case) para ingesta de eventos desde Kafka.
 *
 * <p>Invocado por el consumidor Kafka ({@link com.yanki.reports.infrastructure.messaging.consumer.ReportsEventConsumer})
 * después de validar idempotencia. Persiste la proyección en MongoDB.
 */
public interface IngestEventUseCase {

    /**
     * Ingiere un evento de producto en la proyección analítica.
     *
     * @param event evento a persistir
     * @return evento persistido
     */
    Single<ProductEvent> execute(ProductEvent event);
}