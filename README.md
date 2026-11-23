# reports-service

Microservicio de **reportes y analítica** de la plataforma bancaria **Yanki**.

Servicio **solo lectura / analítica** que consume **todos los eventos de dominio** para generar reportes sin invocar otros microservicios por REST. Implementa un patrón **CQRS read-only**: el write path llega por **Kafka** y la consulta se resuelve sobre proyecciones persistidas en **MongoDB** y cacheadas en **Redis**.

- **Puerto:** `8088`
- **Base de datos propia:** MongoDB `yanki_reports`
- **Colección principal:** `product_events`
- **Colección técnica:** `processed_events` (idempotencia)
- **Cache:** Redis (TTL por defecto `5m`)
- **Naturaleza:** Inbound REST de solo lectura + consumo Kafka de 19 tópicos
- **Outbound:** Ninguno por REST y ninguno por Kafka

---

## 1. Propósito del microservicio

`reports-service` centraliza la **consulta histórica y agregada** de productos bancarios sin depender de llamadas síncronas a otros microservicios.

### Objetivos funcionales (Parte II - Requerimientos obligatorios)
1. **Generar un reporte completo y general por producto** en un rango de fechas especificado por el usuario.
2. **Implementar un reporte con los últimos 10 movimientos** de la tarjeta de débito y de crédito.

### Objetivos funcionales adicionales (Parte III - Arquitectura orientada a eventos)
3. Permitir la consulta de **productos por cliente** (vista 360°).
4. Exponer **reportes agregados** por tipo de producto, período y perfil de cliente.
5. Mantener **proyección global** de todos los eventos de dominio via Kafka.

### Objetivos técnicos
1. Consumir eventos de dominio desde Kafka (19 tópicos).
2. Persistir una proyección analítica en MongoDB (`product_events`).
3. Mantener **idempotencia** al reprocesar eventos (`processed_events`).
4. Servir consultas optimizadas usando índices y cache Redis.
5. No depender de REST saliente para cumplir consultas (CQRS puro).
6. Reactividad end-to-end (Spring WebFlux + RxJava).

---

## 2. Stack tecnológico

| Capa | Tecnología | Versión / detalle |
|---|---|---|
| Lenguaje | Java | 17 |
| Framework base | Spring Boot | 3.3.5 |
| Cloud | Spring Cloud | 2023.0.3 |
| Web reactiva | Spring WebFlux | Reactivo end-to-end |
| Reactividad de dominio | RxJava | 3.1.10 |
| Persistencia | Spring Data MongoDB Reactive | `ReactiveMongoRepository` + `ReactiveMongoTemplate` |
| Caché | Spring Data Redis Reactive | TTL configurable |
| Mensajería | Spring Kafka | Consumidor de 19 tópicos |
| Documentación API | springdoc-openapi | 2.6.0 |
| Resiliencia | Resilience4j | timeout `2s`, circuit breaker, time limiter, bulkhead |
| Calidad | Checkstyle | 10.21.1 (deuda técnica — plugin no se ejecuta) |
| Cobertura | JaCoCo | 0.8.12 |
| Build | Maven | 3.9+ |
| Contenedorización | Docker | Imagen Java 17 |
| Descubrimiento | Eureka Client | Registro y heartbeat |
| Configuración | Spring Cloud Config Client | Externalizada |

### Restricciones obligatorias (Regla de Oro Arquitectura Hexagonal)
- **Database per service**: solo accede a `yanki_reports`.
- **Sin `@Query`**: agregaciones con `ReactiveMongoTemplate`.
- **Sin `block()` ni `Thread.sleep()`**.
- **Sin REST entre microservicios** para obtener datos.
- **Nombres en inglés** para clases, métodos, endpoints y payloads.
- **Error unificado** mediante `ApiError`.
- **Domain aislado**: `domain` no importa `infrastructure` ni `interfaces`.
- **Javadoc en español** en clases y métodos públicos.

---

## 3. Lógica de negocio

Toda la lógica de negocio analítica del servicio se basa en una **proyección global de eventos de dominio**.

### 3.1 Regla base
Cada evento de negocio consumido desde Kafka se transforma en un documento homogéneo dentro de `product_events`, permitiendo consultas históricas por:
- producto,
- cliente,
- tipo de producto,
- período,
- tipo de evento.

### 3.2 Qué reportes resuelve (Estado actual - 8 endpoints implementados)

#### A. Reporte agregado por tipo de producto (`GET /api/reports/aggregated`)
Expone métricas analíticas agregadas por:
- `type` de producto: `ACCOUNT`, `CARD`, `CREDIT`, `WALLET`
- rango de fechas (`from`, `to` obligatorios)
- `customerType`: `NATURAL`, `JURIDICA`

Métricas:
- cantidad de eventos (`count`)
- suma de montos (`sumAmount`)
- promedio (`averageAmount`)

