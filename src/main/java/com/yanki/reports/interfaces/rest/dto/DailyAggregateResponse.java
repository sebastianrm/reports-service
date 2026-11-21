package com.yanki.reports.interfaces.rest.dto;

import com.yanki.reports.domain.model.DailyAggregate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para los agregados diarios de dashboards y analítica.
 *
 * <p>Contiene métricas consolidadas por día, tipo de producto y tipo de cliente,
 * materializadas a partir de eventos Kafka.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Agregados diarios consolidados para dashboards y analítica")
public class DailyAggregateResponse {

    @Schema(description = "Identificador único compuesto en formato YYYY-MM-DD:PRODUCT_TYPE:CUSTOMER_TYPE", example = "2024-06-15:ACCOUNT:NATURAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "Fecha del día agregado (ISO-8601)", example = "2024-06-15", format = "date")
    private LocalDate date;

    @Schema(description = "Tipo de producto bancario", example = "ACCOUNT", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET"})
    private String productType;

    @Schema(description = "Tipo de cliente", example = "NATURAL", allowableValues = {"NATURAL", "JURIDICA"})
    private String customerType;

    @Schema(description = "Cantidad total de movimientos registrados en el día", example = "150")
    private Integer movementCount;

    @Schema(description = "Monto total acumulado de todos los movimientos del día", example = "75000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Monto promedio por movimiento en el día (totalAmount / movementCount)", example = "500.00")
    private BigDecimal avgAmount;

    @Schema(description = "Cantidad de clientes únicos con movimientos en el día", example = "45")
    private Integer uniqueCustomers;

    @Schema(description = "Cantidad de productos únicos con movimientos en el día", example = "60")
    private Integer uniqueProducts;

    @Schema(description = "Timestamp de última actualización del agregado (ISO-8601)", example = "2024-06-15T23:59:59", format = "date-time")
    private LocalDateTime updatedAt;

    public static DailyAggregateResponse fromDomain(DailyAggregate aggregate) {
        return DailyAggregateResponse.builder()
                .id(aggregate.getId())
                .date(aggregate.getDate())
                .productType(aggregate.getProductType())
                .customerType(aggregate.getCustomerType())
                .movementCount(aggregate.getMovementCount())
                .totalAmount(aggregate.getTotalAmount())
                .avgAmount(aggregate.getAvgAmount())
                .uniqueCustomers(aggregate.getUniqueCustomers())
                .uniqueProducts(aggregate.getUniqueProducts())
                .updatedAt(aggregate.getUpdatedAt())
                .build();
    }
}
