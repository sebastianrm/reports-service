package com.yanki.reports.infrastructure.persistence.adapter;

import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.port.out.CustomerSummaryPersistencePort;
import com.yanki.reports.infrastructure.persistence.document.CustomerSummaryDocument;
import com.yanki.reports.infrastructure.persistence.mapper.CustomerSummaryDocumentMapper;
import com.yanki.reports.infrastructure.persistence.repository.SpringDataCustomerSummaryRepository;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * Adaptador de salida que implementa la persistencia del resumen 360° del cliente sobre MongoDB Reactivo.
 *
 * <p>Convierte entre tipos Reactor ({@code Mono}) de Spring Data
 * y tipos RxJava 3 ({@code Single}, {@code Maybe}) del dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerSummaryPersistenceAdapter implements CustomerSummaryPersistencePort {

    private final SpringDataCustomerSummaryRepository repository;

    @Override
    public Single<CustomerSummary> save(CustomerSummary summary) {
        log.debug("Guardando resumen de cliente en MongoDB: customerId={}", summary.getCustomerId());
        CustomerSummaryDocument document = CustomerSummaryDocumentMapper.toDocument(summary);
        return RxJava3Adapter.monoToSingle(
                repository.save(document)
                        .map(CustomerSummaryDocumentMapper::toDomain)
                        .doOnSuccess(saved -> log.info("Resumen de cliente guardado en MongoDB: customerId={}", saved.getCustomerId()))
                        .doOnError(e -> log.error("Error guardando resumen de cliente en MongoDB: customerId={}, error={}",
                                summary.getCustomerId(), e.getMessage(), e))
        );
    }

    @Override
    public Maybe<CustomerSummary> findByCustomerId(String customerId) {
        log.debug("Buscando resumen de cliente en MongoDB: customerId={}", customerId);
        return RxJava3Adapter.monoToMaybe(
                repository.findByCustomerId(customerId)
                        .map(CustomerSummaryDocumentMapper::toDomain)
                        .doOnSuccess(summary -> log.debug("Resumen de cliente encontrado: customerId={}", customerId))
                        .doOnError(e -> log.error("Error buscando resumen de cliente en MongoDB: customerId={}, error={}",
                                customerId, e.getMessage(), e))
        );
    }
}