#### B. Eventos de producto por rango de fechas (`GET /api/reports/products`)
Retorna **flujo reactivo** (server-sent events) de todos los eventos proyectados en un rango de fechas.
- Query params: `from`, `to` (con valores por defecto: `2020-01-01` y `2099-12-31`)
- Alias compatibles: `startDate` / `endDate` (si se proporcionan, sobrescriben `from`/`to`)
- Respuesta: `application/x-ndjson` (Flux de `ProductEventResponse`)
- Caso de uso: Auditoría, reconstrucción de estado, debugging de proyecciones.

#### C. Reporte completo de cliente / Vista 360° (`GET /api/reports/customers/{customerId}`)
Agrega todos los productos (cuentas, tarjetas, créditos, wallets) asociados a un cliente con sus eventos correspondientes, organizados por tipo de producto.
- Retorna `CustomerReportResponse` con lista de `ProductGroup` (aggregateId, productType, subType, events[]).
- Si el cliente no tiene eventos, retorna objeto vacío (`products = []`), **no 404**.

#### D. Últimos movimientos de tarjeta (`GET /api/reports/movements/cards/{cardId}`)
**Cumple requerimiento Parte II**: "Últimos 10 movimientos de la tarjeta de débito y de crédito".
- Query param opcional: `limit` (default 10, máx recomendado 100)
- Orden: descendente por `occurredAt`
- Retorna `CardMovementsResponse` con cardId, productType, subType (CREDIT/DEBIT), movements[].

#### E. Resumen 360° del cliente (`GET /api/reports/customer-summary/{customerId}`)
Consulta la colección `customer_summary` para obtener un resumen consolidado del cliente.
- Retorna `CustomerSummaryResponse` con 15 campos: `customerId`, `customerType`, `profile`, `status`, `totalAccounts`, `totalCards`, `totalCredits`, `totalWallets`, `totalBalanceAccounts`, `totalCreditLimit`, `totalCreditDebt`, `totalWalletBalance`, `hasOverdueDebt`, `lastMovementAt`, `updatedAt`.
- Fuente de datos: Colección `customer_summary` (materializada desde eventos Kafka).

#### F. Vista 360° completa del cliente (`GET /api/reports/customer-360/{customerId}`)
**Nuevo endpoint**: Combina el resumen consolidado del cliente, la lista detallada de productos con su estado actual, y los últimos eventos registrados.
- Retorna `Customer360Response` con: resumen del cliente (15 campos), `products[]` (lista de `ProductSummaryResponse` con estado, saldos y límites), `recentEvents[]` (últimos 20 eventos como `ProductEventResponse`).
- Fuente de datos: Proyecciones MongoDB (`customer_summary`, `product_summary`, `product_events`).
- Caso de uso: Dashboard de cliente, atención al cliente, análisis de cross-selling, scoring crediticio.

#### G. Resumen de producto específico (`GET /api/reports/products/{productId}`)
Consulta la colección `product_summary` para obtener el estado actual de un producto bancario.
- Retorna `ProductSummaryResponse` con 13 campos: `productId`, `productType`, `subType`, `customerId`, `status`, `currentBalance`, `creditLimit`, `availableLimit`, `outstandingBalance`, `currency`, `openedAt`, `closedAt`, `lastMovementAt`, `updatedAt`.
- Fuente de datos: Colección `product_summary` (materializada desde eventos Kafka).

#### H. Agregados diarios (`GET /api/reports/daily-aggregates`)
Consulta la colección `daily_aggregates` para obtener métricas diarias consolidadas: útil para dashboards y analytics.
- Retorna lista de `DailyAggregateResponse` con 10 campos: `id` (clave compuesta `YYYY-MM-DD:PRODUCT_TYPE:CUSTOMER_TYPE`), `date`, `productType`, `customerType`, `movementCount`, `totalAmount`, `avgAmount`, `uniqueCustomers`, `uniqueProducts`, `updatedAt`.
- Query params: `date*` (obligatorio, ISO-8601), `productType` (opcional, actualmente ignorado en el controller).
- Fuente de datos: Colección `daily_aggregates` (materializada desde eventos Kafka).

### 3.3 Reportes NO implementados aún (Backlog)
| Reporte | Endpoint propuesto | Requerimiento |
|---------|-------------------|---------------|
| Estado de cuenta paginado | `GET /api/reports/accounts/{accountId}/statement` | Análisis movimientos cuenta |
| Plan de pagos de crédito | `GET /api/reports/credits/{creditId}/schedule` | Créditos |
| Actividad de wallet | `GET /api/reports/wallets/{walletId}/activity` | Wallet Yanki |

### 3.4 Reglas de consistencia
1. **Idempotencia obligatoria**: si un mismo evento llega más de una vez, se descarta (clave `<topic>:<aggregateId>:<occurredAt>` en `processed_events`).
2. **Orden lógico por aggregate**: la consulta se basa en `occurredAt`.
3. **Degradación por lag alto**: si el consumer lag supera el umbral operativo (`YANKI_REPORTS_LAG_THRESHOLD`), los reportes pueden responder `503 Service Unavailable` con code `KAFKA_LAG_HIGH`.
4. **No muta productos operacionales**: solo materializa una vista analítica.
5. **Backfill permitido**: puede reprocesar eventos desde el inicio sin duplicar información lógica.

