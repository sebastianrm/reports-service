package com.yanki.reports.infrastructure.persistence.adapter;

import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.out.ProductEventPersistencePort;
import com.yanki.reports.infrastructure.persistence.document.ProductEventDocument;
import com.yanki.reports.infrastructure.persistence.mapper.ProductEventDocumentMapper;
import com.yanki.reports.infrastructure.persistence.repository.SpringDataProductEventRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de salida que implementa la persistencia de eventos de producto sobre MongoDB Reactivo.
 *
 * <p>Convierte entre tipos Reactor ({@code Mono}, {@code Flux}) de Spring Data
 * y tipos RxJava 3 ({@code Single}, {@code Maybe}, {@code Flowable}) del dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPersistenceAdapter implements ProductEventPersistencePort {

    private final SpringDataProductEventRepository repository;

    @Override
    public Single<ProductEvent> save(ProductEvent event) {
        log.debug("Guardando evento en MongoDB: id={}, eventType={}", event.getId(), event.getEventType());
        ProductEventDocument document = ProductEventDocumentMapper.toDocument(event);
        return RxJava3Adapter.monoToSingle(
                repository.save(document)
                        .map(ProductEventDocumentMapper::toDomain)
                        .doOnSuccess(saved -> log.info("Evento guardado en MongoDB: id={}, type={}", saved.getId(), saved.getEventType()))
                        .doOnError(e -> log.error("Error guardando evento en MongoDB: id={}, error={}", event.getId(), e.getMessage(), e))
        );
    }

    @Override
    public Single<List<ProductEvent>> saveAll(List<ProductEvent> events) {
        log.info("Guardando {} eventos en MongoDB (lote)", events.size());
        List<ProductEventDocument> documents = events.stream()
                .map(ProductEventDocumentMapper::toDocument)
                .collect(Collectors.toList());
        return RxJava3Adapter.monoToSingle(
                repository.saveAll(documents)
                        .collectList()
                        .map(savedDocs -> savedDocs.stream()
                                .map(ProductEventDocumentMapper::toDomain)
                                .collect(Collectors.toList()))
                        .doOnSuccess(saved -> log.info("{} eventos guardados en MongoDB (lote)", saved.size()))
                        .doOnError(e -> log.error("Error guardando eventos en lote en MongoDB: count={}, error={}",
                                events.size(), e.getMessage(), e))
        );
    }

    @Override
    public Maybe<ProductEvent> findById(String id) {
        log.debug("Buscando evento por ID en MongoDB: id={}", id);
        return RxJava3Adapter.monoToMaybe(
                repository.findById(id)
                        .map(ProductEventDocumentMapper::toDomain)
                        .doOnSuccess(e -> log.debug("Evento encontrado por ID: id={}", id))
                        .doOnError(err -> log.error("Error buscando evento por ID: id={}, error={}", id, err.getMessage(), err))
        );
    }

    @Override
    public Flowable<ProductEvent> findByAggregateIdOrderByOccurredAtDesc(String aggregateId) {
        log.debug("Buscando eventos por aggregateId en MongoDB: aggregateId={}", aggregateId);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByAggregateIdOrderByOccurredAtDesc(aggregateId)
                        .map(ProductEventDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando eventos por aggregateId: aggregateId={}, error={}",
                                aggregateId, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<ProductEvent> findByProductTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            String productType, LocalDateTime from, LocalDateTime to) {
        log.debug("Buscando eventos por tipo y rango en MongoDB: productType={}, from={}, to={}",
                productType, from, to);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByProductTypeAndOccurredAtBetweenOrderByOccurredAtDesc(productType, from, to)
                        .map(ProductEventDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando eventos por tipo y rango: productType={}, error={}",
                                productType, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<ProductEvent> findByOccurredAtBetweenOrderByOccurredAtDesc(LocalDateTime from, LocalDateTime to) {
        log.debug("Buscando eventos por rango de fechas en MongoDB: from={}, to={}", from, to);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByOccurredAtBetweenOrderByOccurredAtDesc(from, to)
                        .map(ProductEventDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando eventos por rango de fechas: from={}, to={}, error={}",
                                from, to, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<ProductEvent> findByCustomerIdOrderByOccurredAtDesc(String customerId) {
        log.debug("Buscando eventos por customerId en MongoDB: customerId={}", customerId);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByCustomerIdOrderByOccurredAtDesc(customerId)
                        .map(ProductEventDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando eventos por customerId: customerId={}, error={}",
                                customerId, e.getMessage(), e))
        );
    }

    @Override
    public Single<Boolean> existsByAggregateId(String aggregateId) {
        log.debug("Verificando existencia de aggregateId en MongoDB: aggregateId={}", aggregateId);
        return RxJava3Adapter.monoToSingle(
                repository.existsByAggregateId(aggregateId)
                        .doOnSuccess(exists -> log.debug("AggregateId {} existe: {}", aggregateId, exists))
                        .doOnError(e -> log.error("Error verificando existencia de aggregateId: aggregateId={}, error={}",
                                aggregateId, e.getMessage(), e))
        );
    }
}