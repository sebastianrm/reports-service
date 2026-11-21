package com.yanki.reports.domain.service;

import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.port.in.GetCustomerSummaryUseCase;
import com.yanki.reports.domain.port.out.CustomerSummaryPersistencePort;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso para obtener el resumen 360° de un cliente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetCustomerSummaryService implements GetCustomerSummaryUseCase {

    private final CustomerSummaryPersistencePort persistencePort;

    @Override
    public Maybe<CustomerSummary> execute(String customerId) {
        log.info("Ejecutando consulta de resumen de cliente: customerId={}", customerId);

        try {
            return persistencePort.findByCustomerId(customerId)
                    .doOnSuccess(summary -> {
                        log.info("Resumen de cliente obtenido exitosamente: customerId={}, customerType={}, profile={}",
                                customerId, summary.getCustomerType(), summary.getProfile());
                    })
                    .doOnComplete(() ->
                            log.debug("Consulta de resumen de cliente completada: customerId={}", customerId)
                    )
                    .doOnError(e -> log.error("Error obteniendo resumen de cliente: customerId={}, error={}",
                            customerId, e.getMessage(), e));
        } catch (Exception ex) {
            log.error("Error inesperado en execute() de resumen de cliente: customerId={}", customerId, ex);
            throw ex;
        }
    }
}