---

## 4. Arquitectura funcional

### Flujo de escritura analítica
`Kafka event → ReportsEventConsumer → validación/idempotencia → normalización → product_events / processed_events`

### Flujo de lectura
`HTTP GET → ReportController → UseCase (domain.port.in) → Service (domain.service) → PersistencePort → Adapter → Redis Cache / Mongo Aggregation → Response DTO`

### Fuente de verdad del servicio
- **Operacional del servicio:** `product_events`
- **Técnica de deduplicación:** `processed_events`

### Estructura hexagonal (Regla de Oro)
```
src/main/java/com/yanki/reports
├── domain
│   ├── model           # Entidades puras - SIN anotaciones Spring
│   │   ├── ProductEvent.java
│   │   ├── ProcessedEvent.java
│   │   ├── AggregatedReport.java
│   │   ├── CardMovementsReport.java
│   │   ├── CustomerReport.java
│   │   ├── CustomerSummary.java      # Resumen consolidado del cliente
│   │   ├── Customer360.java          # Vista 360° completa (nuevo)
│   │   ├── ProductSummary.java       # Estado actual del producto
│   │   ├── DailyAggregate.java       # Agregados diarios
│   │   └── ProductGroup.java
│   ├── port
│   │   ├── in          # Use Cases (puertos de entrada)
│   │   │   ├── GenerateAggregatedReportUseCase.java
│   │   │   ├── GetProductEventsByDateRangeUseCase.java
│   │   │   ├── GetCustomerReportUseCase.java
│   │   │   ├── GetCardMovementsUseCase.java
│   │   │   ├── GetCustomerSummaryUseCase.java
│   │   │   ├── GetCustomer360UseCase.java        # Nuevo
│   │   │   ├── GetProductSummaryUseCase.java
│   │   │   ├── GetDailyAggregatesUseCase.java
│   │   │   └── IngestEventUseCase.java
│   │   └── out         # Puertos de salida (interfaces)
│   │       ├── ProductEventPersistencePort.java
│   │       ├── CustomerSummaryPersistencePort.java
│   │       ├── ProductSummaryPersistencePort.java
│   │       ├── DailyAggregatePersistencePort.java
│   │       ├── ProcessedEventPort.java
│   │       ├── CachePort.java
│   │       └── KafkaLagPort.java
│   ├── service         # Implementaciones de Use Cases (lógica de dominio)
│   │   ├── GenerateAggregatedReportService.java
│   │   ├── GetProductEventsByDateRangeService.java
│   │   ├── GetCustomerReportService.java
│   │   ├── GetCardMovementsService.java
│   │   ├── GetCustomerSummaryService.java
│   │   ├── GetCustomer360Service.java            # Nuevo
│   │   ├── GetProductSummaryService.java
│   │   ├── GetDailyAggregatesService.java
│   │   └── IngestEventService.java
│   ├── event           # Eventos de dominio consumidos (19 tipos)
│   └── exception       # Excepciones de dominio
│       ├── BusinessException.java
│       ├── InvalidDateRangeException.java
│       ├── KafkaLagHighException.java
│       └── ProductNotFoundException.java
├── infrastructure
│   ├── config          # Configuración
│   │   ├── InfrastructureConfig.java
│   │   ├── KafkaConsumerConfig.java    # 19 container factories (1 por tipo de evento)
│   │   └── OpenApiConfig.java
│   ├── persistence
│   │   ├── adapter     # Adaptadores de persistencia (implementan puertos de salida)
│   │   │   ├── ProductEventPersistenceAdapter.java
│   │   │   ├── CustomerSummaryPersistenceAdapter.java
│   │   │   ├── ProductSummaryPersistenceAdapter.java
│   │   │   ├── DailyAggregatePersistenceAdapter.java
│   │   │   ├── ProcessedEventAdapter.java
│   │   │   └── RedisCacheAdapter.java
│   │   ├── document    # Documentos MongoDB (con anotaciones @Document)
│   │   │   ├── ProductEventDocument.java
│   │   │   ├── CustomerSummaryDocument.java
│   │   │   ├── ProductSummaryDocument.java
│   │   │   ├── DailyAggregateDocument.java
│   │   │   └── ProcessedEventDocument.java
│   │   ├── mapper      # Mapeadores Document ↔ Domain
│   │   │   ├── ProductEventDocumentMapper.java
│   │   │   ├── CustomerSummaryDocumentMapper.java
│   │   │   ├── ProductSummaryDocumentMapper.java
│   │   │   ├── DailyAggregateDocumentMapper.java
│   │   │   └── ProcessedEventDocumentMapper.java
│   │   └── repository  # Spring Data Reactive Repositories
│   │       ├── SpringDataProductEventRepository.java
│   │       ├── SpringDataCustomerSummaryRepository.java
│   │       ├── SpringDataProductSummaryRepository.java
│   │       ├── SpringDataDailyAggregateRepository.java
│   │       └── SpringDataProcessedEventRepository.java
│   └── messaging
│       ├── adapter     # Adaptadores de mensajería
│       │   └── KafkaLagAdapter.java
│       └── consumer    # Consumidor Kafka
│           └── ReportsEventConsumer.java   # 19 @KafkaListener methods
└── interfaces
    └── rest
        ├── controller  # ReportController (8 endpoints)
        ├── dto         # DTOs de respuesta
        │   ├── AggregatedReportResponse.java
        │   ├── CardMovementsResponse.java
        │   ├── CustomerReportResponse.java
        │   ├── CustomerSummaryResponse.java
        │   ├── Customer360Response.java        # Nuevo
        │   ├── ProductSummaryResponse.java
        │   ├── DailyAggregateResponse.java
        │   ├── ProductEventResponse.java
        │   ├── ProductGroupResponse.java
        │   └── DateRange.java
        ├── ApiError.java
        └── GlobalExceptionHandler.java
```

