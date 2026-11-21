package com.yanki.reports.infrastructure.persistence.adapter;

import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.domain.port.out.DailyAggregatePersistencePort;
import com.yanki.reports.infrastructure.persistence.document.DailyAggregateDocument;
import com.yanki.reports.infrastructure.persistence.mapper.DailyAggregateDocumentMapper;
import com.yanki.reports.infrastructure.persistence.repository.SpringDataDailyAggregateRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

import java.time.LocalDate;

/**
 * Adaptador de salida que implementa la persistencia de agregados diarios sobre MongoDB Reactivo.
 *
 * <p>Convierte entre tipos Reactor ({@code Mono}, {@code Flux}) de Spring Data
 * y tipos RxJava 3 ({@code Single}, {@code Maybe}, {@code Flowable}) del dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAggregatePersistenceAdapter implements DailyAggregatePersistencePort {

    private final SpringDataDailyAggregateRepository repository;

    @Override
    public Single<DailyAggregate> save(DailyAggregate aggregate) {
        log.debug("Guardando agregado diario en MongoDB: id={}", aggregate.getId());
        DailyAggregateDocument document = DailyAggregateDocumentMapper.toDocument(aggregate);
        return RxJava3Adapter.monoToSingle(
                repository.save(document)
                        .map(DailyAggregateDocumentMapper::toDomain)
                        .doOnSuccess(saved -> log.info("Agregado diario guardado en MongoDB: id={}", saved.getId()))
                        .doOnError(e -> log.error("Error guardando agregado diario en MongoDB: id={}, error={}",
                                aggregate.getId(), e.getMessage(), e))
        );
    }

    @Override
    public Maybe<DailyAggregate> findByDateAndProductType(LocalDate date, String productType) {
        log.debug("Buscando agregado diario en MongoDB: date={}, productType={}", date, productType);
        return RxJava3Adapter.monoToMaybe(
                repository.findByDateAndProductType(date, productType)
                        .map(DailyAggregateDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando agregado diario en MongoDB: date={}, productType={}, error={}",
                                date, productType, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<DailyAggregate> findByDate(LocalDate date) {
        log.debug("Buscando agregados diarios por fecha en MongoDB: date={}", date);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByDate(date)
                        .map(DailyAggregateDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando agregados diarios por fecha en MongoDB: date={}, error={}",
                                date, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<DailyAggregate> findByDateAndCustomerType(LocalDate date, String customerType) {
        log.debug("Buscando agregados diarios por fecha y customerType en MongoDB: date={}, customerType={}",
                date, customerType);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByDateAndCustomerType(date, customerType)
                        .map(DailyAggregateDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando agregados por fecha y customerType en MongoDB: date={}, customerType={}, error={}",
                                date, customerType, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<DailyAggregate> findByDateBetweenAndProductTypeOrderByDateDesc(
            LocalDate from, LocalDate to, String productType) {
        log.debug("Buscando agregados diarios por rango y tipo en MongoDB: from={}, to={}, productType={}",
                from, to, productType);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByDateBetweenAndProductTypeOrderByDateDesc(from, to, productType)
                        .map(DailyAggregateDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando agregados por rango y tipo en MongoDB: from={}, to={}, productType={}, error={}",
                                from, to, productType, e.getMessage(), e))
        );
    }
}
