package com.yanki.reports.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad pura de dominio que representa los agregados diarios para dashboards y analítica.
 *
 * <p>Se materializa a partir de eventos Kafka y se almacena en la colección {@code daily_aggregates}.
 * La clave primaria es compuesta: {@code date:productType:customerType}.
 *
 * <p>Permite consultas rápidas de métricas diarias sin necesidad de agregar en tiempo real.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAggregate {

    /**
     * Identificador único compuesto (YYYY-MM-DD:PRODUCT_TYPE:CUSTOMER_TYPE).
     */
    private String id;

    /**
     * Fecha del día agregado (YYYY-MM-DD).
     */
    private LocalDate date;

    /**
     * Tipo de producto (ACCOUNT, CARD, CREDIT, WALLET).
     */
    private String productType;

    /**
     * Tipo de cliente (NATURAL, JURIDICA).
     */
    private String customerType;

    /**
     * Cantidad total de movimientos en el día.
     */
    private Integer movementCount;

    /**
     * Monto total acumulado en el día.
     */
    private BigDecimal totalAmount;

    /**
     * Monto promedio por movimiento en el día.
     */
    private BigDecimal avgAmount;

    /**
     * Cantidad de clientes únicos con movimientos en el día.
     */
    private Integer uniqueCustomers;

    /**
     * Cantidad de productos únicos con movimientos en el día.
     */
    private Integer uniqueProducts;

    /**
     * Timestamp de última actualización del agregado.
     */
    private LocalDateTime updatedAt;
}