---

## 5. Endpoints REST (8 Implementados)

Base URL: `/api/reports`

> Todos los endpoints son **GET** y de **solo lectura**. Requieren JWT Bearer token (validado en API Gateway).

| Método | Endpoint | Descripción | Parámetros |
|---|---|---|---|
| `GET` | `/api/reports/aggregated` | Reporte agregado por tipo producto | `type*` (ACCOUNT/CARD/CREDIT/WALLET), `from*` (date), `to*` (date), `customerType*` (NATURAL/JURIDICA) |
| `GET` | `/api/reports/products` | Eventos de producto por rango (streaming) | `from` (date, def: 2020-01-01), `to` (date, def: 2099-12-31), `startDate`/`endDate` (alias) |
| `GET` | `/api/reports/products/{productId}` | Resumen de producto específico (colección product_summary) | Path: `productId` |
| `GET` | `/api/reports/customers/{customerId}` | Reporte completo cliente (vista 360° con eventos) | Path: `customerId` |
| `GET` | `/api/reports/customer-summary/{customerId}` | Resumen 360° del cliente (colección customer_summary) | Path: `customerId` |
| `GET` | `/api/reports/customer-360/{customerId}` | **Vista 360° completa** (resumen + productos + eventos recientes) | Path: `customerId` |
| `GET` | `/api/reports/movements/cards/{cardId}` | Últimos movimientos tarjeta (req. Parte II) | Path: `cardId`, Query: `limit` (default 10) |
| `GET` | `/api/reports/daily-aggregates` | Agregados diarios (dashboards/analytics) | `date*` (date), `productType` (opcional) |

### 5.1 `GET /api/reports/aggregated`

**Propósito**: Métricas consolidadas para dashboard ejecutivo / KPIs.

**Query params obligatorios**:
| Parámetro | Descripción | Valores permitidos |
|---|---|---|
| `type` | Tipo de producto | `ACCOUNT`, `CARD`, `CREDIT`, `WALLET` |
| `from` | Fecha inicial (inclusive) | ISO-8601 date (`yyyy-MM-dd`) |
| `to` | Fecha final (inclusive) | ISO-8601 date (`yyyy-MM-dd`) |
| `customerType` | Tipo de cliente | `NATURAL`, `JURIDICA` |

**Caso de éxito**: `200 OK` + `AggregatedReportResponse`
```json
{
  "count": 120,
  "sumAmount": 24000.50,
  "averageAmount": 200.00,
  "type": "ACCOUNT",
  "dateRange": { "from": "2026-01-01", "to": "2026-12-31" },
  "customerType": "PERSONAL"
}
```

**Casos de error**:
| HTTP | Code | Cuándo aplica |
|---|---|---|
| `400` | `INVALID_REPORT_TYPE` | `type` no soportado |
| `400` | `INVALID_DATE_RANGE` | `from > to` o formato inválido |
| `400` | `INVALID_CUSTOMER_TYPE` | `customerType` no válido |
| `401` | `UNAUTHORIZED` | Token JWT inválido/expirado |
| `403` | `FORBIDDEN` | Rol sin permiso |
| `500` | `REPORT_GENERATION_FAILED` | Error agregación MongoDB |
| `503` | `KAFKA_LAG_HIGH` | Lag > threshold (modo degradado) |

---

### 5.2 `GET /api/reports/products`

**Propósito**: Flujo completo de eventos para auditoría / debugging.

**Query params obligatorios**:
| Parámetro | Descripción |
|---|---|
| `from` | Fecha inicial (inclusive, ISO-8601) |
| `to` | Fecha final (inclusive, ISO-8601) |

**Caso de éxito**: `200 OK` + `application/stream+json` (Flux de `ProductEventResponse`)
```json
[
  {
    "id": "event1",
    "eventType": "ACCOUNT_OPENED",
    "aggregateId": "acc123",
    "customerId": "cust1",
    "productType": "ACCOUNT",
    "subType": "SAVINGS",
    "amount": 1000,
    "balanceAfter": 1000,
    "occurredAt": "2026-06-15T10:00:00",
    "ingestedAt": "2026-06-15T10:00:00"
  }
]
```

