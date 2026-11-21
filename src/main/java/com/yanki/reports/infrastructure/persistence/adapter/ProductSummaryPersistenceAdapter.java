package com.yanki.reports.infrastructure.persistence.adapter;

import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.out.ProductSummaryPersistencePort;
import com.yanki.reports.infrastructure.persistence.document.ProductSummaryDocument;
import com.yanki.reports.infrastructure.persistence.mapper.ProductSummaryDocumentMapper;
import com.yanki.reports.infrastructure.persistence.repository.SpringDataProductSummaryRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

/**
 * Adaptador de salida que implementa la persistencia del resumen de productos sobre MongoDB Reactivo.
 *
 * <p>Convierte entre tipos Reactor ({@code Mono}, {@code Flux}) de Spring Data
 * y tipos RxJava 3 ({@code Single}, {@code Maybe}, {@code Flowable}) del dominio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSummaryPersistenceAdapter implements ProductSummaryPersistencePort {

    private final SpringDataProductSummaryRepository repository;

    @Override
    public Single<ProductSummary> save(ProductSummary summary) {
        log.debug("Guardando resumen de producto en MongoDB: productId={}", summary.getProductId());
        ProductSummaryDocument document = ProductSummaryDocumentMapper.toDocument(summary);
        return RxJava3Adapter.monoToSingle(
                repository.save(document)
                        .map(ProductSummaryDocumentMapper::toDomain)
                        .doOnSuccess(saved -> log.info("Resumen de producto guardado en MongoDB: productId={}", saved.getProductId()))
                        .doOnError(e -> log.error("Error guardando resumen de producto en MongoDB: productId={}, error={}",
                                summary.getProductId(), e.getMessage(), e))
        );
    }

    @Override
    public Maybe<ProductSummary> findByProductId(String productId) {
        log.debug("Buscando resumen de producto en MongoDB: productId={}", productId);
        return RxJava3Adapter.monoToMaybe(
                repository.findByProductId(productId)
                        .map(ProductSummaryDocumentMapper::toDomain)
                        .doOnSuccess(summary -> log.debug("Resumen de producto encontrado: productId={}", productId))
                        .doOnError(e -> log.error("Error buscando resumen de producto en MongoDB: productId={}, error={}",
                                productId, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<ProductSummary> findByCustomerId(String customerId) {
        log.debug("Buscando resúmenes de producto por customerId en MongoDB: customerId={}", customerId);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByCustomerId(customerId)
                        .map(ProductSummaryDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando resúmenes por customerId en MongoDB: customerId={}, error={}",
                                customerId, e.getMessage(), e))
        );
    }

    @Override
    public Flowable<ProductSummary> findByCustomerIdAndProductType(String customerId, String productType) {
        log.debug("Buscando resúmenes por customerId y productType en MongoDB: customerId={}, productType={}",
                customerId, productType);
        return RxJava3Adapter.fluxToFlowable(
                repository.findByCustomerIdAndProductType(customerId, productType)
                        .map(ProductSummaryDocumentMapper::toDomain)
                        .doOnError(e -> log.error("Error buscando resúmenes por customerId y productType en MongoDB: customerId={}, productType={}, error={}",
                                customerId, productType, e.getMessage(), e))
        );
    }
}
