package com.yanki.reports.domain.port.in;

import com.yanki.reports.domain.model.CustomerReport;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de entrada (Use Case) para obtener reporte completo de cliente (Vista 360°).
 *
 * <p>Agrega todos los productos (cuentas, tarjetas, créditos, wallets) asociados a un cliente
 * con sus eventos correspondientes, organizados por tipo de producto.
 */
public interface GetCustomerReportUseCase {

    /**
     * Obtiene el reporte completo de un cliente.
     *
     * @param customerId identificador del cliente
     * @return {@link Single} con el reporte del cliente (products = [] si no tiene eventos)
     */
    Single<CustomerReport> execute(String customerId);
}