**Casos de error**: Igual que agregado (400, 401, 403, 500, 503).

---

### 5.3 `GET /api/reports/customers/{customerId}`

**Propósito**: Vista 360° del cliente - todos sus productos y eventos.

**Path parameter**: `customerId` (string, obligatorio)

**Caso de éxito**: `200 OK` + `CustomerReportResponse`
```json
{
  "customerId": "6af39f59b089e973a5ffe9b6",
  "products": [
    {
      "aggregateId": "acc123",
      "productType": "ACCOUNT",
      "subType": "SAVINGS",
      "events": [
        { "id": "event1", "eventType": "ACCOUNT_OPENED", ... }
      ]
    },
    {
      "aggregateId": "card456",
      "productType": "CARD",
      "subType": "CREDIT",
      "events": [
        { "id": "event2", "eventType": "CARD_OPENED", ... }
      ]
    }
  ]
}
```

**Nota**: Si el cliente no tiene eventos, retorna `products: []` (no 404).

**Casos de error**: 401, 403, 500, 503.

---

### 5.4 `GET /api/reports/movements/cards/{cardId}`

**Propósito**: **Cumple requerimiento Parte II** - "Últimos 10 movimientos de la tarjeta de débito y de crédito".

**Path parameter**: `cardId` (string, obligatorio)

**Query param opcional**: `limit` (int, default 10, máx recomendado 100)

**Caso de éxito**: `200 OK` + `CardMovementsResponse`
```json
{
  "cardId": "card789",
  "productType": "CARD",
  "subType": "CREDIT",
  "movements": [
    {
      "id": "mov1",
      "eventType": "CARD_CONSUMPTION",
      "aggregateId": "card789",
      "customerId": "cust1",
      "productType": "CARD",
      "subType": "CREDIT",
      "amount": 150,
      "balanceAfter": 4850,
      "occurredAt": "2026-11-10T12:00:00",
      "ingestedAt": "2026-11-10T12:00:00"
    }
  ]
}
```

**Casos de error**:
| HTTP | Code | Cuándo aplica |
|---|---|---|
| `404` | `PRODUCT_NOT_FOUND` | No hay eventos para el cardId |
| `401` | `UNAUTHORIZED` | Token JWT inválido |
| `403` | `FORBIDDEN` | Sin permiso |
| `500` | `REPORT_GENERATION_FAILED` | Error consulta MongoDB |
| `503` | `KAFKA_LAG_HIGH` | Lag alto |

---

## 6. Contrato de error unificado

Formato `ApiError` para cualquier error controlado:

```json
{
  "timestamp": "2026-11-08T21:00:00",
  "status": 404,
  "error": "Not Found",
  "code": "PRODUCT_NOT_FOUND",
  "message": "No report data found for cardId card789",
  "path": "/api/reports/movements/cards/card789",
  "details": []
}
```

### Catálogo de códigos
| Code | HTTP | Descripción |
|---|---|---|
| `PRODUCT_NOT_FOUND` | `404` | No existen eventos para el recurso consultado |
| `INVALID_DATE_RANGE` | `400` | Fechas inválidas o `from > to` |
| `INVALID_REPORT_TYPE` | `400` | Tipo de reporte agregado no soportado |
| `INVALID_CUSTOMER_TYPE` | `400` | Tipo de cliente no válido |
| `MISSING_REQUIRED_PARAMETER` | `400` | Faltan query params obligatorios |
| `KAFKA_LAG_HIGH` | `503` | Proyección puede estar inconsistente/atrasada |
| `REPORT_GENERATION_FAILED` | `500` | Fallo interno generando la consulta |

---

## 7. Ejemplos de uso

```bash
# Reporte agregado
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&from=2026-01-01&to=2026-12-31&customerType=NATURAL"

# Eventos por rango (streaming)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/products?from=2026-06-01&to=2026-06-30"

# Vista 360° cliente
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/customers/6af39f59b089e973a5ffe9b6"

# Últimos 10 movimientos tarjeta (req. Parte II)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/movements/cards/CARD-12345?limit=10"

# Resumen 360° del cliente (colección customer_summary)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/customer-summary/6af39f59b089e973a5ffe9b6"

# Vista 360° completa del cliente (resumen + productos + eventos)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/customer-360/6af39f59b089e973a5ffe9b6"

# Resumen de producto específico (colección product_summary)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/products/PRODUCT-12345"

# Agregados diarios (dashboards/analytics)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8088/api/reports/daily-aggregates?date=2026-11-14"
```

---

## 8. Eventos Kafka consumidos (19 tópicos)

> `reports-service` **no produce eventos**. Solo consume y proyecta.
>
> Los 19 tópicos provienen de **6 microservicios distintos**: customers-service (3), accounts-service (3), cards-service (3), credits-service (3), wallet-service (4), transfers-service (2), y 1 evento de pagos débito.

