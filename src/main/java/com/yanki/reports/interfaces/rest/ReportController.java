package com.yanki.reports.interfaces.rest;

import com.yanki.reports.domain.model.AggregatedReport;
import com.yanki.reports.domain.model.CardMovementsReport;
import com.yanki.reports.domain.model.Customer360;
import com.yanki.reports.domain.model.CustomerReport;
import com.yanki.reports.domain.model.CustomerSummary;
import com.yanki.reports.domain.model.DailyAggregate;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductSummary;
import com.yanki.reports.domain.port.in.GenerateAggregatedReportUseCase;
import com.yanki.reports.domain.port.in.GetCardMovementsUseCase;
import com.yanki.reports.domain.port.in.GetCustomer360UseCase;
import com.yanki.reports.domain.port.in.GetCustomerReportUseCase;
import com.yanki.reports.domain.port.in.GetCustomerSummaryUseCase;
import com.yanki.reports.domain.port.in.GetDailyAggregatesUseCase;
import com.yanki.reports.domain.port.in.GetProductEventsByDateRangeUseCase;
import com.yanki.reports.domain.port.in.GetProductSummaryUseCase;
import com.yanki.reports.interfaces.rest.dto.AggregatedReportResponse;
import com.yanki.reports.interfaces.rest.dto.CardMovementsResponse;
import com.yanki.reports.interfaces.rest.dto.Customer360Response;
import com.yanki.reports.interfaces.rest.dto.CustomerReportResponse;
import com.yanki.reports.interfaces.rest.dto.CustomerSummaryResponse;
import com.yanki.reports.interfaces.rest.dto.DailyAggregateResponse;
import com.yanki.reports.interfaces.rest.dto.ProductEventResponse;
import com.yanki.reports.interfaces.rest.dto.ProductSummaryResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Controlador REST reactivo para endpoints de reportes.
 *
 * <p>Adaptador primario (Driving Adapter) que traduce peticiones HTTP a invocaciones
 * de puertos de entrada del dominio ({@code domain.port.in.*}).
 *
 * <p>Endpoints disponibles:
 * <ul>
 *   <li>{@code GET /api/reports/aggregated} — Reporte agregado por tipo de producto y rango</li>
 *   <li>{@code GET /api/reports/products} — Eventos de producto en rango de fechas (streaming)</li>
 *   <li>{@code GET /api/reports/customers/{customerId}} — Reporte completo cliente (Vista 360°)</li>
 *   <li>{@code GET /api/reports/movements/cards/{cardId}} — Últimos movimientos de tarjeta (Req. Parte II)</li>
 * </ul>
 *
 * <p>Todos los endpoints son de solo lectura y requieren autenticación JWT
 * (validada en API Gateway).
 */
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Endpoints de reportes y analítica — CQRS read-only")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final GenerateAggregatedReportUseCase generateAggregatedReportUseCase;
    private final GetProductEventsByDateRangeUseCase getProductEventsByDateRangeUseCase;
    private final GetCustomerReportUseCase getCustomerReportUseCase;
    private final GetCardMovementsUseCase getCardMovementsUseCase;
    private final GetCustomerSummaryUseCase getCustomerSummaryUseCase;
    private final GetCustomer360UseCase getCustomer360UseCase;
    private final GetProductSummaryUseCase getProductSummaryUseCase;
    private final GetDailyAggregatesUseCase getDailyAggregatesUseCase;

    /**
     * Obtiene un reporte agregado filtrado por tipo y rango.
     *
     * @param type         tipo de producto (ACCOUNT, CARD, CREDIT, WALLET)
     * @param from         fecha inicial (inclusive, ISO-8601)
     * @param to           fecha final (inclusive, ISO-8601)
     * @param customerType tipo de cliente (NATURAL, JURIDICA)
     * @return reporte agregado con métricas consolidadas
     */
    @Operation(
            summary = "Reporte agregado por tipo de producto",
            description = "Obtiene estadísticas consolidadas de eventos para un tipo de producto específico "
                    + "dentro de un rango de fechas y tipo de cliente.\n\n"
                    + "El reporte incluye:\n"
                    + "- **count**: Cantidad total de eventos en el rango\n"
                    + "- **sumAmount**: Suma total de montos de todos los eventos\n"
                    + "- **averageAmount**: Promedio de montos (redondeado a 2 decimales)\n\n"
                    + "**Casos de uso:** Dashboard ejecutivo, KPIs por producto, análisis de tendencias.\n\n"
                    + "**Nota:** Los datos se obtienen de la proyección MongoDB alimentada por eventos Kafka, "
                    + "no de llamadas REST a otros servicios."
    )
    @Parameters({
            @Parameter(name = "type", in = ParameterIn.QUERY, required = true, description = "Tipo de producto a filtrar",
                    example = "ACCOUNT", schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET"})),
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Fecha inicial del rango (inclusive, ISO-8601)",
                    example = "2024-01-01", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date")),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "Fecha final del rango (inclusive, ISO-8601)",
                    example = "2024-12-31", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date")),
            @Parameter(name = "customerType", in = ParameterIn.QUERY, required = true, description = "Tipo de cliente a filtrar",
                    example = "NATURAL", schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", allowableValues = {"NATURAL", "JURIDICA"}))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte agregado obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos: tipo no reconocido, rango inválido o tipo cliente no válido"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para acceder a reportes"),
            @ApiResponse(responseCode = "500", description = "Error interno en agregación MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto (proyección degradada)")
    })
    @GetMapping("/aggregated")
    public Mono<AggregatedReportResponse> getAggregatedReport(
            @RequestParam("type") @NotBlank final String type,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam("customerType") @NotBlank final String customerType) {

        log.info("Solicitud de reporte agregado: type={}, from={}, to={}, customerType={}", type, from, to, customerType);
        try {
            return RxJava3Adapter.singleToMono(
                    generateAggregatedReportUseCase.execute(type, from, to, customerType)
                            .map(aggregated -> {
                                log.info("Reporte agregado generado exitosamente: type={}, count={}", type, aggregated.getCount());
                                return AggregatedReportResponse.fromDomain(aggregated);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado en reporte agregado: type={}, from={}, to={}, customerType={}", type, from, to, customerType, ex);
            throw ex;
        }
    }

    /**
     * Obtiene todos los eventos de producto en un rango de fechas (streaming).
     *
     * @param from fecha inicial (inclusive, ISO-8601)
     * @param to   fecha final (inclusive, ISO-8601)
     * @return flujo reactivo de eventos (Server-Sent Events)
     */
    @Operation(
            summary = "Eventos de producto por rango de fechas",
            description = "Retorna el flujo completo de eventos de producto proyectados desde Kafka a MongoDB "
                    + "dentro de un rango de fechas especificado.\n\n"
                    + "Cada evento incluye información completa: tipo de evento, identificadores, montos, "
                    + "saldo posterior, metadata adicional y timestamps.\n\n"
                    + "**Casos de uso:** Auditoría de eventos, reconstrucción de estado, debugging de proyecciones.\n\n"
                    + "**Nota:** Retorna un flujo reactivo (Server-Sent Events). La respuesta puede contener "
                    + "miles de eventos dependiendo del rango de fechas."
    )
    @Parameters({
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Fecha inicial del rango (inclusive, ISO-8601)",
                    example = "2024-06-01", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date")),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "Fecha final del rango (inclusive, ISO-8601)",
                    example = "2024-06-30", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flujo de eventos obtenido exitosamente",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.APPLICATION_NDJSON_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ProductEventResponse.class)
                            ))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos: rango de fechas inválido (from > to)"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para acceder a eventos"),
            @ApiResponse(responseCode = "500", description = "Error interno consultando eventos de MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto")
    })
    @GetMapping(value = "/products", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<ProductEventResponse> getProducts(
            @RequestParam(value = "from", defaultValue = "2020-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam(value = "to", defaultValue = "2099-12-31") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate) {
        
        // Soporte para alias startDate/endDate (compatibilidad con clientes que usan estos nombres)
        LocalDate effectiveFrom = (startDate != null) ? startDate : from;
        LocalDate effectiveTo = (endDate != null) ? endDate : to;

        log.info("Solicitud de eventos de producto por rango: from={}, to={}", from, to);
        try {
            return RxJava3Adapter.flowableToFlux(
                    getProductEventsByDateRangeUseCase.execute(from, to)
                            .map(productEvent -> {
                                log.debug("Evento de producto emitido: id={}, type={}", productEvent.getId(), productEvent.getEventType());
                                return ProductEventResponse.fromDomain(productEvent);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado obteniendo eventos de producto: from={}, to={}", from, to, ex);
            throw ex;
        }
    }

    /**
     * Obtiene el reporte completo de un cliente con todos sus productos y eventos (Vista 360°).
     *
     * @param customerId identificador del cliente
     * @return reporte del cliente con productos agrupados (products = [] si no tiene eventos)
     */
    @Operation(
            summary = "Reporte completo de cliente (Vista 360°)",
            description = "Agrega todos los productos (cuentas, tarjetas, créditos, wallets) asociados a un cliente "
                    + "con sus eventos correspondientes, organizados por tipo de producto.\n\n"
                    + "La respuesta incluye:\n"
                    + "- **customerId**: Identificador del cliente\n"
                    + "- **products**: Lista de grupos de productos, cada uno con:\n"
                    + "  - **aggregateId**: ID del producto (accountId, cardId, etc.)\n"
                    + "  - **productType**: Tipo (ACCOUNT, CARD, CREDIT, WALLET)\n"
                    + "  - **subType**: Subtipo (SAVINGS, CREDIT, PERSONAL_LOAN, etc.)\n"
                    + "  - **events**: Lista de eventos asociados al producto\n\n"
                    + "**Casos de uso:** Vista 360° del cliente, atención al cliente, análisis de cross-selling.\n\n"
                    + "**Nota:** Si el cliente no tiene eventos registrados, se retorna un objeto vacío con products = []. "
                    + "No se lanza 404, ya que el cliente puede existir sin haber generado eventos aún."
    )
    @Parameters({
            @Parameter(name = "customerId", in = ParameterIn.PATH, required = true, description = "Identificador único del cliente",
                    example = "6af39f59b089e973a5ffe9b6", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte del cliente obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para ver reportes de cliente"),
            @ApiResponse(responseCode = "500", description = "Error interno agregando productos desde MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto")
    })
    @GetMapping("/customers/{customerId}")
    public Mono<CustomerReportResponse> getCustomerReport(
            @PathVariable @NotBlank final String customerId) {

        log.info("Solicitud de reporte de cliente: customerId={}", customerId);
        try {
            return RxJava3Adapter.singleToMono(
                    getCustomerReportUseCase.execute(customerId)
                            .map(customerReport -> {
                                log.info("Reporte de cliente generado exitosamente: customerId={}, products={}",
                                        customerId, customerReport.getProducts().size());
                                return CustomerReportResponse.fromDomain(customerReport);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado en reporte de cliente: customerId={}", customerId, ex);
            throw ex;
        }
    }

    /**
     * Obtiene los últimos movimientos de una tarjeta (crédito o débito).
     *
     * <p><b>Requerimiento obligatorio Parte II:</b> "Implementar un reporte con los últimos 10 movimientos
     * de la tarjeta de débito y de crédito".
     *
     * @param cardId identificador de la tarjeta
     * @param limit  número máximo de movimientos a retornar (default 10, máx 100)
     * @return respuesta con los movimientos de la tarjeta
     */
    @Operation(
            summary = "Últimos movimientos de tarjeta (Req. Parte II)",
            description = "Consulta optimizada para obtener los movimientos más recientes de una tarjeta "
                    + "de crédito o débito, ordenados por fecha de ocurrencia (más reciente primero).\n\n"
                    + "La respuesta incluye:\n"
                    + "- **cardId**: Identificador de la tarjeta\n"
                    + "- **productType**: Tipo de producto (CARD)\n"
                    + "- **subType**: Subtipo de tarjeta (CREDIT, DEBIT)\n"
                    + "- **movements**: Lista de eventos de consumo/pago asociados a la tarjeta\n\n"
                    + "**Casos de uso:** Estado de cuenta en tiempo real, disputas de transacciones, "
                    + "monitoreo de actividad sospechosa.\n\n"
                    + "**Nota:** El parámetro `limit` permite controlar el número de movimientos retornados "
                    + "(por defecto 10, máximo recomendado 100 para rendimiento óptimo)."
    )
    @Parameters({
            @Parameter(name = "cardId", in = ParameterIn.PATH, required = true, description = "Identificador único de la tarjeta",
                    example = "CARD-12345", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")),
            @Parameter(name = "limit", in = ParameterIn.QUERY, required = false, description = "Número máximo de movimientos (default: 10, máx: 100)",
                    example = "10", schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "integer", format = "int32", defaultValue = "10"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos de tarjeta obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada: no hay eventos para el cardId"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para ver movimientos de tarjeta"),
            @ApiResponse(responseCode = "500", description = "Error interno consultando MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto")
    })
    @GetMapping("/movements/cards/{cardId}")
    public Mono<CardMovementsResponse> getCardMovements(
            @PathVariable @NotBlank final String cardId,
            @RequestParam(value = "limit", defaultValue = "10") final int limit) {

        log.info("Solicitud de movimientos de tarjeta: cardId={}, limit={}", cardId, limit);
        try {
            return RxJava3Adapter.singleToMono(
                    getCardMovementsUseCase.execute(cardId, limit)
                            .map(cardMovements -> {
                                log.info("Movimientos de tarjeta obtenidos exitosamente: cardId={}, count={}",
                                        cardId, cardMovements.getMovements().size());
                                return CardMovementsResponse.fromDomain(cardMovements);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado obteniendo movimientos de tarjeta: cardId={}, limit={}", cardId, limit, ex);
            throw ex;
        }
    }

    /**
     * Obtiene el resumen 360° de un cliente.
     *
     * @param customerId identificador del cliente
     * @return resumen del cliente con totales de productos y saldos
     */
    @Operation(
            summary = "Resumen 360° del cliente",
            description = "Obtiene el resumen consolidado de un cliente con totales de cuentas, tarjetas, "
                    + "créditos y wallets, así como saldos acumulados y estado de deudas vencidas.\n\n"
                    + "La respuesta incluye:\n"
                    + "- **customerId**: Identificador del cliente\n"
                    + "- **customerType**: Tipo de cliente (NATURAL, JURIDICA)\n"
                    + "- **profile**: Perfil (PERSONAL, VIP, PYME, EMPRESARIAL)\n"
                    + "- **status**: Estado (ACTIVE, INACTIVE, BLOCKED)\n"
                    + "- **totalAccounts/totalCards/totalCredits/totalWallets**: Contadores de productos\n"
                    + "- **totalBalanceAccounts**: Saldo total en cuentas bancarias\n"
                    + "- **totalCreditLimit/totalCreditDebt**: Límite y deuda de créditos\n"
                    + "- **totalWalletBalance**: Saldo total en wallets\n"
                    + "- **hasOverdueDebt**: Indica si tiene deudas vencidas\n\n"
                    + "**Casos de uso:** Dashboard de cliente, scoring crediticio, análisis de relación cliente-banco.\n\n"
                    + "**Fuente:** Colección customer_summary materializada por eventos Kafka."
    )
    @Parameters({
            @Parameter(name = "customerId", in = ParameterIn.PATH, required = true,
                    description = "Identificador único del cliente",
                    example = "6af39f59b089e973a5ffe9b6",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen del cliente obtenido exitosamente",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CustomerSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado: no hay resumen materializado para este customerId"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para ver resumen de cliente"),
            @ApiResponse(responseCode = "500", description = "Error interno consultando MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto (proyección degradada)")
    })
    @GetMapping("/customer-summary/{customerId}")
    public Mono<CustomerSummaryResponse> getCustomerSummary(
            @PathVariable @NotBlank final String customerId) {

        log.info("Solicitud de resumen de cliente: customerId={}", customerId);
        try {
            return RxJava3Adapter.maybeToMono(
                    getCustomerSummaryUseCase.execute(customerId)
                            .map(customerSummary -> {
                                log.info("Resumen de cliente obtenido exitosamente: customerId={}", customerId);
                                return CustomerSummaryResponse.fromDomain(customerSummary);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado obteniendo resumen de cliente: customerId={}", customerId, ex);
            throw ex;
        }
    }

    /**
     * Obtiene la vista 360° completa de un cliente con productos y eventos recientes.
     *
     * @param customerId identificador del cliente
     * @return vista 360° con resumen, productos detallados y eventos recientes
     */
    @Operation(
            summary = "Vista 360° completa del cliente",
            description = "Obtiene la vista completa de un cliente combinando el resumen consolidado, "
                    + "la lista detallada de productos (cuentas, tarjetas, créditos, wallets) con su estado actual, "
                    + "y los últimos eventos registrados.\n\n"
                    + "La respuesta incluye:\n"
                    + "- **customerId**: Identificador del cliente\n"
                    + "- **customerType**: Tipo de cliente (NATURAL, JURIDICA)\n"
                    + "- **profile**: Perfil (PERSONAL, VIP, PYME, EMPRESARIAL)\n"
                    + "- **status**: Estado (ACTIVE, INACTIVE, BLOCKED)\n"
                    + "- **totalAccounts/totalCards/totalCredits/totalWallets**: Contadores de productos\n"
                    + "- **totalBalanceAccounts**: Saldo total en cuentas bancarias\n"
                    + "- **totalCreditLimit/totalCreditDebt**: Límite y deuda de créditos\n"
                    + "- **totalWalletBalance**: Saldo total en wallets\n"
                    + "- **hasOverdueDebt**: Indica si tiene deudas vencidas\n"
                    + "- **products**: Lista detallada de productos con estado, saldos y límites\n"
                    + "- **recentEvents**: Últimos 20 eventos del cliente (ordenados por fecha descendente)\n\n"
                    + "**Casos de uso:** Dashboard de cliente, atención al cliente, análisis de cross-selling, "
                    + "scoring crediticio.\n\n"
                    + "**Fuente:** Proyecciones MongoDB materializadas por eventos Kafka "
                    + "(customer_summary, product_summary, product_events)."
    )
    @Parameters({
            @Parameter(name = "customerId", in = ParameterIn.PATH, required = true,
                    description = "Identificador único del cliente",
                    example = "6af39f59b089e973a5ffe9b6",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vista 360° del cliente obtenida exitosamente",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Customer360Response.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado: no hay resumen materializado para este customerId"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para ver vista 360° del cliente"),
            @ApiResponse(responseCode = "500", description = "Error interno consultando MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto (proyección degradada)")
    })
    @GetMapping("/customer-360/{customerId}")
    public Mono<Customer360Response> getCustomer360(
            @PathVariable @NotBlank final String customerId) {

        log.info("Solicitud de vista 360° del cliente: customerId={}", customerId);
        try {
            return RxJava3Adapter.maybeToMono(
                    getCustomer360UseCase.execute(customerId)
                            .map(customer360 -> {
                                log.info("Vista 360° del cliente obtenida exitosamente: customerId={}, products={}, events={}",
                                        customerId, customer360.getProducts().size(), customer360.getRecentEvents().size());
                                return Customer360Response.fromDomain(customer360);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado obteniendo vista 360° del cliente: customerId={}", customerId, ex);
            throw ex;
        }
    }

    /**
     * Obtiene el resumen de un producto específico.
     *
     * @param productId identificador del producto
     * @return resumen del producto con estado, saldos y límites
     */
    @Operation(
            summary = "Resumen de producto específico",
            description = "Obtiene el estado actual de un producto bancario (cuenta, tarjeta, crédito o wallet) "
                    + "con saldos, límites y estado.\n\n"
                    + "La respuesta incluye:\n"
                    + "- **productId**: Identificador del producto\n"
                    + "- **productType**: Tipo (ACCOUNT, CARD, CREDIT, WALLET)\n"
                    + "- **subType**: Subtipo (SAVINGS, CREDIT, DEBIT, PERSONAL_LOAN, etc.)\n"
                    + "- **customerId**: Cliente propietario\n"
                    + "- **status**: Estado (OPEN, ACTIVE, CLOSED, BLOCKED, OVERDUE)\n"
                    + "- **currentBalance**: Saldo actual (cuentas y wallets)\n"
                    + "- **creditLimit/availableLimit**: Límites de crédito (tarjetas)\n"
                    + "- **outstandingBalance**: Saldo pendiente (créditos)\n"
                    + "- **currency**: Moneda (PEN, USD)\n"
                    + "- **openedAt/closedAt/lastMovementAt**: Timestamps clave\n\n"
                    + "**Casos de uso:** Consulta de estado de producto, validación de límites, monitoreo de productos.\n\n"
                    + "**Fuente:** Colección product_summary materializada por eventos Kafka."
    )
    @Parameters({
            @Parameter(name = "productId", in = ParameterIn.PATH, required = true,
                    description = "Identificador único del producto (accountId, cardId, creditId, walletId)",
                    example = "ACC-12345",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen del producto obtenido exitosamente",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ProductSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado: no hay resumen materializado para este productId"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para ver resumen de producto"),
            @ApiResponse(responseCode = "500", description = "Error interno consultando MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto (proyección degradada)")
    })
    @GetMapping("/products/{productId}")
    public Mono<ProductSummaryResponse> getProductSummary(
            @PathVariable @NotBlank final String productId) {

        log.info("Solicitud de resumen de producto: productId={}", productId);
        try {
            return RxJava3Adapter.maybeToMono(
                    getProductSummaryUseCase.executeByProductId(productId)
                            .map(productSummary -> {
                                log.info("Resumen de producto obtenido exitosamente: productId={}", productId);
                                return ProductSummaryResponse.fromDomain(productSummary);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado obteniendo resumen de producto: productId={}", productId, ex);
            throw ex;
        }
    }

    /**
     * Obtiene los agregados diarios para dashboards y analítica.
     *
     * @param date        fecha del día (ISO-8601)
     * @param productType tipo de producto (opcional)
     * @return lista de agregados diarios
     */
    @Operation(
            summary = "Agregados diarios para dashboards y analítica",
            description = "Obtiene las métricas diarias consolidadas por tipo de producto y tipo de cliente.\n\n"
                    + "La respuesta incluye:\n"
                    + "- **id**: Identificador compuesto (YYYY-MM-DD:PRODUCT_TYPE:CUSTOMER_TYPE)\n"
                    + "- **date**: Fecha del día agregado\n"
                    + "- **productType**: Tipo de producto (ACCOUNT, CARD, CREDIT, WALLET)\n"
                    + "- **customerType**: Tipo de cliente (NATURAL, JURIDICA)\n"
                    + "- **movementCount**: Cantidad total de movimientos en el día\n"
                    + "- **totalAmount**: Monto total acumulado en el día\n"
                    + "- **avgAmount**: Monto promedio por movimiento\n"
                    + "- **uniqueCustomers**: Clientes únicos con movimientos\n"
                    + "- **uniqueProducts**: Productos únicos con movimientos\n\n"
                    + "**Casos de uso:** Dashboards operativos, reportes diarios, análisis de volumen transaccional.\n\n"
                    + "**Fuente:** Colección daily_aggregates materializada por eventos Kafka."
    )
    @Parameters({
            @Parameter(name = "date", in = ParameterIn.QUERY, required = true,
                    description = "Fecha del día para obtener agregados (ISO-8601)",
                    example = "2024-06-15",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date")),
            @Parameter(name = "productType", in = ParameterIn.QUERY, required = false,
                    description = "Filtro opcional por tipo de producto. Si no se especifica, retorna todos los tipos.",
                    example = "ACCOUNT",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string", allowableValues = {"ACCOUNT", "CARD", "CREDIT", "WALLET"}))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agregados diarios obtenidos exitosamente",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = DailyAggregateResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos: fecha inválida o tipo de producto no reconocido"),
            @ApiResponse(responseCode = "401", description = "No autorizado: token JWT inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "Prohibido: rol sin permiso para ver agregados diarios"),
            @ApiResponse(responseCode = "500", description = "Error interno consultando MongoDB"),
            @ApiResponse(responseCode = "503", description = "Servicio no disponible: lag Kafka alto (proyección degradada)")
    })
    @GetMapping("/daily-aggregates")
    public Flux<DailyAggregateResponse> getDailyAggregates(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            @RequestParam(value = "productType", required = false) final String productType) {

        log.info("Solicitud de agregados diarios: date={}, productType={}", date, productType);
        try {
            return RxJava3Adapter.flowableToFlux(
                    getDailyAggregatesUseCase.executeByDate(date)
                            .map(dailyAggregate -> {
                                log.debug("Agregado diario emitido: date={}, productType={}",
                                        dailyAggregate.getDate(), dailyAggregate.getProductType());
                                return DailyAggregateResponse.fromDomain(dailyAggregate);
                            })
            );
        } catch (Exception ex) {
            log.error("Error inesperado obteniendo agregados diarios: date={}, productType={}", date, productType, ex);
            throw ex;
        }
    }
}