package com.yanki.reports.domain.service;

import com.yanki.reports.domain.exception.InvalidDateRangeException;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.in.GetProductEventsByDateRangeUseCase;
import com.yanki.reports.domain.port.out.KafkaLagPort;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import io.reactivex.rxjava3.core.Flowable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso para obtener eventos de producto por rango de fechas.
 *
 * <p>Retorna un Flowable (streaming) para evitar cargar todos los eventos en memoria.
 * No usa caché por ser datos potencialmente masivos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProductEventsByDateRangeService implements GetProductEventsByDateRangeUseCase {

    private final ProductEventPersistencePort persistencePort;
    private final KafkaLagPort kafkaLagPort;

    private static final long LAG_THRESHOLD = 1000L;

    @Override
    public Flowable<ProductEvent> execute(LocalDate from, LocalDate to) {
        log.info("Ejecutando consulta de eventos por rango de fechas: from={}, to={}", from, to);

        try {
            // Validar rango de fechas
            if (from.isAfter(to)) {
                log.warn("Rango de fechas inválido: from={} es posterior a to={}", from, to);
                return Flowable.error(new InvalidDateRangeException(
                        "Fecha inicial (from) debe ser anterior o igual a fecha final (to)"));
            }

            // Verificar lag de Kafka
            return Flowable.defer(() ->
                    kafkaLagPort.isLagHigh(LAG_THRESHOLD)
                            .flatMapPublisher(isHigh -> {
                                if (isHigh) {
                                    log.warn("Lag de Kafka alto, superando umbral de {} eventos. Consulta de eventos rechazada.",
                                            LAG_THRESHOLD);
                                    return Flowable.error(new com.yanki.reports.domain.exception.KafkaLagHighException(
                                            LAG_THRESHOLD, LAG_THRESHOLD));
                                }
                                log.debug("Lag de Kafka dentro de límites. Procediendo con consulta de eventos por rango.");
                                LocalDateTime fromDateTime = from.atStartOfDay();
                                LocalDateTime toDateTime = to.atTime(23, 59, 59);

                                log.info("Consultando MongoDB para eventos entre {} y {}", fromDateTime, toDateTime);
                                return persistencePort.findByOccurredAtBetweenOrderByOccurredAtDesc(
                                        fromDateTime, toDateTime);
                            })
                            .doOnSubscribe(s -> log.debug("Suscripción activa para streaming de eventos: {} - {}", from, to))
                            .doOnComplete(() -> log.info("Streaming de eventos completado: from={}, to={}", from, to))
                            .doOnError(e -> log.error("Error consultando eventos por rango: from={}, to={}, error={}",
                                    from, to, e.getMessage(), e))
            );
        } catch (Exception ex) {
            log.error("Error inesperado en execute() de eventos por rango: from={}, to={}", from, to, ex);
            throw ex;
        }
    }
}