| Topic | Payload | Proyección en `product_events` |
|---|---|---|
| `yanki.customer.registered.v1` | `CustomerRegisteredEvent` | `CUSTOMER_REGISTERED` |
| `yanki.customer.updated.v1` | `CustomerUpdatedEvent` | `CUSTOMER_UPDATED` |
| `yanki.customer.deleted.v1` | `CustomerDeletedEvent` | `CUSTOMER_DELETED` |
| `yanki.account.opened.v1` | `AccountOpenedEvent` | `ACCOUNT_OPENED` |
| `yanki.account.closed.v1` | `AccountClosedEvent` | `ACCOUNT_CLOSED` |
| `yanki.account.movement.registered.v1` | `AccountMovementRegisteredEvent` | `ACCOUNT_MOVEMENT` |
| `yanki.card.opened.v1` | `CardOpenedEvent` | `CARD_OPENED` |
| `yanki.card.closed.v1` | `CardClosedEvent` | `CARD_CLOSED` |
| `yanki.card.consumption.registered.v1` | `CardConsumptionRegisteredEvent` | `CARD_CONSUMPTION` |
| `yanki.debit.payment.settled.v1` | `DebitPaymentSettledEvent` | `DEBIT_PAYMENT_SETTLED` |
| `yanki.transfer.requested.v1` | `TransferRequestedEvent` | `TRANSFER_REQUESTED` |
| `yanki.transfer.settled.v1` | `TransferSettledEvent` | `TRANSFER_SETTLED` |
| `yanki.credit.opened.v1` | `CreditOpenedEvent` | `CREDIT_OPENED` |
| `yanki.credit.payment.registered.v1` | `CreditPaymentRegisteredEvent` | `CREDIT_PAYMENT` |
| `yanki.credit.overdue.v1` | `CreditOverdueEvent` | `CREDIT_OVERDUE` |
| `yanki.wallet.opened.v1` | `WalletOpenedEvent` | `WALLET_OPENED` |
| `yanki.wallet.transaction.registered.v1` | `WalletTransactionRegisteredEvent` | `WALLET_TRANSACTION` |
| `yanki.wallet.topup.requested.v1` | `WalletTopUpRequestedEvent` | `WALLET_TOPUP_REQUESTED` |
| `yanki.wallet.topup.settled.v1` | `WalletTopUpSettledEvent` | `WALLET_TOPUP_SETTLED` |

---

## 9. Modelo de datos

### Colección `product_events`
```java
// domain/model/ProductEvent.java (SIN anotaciones Spring/Mongo)
public class ProductEvent {
    String id;                    // UUID
    String eventType;             // e.g., "ACCOUNT_OPENED", "CARD_CONSUMPTION"
    String aggregateId;           // accountId, cardId, creditId, walletId, customerId
    String customerId;            // Para queries por cliente
    String productType;           // ACCOUNT, CARD, CREDIT, WALLET, CUSTOMER
    String subType;               // SAVINGS, CREDIT, PERSONAL_LOAN, etc.
    BigDecimal amount;            // Monto del evento (si aplica)
    BigDecimal balanceAfter;      // Saldo posterior (si aplica)
    Map<String, Object> metadata; // Campos adicionales del payload original
    LocalDateTime occurredAt;     // Timestamp del evento (del payload)
    LocalDateTime ingestedAt;     // Timestamp de ingesta (now)
}
```

### Colección `processed_events` (Idempotencia)
```java
// domain/model/ProcessedEvent.java
public class ProcessedEvent {
    String id; // <topic>:<aggregateId>:<occurredAt> (única)
    LocalDateTime processedAt;
}
```

### Colección `customer_summary`
```java
// domain/model/CustomerSummary.java (SIN anotaciones Spring/Mongo)
public class CustomerSummary {
    String customerId;
    String customerType;          // NATURAL, JURIDICA
    String profile;               // PERSONAL, VIP, PYME, EMPRESARIAL
    String status;                // ACTIVE, INACTIVE, BLOCKED
    Integer totalAccounts;        // Total de cuentas bancarias activas
    Integer totalCards;           // Total de tarjetas activas
    Integer totalCredits;         // Total de créditos activos
    Integer totalWallets;         // Total de wallets activas
    BigDecimal totalBalanceAccounts;  // Saldo total en cuentas
    BigDecimal totalCreditLimit;      // Límite total de crédito
    BigDecimal totalCreditDebt;       // Deuda total en créditos
    BigDecimal totalWalletBalance;    // Saldo total en wallets
    Boolean hasOverdueDebt;     // Indica si tiene deudas vencidas
    LocalDateTime lastMovementAt;
    LocalDateTime updatedAt;
}
```

