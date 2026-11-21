package com.yanki.reports.domain.service;

import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.domain.port.in.GetDailyAggregatesUseCase;
import com.yanki.reports.domain.port.out.DailyAggregatePersistencePort;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Implementación del caso de uso para obtener agregados diarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetDailyAggregatesService implements GetDailyAggregatesUseCase {

    private final DailyAggregatePersistencePort persistencePort;

    @Override
    public Flowable<DailyAggregate> executeByDate(LocalDate date) {
        log.info("Ejecutando consulta de agregados diarios por fecha: date={}", date);

        try {
            return persistencePort.findByDate(date)
                    .doOnSubscribe(s -> log.debug("Suscripción activa para agregados del día: date={}", date))
                    .doOnComplete(() -> log.info("Consulta de agregados del día completada: date={}", date))
                    .doOnError(e -> log.error("Error obteniendo agregados del día: date={}, error={}",
                            date, e.getMessage(), e));
        } catch (Exception ex) {
            log.error("Error inesperado en executeByDate(): date={}", date, ex);
            throw ex;
        }
    }

    @Override
    public Flowable<DailyAggregate> executeByDateRange(LocalDate from, LocalDate to, String productType) {
        log.info("Ejecutando consulta de agregados diarios por rango: from={}, to={}, productType={}",
                from, to, productType);

        try {
            if (productType != null && !productType.isEmpty()) {
                log.debug("Consultando agregados filtrados por tipo de producto: {}", productType);
                return persistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(from, to, productType)
                        .doOnSubscribe(s -> log.debug("Suscripción activa para agregados: from={}, to={}, type={}", from, to, productType))
                        .doOnComplete(() -> log.info("Consulta de agregados por rango completada: from={}, to={}, type={}",
                                from, to, productType))
                        .doOnError(e -> log.error("Error obteniendo agregados por rango: from={}, to={}, type={}, error={}",
                                from, to, productType, e.getMessage(), e));
            } else {
                log.debug("Consultando agregados de todos los tipos de producto");
                return Flowable.concat(
                                persistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(from, to, "ACCOUNT"),
                                persistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(from, to, "CARD"),
                                persistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(from, to, "CREDIT"),
                                persistencePort.findByDateBetweenAndProductTypeOrderByDateDesc(from, to, "WALLET")
                        )
                        .doOnSubscribe(s -> log.debug("Suscripción activa para todos los tipos: from={}, to={}", from, to))
                        .doOnComplete(() -> log.info("Consulta de agregados de todos los tipos completada: from={}, to={}", from, to))
                        .doOnError(e -> log.error("Error obteniendo agregados de todos los tipos: from={}, to={}, error={}",
                                from, to, e.getMessage(), e));
            }
        } catch (Exception ex) {
            log.error("Error inesperado en executeByDateRange(): from={}, to={}, productType={}",
                    from, to, productType, ex);
            throw ex;
        }
    }
}
