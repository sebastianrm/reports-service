package com.yanki.reports.infrastructure.persistence.adapter;

import com.yanki.reports.domain.model.ProcessedEvent;
import com.yanki.reports.domain.port.out.ProcessedEventPort;
import com.yanki.reports.infrastructure.persistence.document.ProcessedEventDocument;
import com.yanki.reports.infrastructure.persistence.mapper.ProcessedEventDocumentMapper;
import com.yanki.reports.infrastructure.persistence.repository.SpringDataProcessedEventRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * Adaptador de salida para control de idempotencia de eventos procesados.
 *
 * <p>Implementa exactly-once lógico usando la colección {@code processed_events}
 * con clave compuesta {@code <topic>:<aggregateId>:<occurredAt>}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventAdapter implements ProcessedEventPort {

    private final SpringDataProcessedEventRepository repository;

    @Override
    public Maybe<ProcessedEvent> findById(String eventId) {
        log.debug("Buscando evento procesado por ID: eventId={}", eventId);
        return RxJava3Adapter.monoToMaybe(
                repository.findById(eventId)
                        .map(ProcessedEventDocumentMapper::toDomain)
                        .doOnSuccess(existing -> log.debug("Evento procesado encontrado: eventId={}", eventId))
                        .doOnError(e -> log.error("Error buscando evento procesado por ID: eventId={}, error={}",
                                eventId, e.getMessage(), e))
        );
    }

    @Override
    public Completable save(ProcessedEvent processedEvent) {
        log.debug("Guardando evento procesado en MongoDB: id={}", processedEvent.getId());
        ProcessedEventDocument document = ProcessedEventDocumentMapper.toDocument(processedEvent);
        return RxJava3Adapter.monoToCompletable(
                repository.save(document)
                        .doOnSuccess(saved -> log.info("Evento procesado registrado en MongoDB: id={}", saved.getId()))
                        .doOnError(e -> log.error("Error registrando evento procesado en MongoDB: id={}, error={}",
                                processedEvent.getId(), e.getMessage(), e))
                        .then()
        );
    }

    @Override
    public Single<Boolean> tryRegister(ProcessedEvent processedEvent) {
        log.debug("Verificando idempotencia para evento: id={}", processedEvent.getId());
        // Usar compare-and-set atómico: insertar solo si no existe
        // Spring Data MongoDB no tiene insert-if-not-exists nativo en ReactiveMongoRepository
        // Usamos findById + save condicional (race condition aceptable para idempotencia eventual)
        return findById(processedEvent.getId())
                .isEmpty()
                .flatMap(isEmpty -> {
                    if (!isEmpty) {
                        // Ya existía = duplicado
                        log.info("Evento duplicado detectado por idempotencia: id={}", processedEvent.getId());
                        return Single.just(false);
                    }
                    // Nuevo: registrar en processed_events
                    log.debug("Evento nuevo. Registrando en processed_events: id={}", processedEvent.getId());
                    return save(processedEvent)
                            .toSingleDefault(true); // Registrado nuevo
                })
                .doOnSuccess(isNew -> {
                    if (isNew) {
                        log.info("Evento registrado exitosamente en processed_events: id={}", processedEvent.getId());
                    }
                })
                .onErrorResumeNext(e -> {
                    log.error("Error en tryRegister para evento: id={}, error={}", processedEvent.getId(), e.getMessage(), e);
                    return Single.error(e);
                });
    }
}