### Colección `product_summary`
```java
// domain/model/ProductSummary.java (SIN anotaciones Spring/Mongo)
public class ProductSummary {
    String productId;
    String productType;           // ACCOUNT, CARD, CREDIT, WALLET
    String subType;               // SAVINGS, CREDIT, DEBIT, PERSONAL_LOAN, etc.
    String customerId;
    String status;                // OPEN, ACTIVE, CLOSED, BLOCKED, OVERDUE
    BigDecimal currentBalance;    // Saldo actual (cuentas y wallets)
    BigDecimal creditLimit;       // Límite de crédito (tarjetas)
    BigDecimal availableLimit;    // Límite disponible (tarjetas)
    BigDecimal outstandingBalance;// Saldo pendiente (créditos)
    String currency;              // PEN, USD
    LocalDateTime openedAt;
    LocalDateTime closedAt;
    LocalDateTime lastMovementAt;
    LocalDateTime updatedAt;
}
```

### Colección `daily_aggregates`
```java
// domain/model/DailyAggregate.java (SIN anotaciones Spring/Mongo)
public class DailyAggregate {
    String id;                    // Clave compuesta: YYYY-MM-DD:PRODUCT_TYPE:CUSTOMER_TYPE
    LocalDate date;
    String productType;           // ACCOUNT, CARD, CREDIT, WALLET
    String customerType;          // NATURAL, JURIDICA
    Integer movementCount;        // Cantidad total de movimientos
    BigDecimal totalAmount;       // Monto total acumulado
    BigDecimal avgAmount;         // Monto promedio por movimiento
    Integer uniqueCustomers;      // Clientes únicos con movimientos
    Integer uniqueProducts;       // Productos únicos con movimientos
    LocalDateTime updatedAt;      // Timestamp de última actualización
}
```

### Colección `customer_360` (Vista derivada)

> La vista 360° completa no tiene colección propia; se construye combinando
> `customer_summary`, `product_summary` y `product_events` en tiempo de consulta.

```java
// domain/model/Customer360.java (SIN anotaciones Spring/Mongo)
public class Customer360 {
    String customerId;
    String customerType;
    String profile;
    String status;
    Integer totalAccounts;
    Integer totalCards;
    Integer totalCredits;
    Integer totalWallets;
    BigDecimal totalBalanceAccounts;
    BigDecimal totalCreditLimit;
    BigDecimal totalCreditDebt;
    BigDecimal totalWalletBalance;
    Boolean hasOverdueDebt;
    LocalDateTime lastMovementAt;
    LocalDateTime updatedAt;
    List<ProductSummary> products;       // Productos detallados
    List<ProductEvent> recentEvents;     // Últimos 20 eventos
}
```

### Índices recomendados (MongoDB)
```javascript
db.product_events.createIndex({ "productId": 1, "occurredAt": -1 })
db.product_events.createIndex({ "aggregateId": 1, "occurredAt": -1 })
db.product_events.createIndex({ "customerId": 1, "occurredAt": -1 })
db.product_events.createIndex({ "eventType": 1, "occurredAt": -1 })
db.product_events.createIndex({ "productType": 1, "occurredAt": -1 })
db.customer_summary.createIndex({ "customerId": 1 })
db.product_summary.createIndex({ "productId": 1 })
db.product_summary.createIndex({ "customerId": 1 })
db.daily_aggregates.createIndex({ "date": 1, "productType": 1 })
```

---

## 10. Redis cache

| Clave | Contenido | TTL |
|---|---|---|
| `yanki:reports:cache:{queryHash}` | Respuesta serializada de consulta | `5m` (config `YANKI_REPORTS_CACHE_TTL`) |

### Estrategia
- Cache-aside pattern
- Lectura primero desde Redis
- Fallback a Mongo (agregaciones con `ReactiveMongoTemplate`)
- Invalidación natural por TTL
- No hay invalidación por evento individual (simplicidad + consistencia eventual)

---

## 11. Variables de entorno

| Variable | Default local | Descripción |
|---|---|---|
| `SERVER_PORT` | `8088` | Puerto HTTP |
| `MONGODB_URI` | `mongodb://localhost:27017/yanki_reports` | URI Mongo |
| `REDIS_HOST` | `localhost` | Host Redis |
| `REDIS_PORT` | `6379` | Puerto Redis |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Bootstrap servers |
| `KAFKA_GROUP_ID` | `reports-service` | Consumer group |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Eureka |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Config Server |
| `YANKI_REPORTS_CACHE_TTL` | `5m` | TTL de caché |
| `YANKI_REPORTS_LAG_THRESHOLD` | `1000` | Umbral de lag para degradación (503) |

---

## 12. Cómo ejecutar

### Desarrollo
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17.0.21.0.3"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

### Build, calidad y pruebas
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17.0.21.0.3"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn clean verify
```

### Docker
```bash
docker build -t yanki/reports-service:1.0.0 .
docker run -p 8088:8088 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/yanki_reports \
  -e REDIS_HOST=host.docker.internal \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  yanki/reports-service:1.0.0
```

### Health
```bash
curl http://localhost:8088/actuator/health
```

### OpenAPI
- Swagger UI: `http://localhost:8088/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8088/v3/api-docs`

---

## 13. Estrategia de pruebas

### Unitarias
- Servicios de dominio: `GetCustomer360Service`, `GetCardMovementsService`, `GetCustomerSummaryService`, etc.
- Mapeo DTOs y validadores de query params
- Manejo de cache (RedisCacheAdapter)

### Integración
- Controladores con `WebTestClient` (ver `ReportControllerTest.java`)
- Repositorios con `@DataMongoTest`
- Configuración infraestructura (`InfrastructureConfigTest`)

### Eventos
- `ReportsEventConsumerTest` (verificar ingesta en `product_events` y descarte duplicados en `processed_events`)
- `ProcessedEventAdapterTest` (verificar idempotencia)
- `MockFixtures.java` para datos de prueba reutilizables

### Test Cases QA
- `test-cases/` con 7 casos: CP-123 (reporte agregado, cliente, movimientos, productos), CP-124 (rango fechas inválido), CP-125 (reporte sin datos)

### Cobertura mínima documental por endpoint
Cada endpoint debe contar con:
1. Al menos un caso `200` (Happy Path)
2. Al menos un caso `400`/`404` según aplique
3. Al menos un caso `500`/`503` para condiciones operativas

---

## 14. Referencias (Regla de Oro Trazabilidad Bidireccional)

| Archivo | Sección | Descripción |
|---|---|---|
| `../../CASOS_USO/CU-13-Reportes_Bancarios.md` | Caso de uso completo | Reportes bancarios (Parte II) |
| `../../DIAGRAMAS_SECUENCIAS/SEC011_Reporte_Producto_Movimientos_Tarjeta.md` | Diagrama completo | Flujo reporte producto + últimos 10 movimientos tarjeta |
| `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123` | CP-123 | Happy Path: Reporte agregado por tipo producto |
| `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-124` | CP-124 | Happy Path: Últimos 10 movimientos tarjeta |
| `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-125` | CP-125 | Error: Parámetros inválidos en reportes |
| `../../CASOS_PRUEBA/RTM.md#cu-13` | Fila CU-13 | Trazabilidad completa CU-13 |
| `../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#reports-service` | reports-service | Matriz por microservicio |
| `../../README.md#trazabilidad-y-documentación` | Sección Trazabilidad | Índice general |
| `../../AGENTS.md` | Reglas de Oro | Trazabilidad + Arquitectura Hexagonal |
| `../../system-prompt-auditoria-hexagonal-yanki.md` | System Prompt | Auditoría arquitectura hexagonal obligatoria |
| `../docs/CASOS_PRUEBA` | Catálogo local | CPs específicos del servicio |
| `../docs/CASOS_PRUEBA` | Mocks | Datos de prueba para eventos |
| `../docs/CASOS_PRUEBA` | CP-027 | Caso de prueba local |
| `api/reports-service.yaml` | Contrato OpenAPI 3.0 | Especificación completa con schemas, endpoints y errores |

---

## 15. Estado actual vs Requerimientos

| Requerimiento (Proyecto General vf.md) | Endpoint | Estado |
|---|---|---|
| **Parte II**: "Generar un reporte completo y general por producto del banco en intervalo de tiempo especificado por el usuario" | `GET /api/reports/products?from=&to=` (streaming todos eventos) | ✅ **Completo** |
| **Parte II**: "Implementar un reporte con los últimos 10 movimientos de la tarjeta de débito y de crédito" | `GET /api/reports/movements/cards/{cardId}?limit=10` | ✅ **Completo** |
| **Parte III**: Arquitectura orientada a eventos, Kafka, sin REST entre servicios, CQRS read-only | Arquitectura completa | ✅ **Completo** |
| **Parte III**: Reportes agregados y vistas 360° | 8 endpoints implementados | ✅ **Completo** |
| **Pendiente**: Estado de cuenta paginado | `GET /api/reports/accounts/{accountId}/statement` | 🚧 Backlog |
| **Pendiente**: Plan de pagos crédito | `GET /api/reports/credits/{creditId}/schedule` | 🚧 Backlog |
| **Pendiente**: Actividad wallet | `GET /api/reports/wallets/{walletId}/activity` | 🚧 Backlog |

### Decisiones Arquitectónicas (Deuda Técnica Conocida)

| Decisión | Justificación | Estado |
|----------|---------------|--------|
| **Checkstyle no implementado** | Deuda técnica conocida. El plugin `maven-checkstyle-plugin` no se ejecuta en el build. | 📌 Deuda técnica |
| **JWT no validado localmente** | La autenticación es responsabilidad exclusiva de `api-gateway` y `auth-service`. | ✅ Por diseño |
| **Materialización de summaries** | Las colecciones `customer_summary`, `product_summary` y `daily_aggregates` no se actualizan automáticamente desde `ReportsEventConsumer`. | 📌 Backlog |
| **Endpoint `daily-aggregates` ignora `productType`** | El parámetro se recibe pero no se usa como filtro. | 📌 Mejora futura |