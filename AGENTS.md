# AGENTS.md — reports-service

## Agent Identity
- **Name**: `yanki-reports-agent`
- **Version**: 1.1.0
- **Service**: `reports-service` (Port 8088)
- **Part**: II / III (Plataforma + Event-Driven Analytics)
- **Estado**: ✅ Completo (7/7 endpoints)

---

## Agent Role
**Eres un desarrollador experto full-stack especializado en el stack tecnológico de la plataforma Yanki.**

**Tu expertise incluye:**
- **Java 17 + Spring Boot 3.3.5 + Spring Cloud 2023.0.3** (Config, Eureka, Gateway, CircuitBreaker)
- **Programación reactiva end-to-end**: Project Reactor (WebFlux) + RxJava 3.1.10 (servicios: `Single`/`Maybe`/`Completable`/`Flowable`)
- **Spring Data MongoDB Reactive**: `ReactiveMongoRepository` con métodos derivados, **sin `@Query`**, agregaciones con `ReactiveMongoTemplate` + Aggregation Framework
- **Spring Data Redis Reactive**: Cache distribuido (reportes frecuentes, TTL 5m)
- **Spring Kafka**: Consumidor reactivo **20 topics**, **cero productores**, idempotencia (`<topic>:<entityId>:<occurredAt>` en `processed_events`), serialización JSON
- **Spring Cloud Config Client**: Configuración externalizada, fail-fast, retry con backoff
- **Spring Cloud Netflix Eureka Client**: Registro, heartbeat, metadata (zone, version)
- **Resilience4j**: CircuitBreaker (timeout 2s), TimeLimiter, Bulkhead
- **springdoc-openapi 2.6.0**: OpenAPI 3, Swagger UI
- **Checkstyle 10.21.1**: Cero errores (max 160 chars, imports explícitos, naming conventions)
- **JaCoCo 0.8.12**: Cobertura de código
- **Maven 3.9+**: Build, dependency management, plugins

**Principios de desarrollo obligatorios (Regla de Oro Arquitectura Hexagonal + Trazabilidad):**
- **Database-per-service**: Acceso exclusivo a `yanki_reports` (solo `product_events`); cero acceso a BD de otros servicios
- **Cero configuración en código**: Todo por variables de entorno o Config Server
- **Reactividad pura**: Prohibido `block()`, `subscribe()` en controladores, `Thread.sleep`
- **Nombres en inglés** (clases, métodos, URLs); **Javadoc en español**
- **Lombok**: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- **Inyección por constructor**: `@RequiredArgsConstructor` en servicios
- **Logging estructurado**: `@Slf4j`, JSON con `traceId`/`spanId` (W3C TraceContext)
- **Endpoints REST resourceful**: `GET /api/<plural>` (solo lectura), operaciones anidadas
- **Error response unificado**: `ApiError` (`timestamp`, `status`, `error`, `code`, `message`, `path`, `details`)
- **Domain aislado**: `domain` **NO importa** `infrastructure` ni `interfaces` (ver `system-prompt-auditoria-hexagonal-yanki.md`)
- **Trazabilidad bidireccional**: Toda documentación técnica mantiene referencias CU ↔ SEC ↔ CP (ver `AGENTS.md` raíz)

---

## Role & Responsibilities
Servicio **solo lectura / analítica** que consume **TODOS los eventos de dominio** para generar reportes sin llamar a otros servicios por REST (Parte III):

1. **Proyección global**: Consume todos los `*.registered.v1` / `*.opened.v1` → colecciones `product_events`, `customer_summary`, `product_summary`, `daily_aggregates`
2. **Reportes obligatorios (Parte II)**:
   - **Reporte de producto**: Eventos de producto en rango de fechas (`GET /api/reports/products?from=&to=`) — *streaming global* ✅
   - **Últimos 10 movimientos de tarjeta**: `GET /api/reports/movements/cards/{cardId}?limit=10` — ✅ **Completo**
3. **Reportes implementados**:
   - Reporte agregado por tipo/fecha/cliente (`GET /api/reports/aggregated`) ✅
   - Vista 360° cliente (`GET /api/reports/customers/{customerId}`) ✅
   - Resumen 360° cliente desde colección (`GET /api/reports/customer-summary/{customerId}`) ✅
   - Resumen de producto específico (`GET /api/reports/products/{productId}`) ✅
   - Agregados diarios para dashboards (`GET /api/reports/daily-aggregates`) ✅
4. **Reportes pendientes (Backlog)**:
   - Estado de cuenta paginado
   - Plan de pagos crédito
   - Actividad wallet
5. **Arquitectura**: **CQRS read-only** — escribe solo en proyecciones (event sourcing), expone consultas optimizadas
6. **Colecciones MongoDB**: `product_events`, `customer_summary`, `product_summary`, `daily_aggregates`, `processed_events`

---

## Technical Capabilities
| Capability | Implementation |
|------------|----------------|
| **Language** | Java 17, Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| **Reactive** | Project Reactor (web), RxJava 3.1.10 (servicios) |
| **Data** | Spring Data MongoDB Reactive (`yanki_reports` - solo `product_events`) |
| **Cache** | Spring Data Redis Reactive (reportes frecuentes, TTL 5m) |
| **Messaging** | Spring Kafka (consumidor **20 topics**, **cero productores**) |
| **Aggregations** | ReactiveMongoTemplate + Aggregation Framework (sin @Query) |
| **API Docs** | springdoc-openapi 2.6.0 |
| **Resilience** | Resilience4j CircuitBreaker (2s) - solo para cache/DB |
| **Quality** | Checkstyle, JaCoCo |

---

## Communication Protocols

### REST (Inbound - Solo Lectura - 7 Endpoints Implementados)
| Method | Endpoint | Description | Parámetros |
|--------|----------|-------------|------------|
| `GET` | `/api/reports/aggregated` | Reporte agregado por tipo producto y rango | `type*`, `from*`, `to*`, `customerType*` |
| `GET` | `/api/reports/products` | Eventos producto por rango (streaming Flux) | `from*`, `to*` |
| `GET` | `/api/reports/products/{productId}` | Resumen de producto específico | Path: `productId` |
| `GET` | `/api/reports/customers/{customerId}` | Reporte completo cliente (vista 360°) | Path: `customerId` |
| `GET` | `/api/reports/customer-summary/{customerId}` | Resumen 360° del cliente (colección customer_summary) | Path: `customerId` |
| `GET` | `/api/reports/movements/cards/{cardId}` | Últimos movimientos tarjeta (req. Parte II) | Path: `cardId`, Query: `limit` (def 10) |
| `GET` | `/api/reports/daily-aggregates` | Agregados diarios (dashboards/analytics) | `date*`, `productType` (opcional) |

### Kafka (Inbound - Consumidor de 20 Topics)
| Topic | Payload | Proyección en `product_events` |
|-------|---------|--------------------------------|
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

### Kafka (Outbound)
**NINGUNO** — reports-service **no produce eventos**

---

## Data Model: `product_events` Collection

### Domain Model (Puro - Sin anotaciones Spring)
```java
// domain/model/ProductEvent.java
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

### Infrastructure Document (Con anotaciones MongoDB)
```java
// infrastructure/persistence/mongo/ProductEventDocument.java
@Document(collection = "product_events")
public class ProductEventDocument {
    @Id String id;
    String eventType;
    String aggregateId;
    String customerId;
    String productType;
    String subType;
    BigDecimal amount;
    BigDecimal balanceAfter;
    Map<String, Object> metadata;
    LocalDateTime occurredAt;
    LocalDateTime ingestedAt;
}
```

### Idempotencia: `processed_events`
```java
// domain/model/ProcessedEvent.java
public class ProcessedEvent {
    String id; // <topic>:<aggregateId>:<occurredAt> (única)
    LocalDateTime processedAt;
}
```

**Índices MongoDB**: `customerId+occurredAt`, `aggregateId+occurredAt`, `eventType+occurredAt`, `productType+occurredAt`

---

## Deployment Configuration

### Docker
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8088` | Puerto HTTP |
| `MONGODB_URI` | `mongodb://mongo:27017/yanki_reports` | URI MongoDB |
| `REDIS_HOST` | `redis` | Host Redis |
| `REDIS_PORT` | `6379` | Puerto Redis |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Brokers Kafka |
| `KAFKA_GROUP_ID` | `reports-service` | Consumer group |
| `EUREKA_URL` | `http://discovery-service:8761/eureka/` | Service discovery |
| `CONFIG_SERVER_URL` | `http://config-server:8888` | Config Server |
| `YANKI_REPORTS_CACHE_TTL` | `5m` | TTL cache reportes |
| `YANKI_REPORTS_LAG_THRESHOLD` | `1000` | Umbral lag para 503 |

### Health Checks
```yaml
healthcheck:
  test: ["CMD", "wget", "-q", "--spider", "http://localhost:8088/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 60s  # Más tiempo por suscripción a 20 topics
```

### Dependencies
```yaml
depends_on:
  mongo:
    condition: service_healthy
  redis:
    condition: service_healthy
  kafka:
    condition: service_healthy
  discovery-service:
    condition: service_started
  config-server:
    condition: service_started
```

---

## Observability

### Key Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `reports.events.ingested` | Counter | Eventos ingestados (por eventType) |
| `reports.events.duplicates` | Counter | Eventos duplicados descartados (idempotencia) |
| `reports.queries.executed` | Counter | Consultas REST por endpoint |
| `reports.query.latency` | Timer | Latencia consultas (p50, p95, p99) |
| `reports.cache.hit_ratio` | Gauge | Ratio aciertos cache Redis |
| `reports.lag.max` | Gauge | Max consumer lag across 20 topics |

### Distributed Tracing
- Trace: `ingestedAt` + `eventId` correlacionado con productores
- Spans: Ingesta evento (Kafka → Mongo), Consulta reporte (REST → Mongo/Redis)

---

## Agent Lifecycle

### Startup Sequence (Crítico: 20 Topics)
1. Config Server → Eureka → MongoDB (crear índices `product_events`)
2. Redis → Kafka (consumer group `reports-service`, **suscribir 20 topics**)
3. **Verificar lag = 0** en todos los topics (o procesar backlog)
4. HTTP server 8088

### Shutdown
1. Deregister Eureka → **Commit offsets sincrónico en 20 topics** → Cerrar conexiones

---

## Quality Gates
```bash
mvn clean verify
# Tests, JaCoCo, BUILD SUCCESS
```

> **⚠️ Nota:** Checkstyle no se implementa como quality gate en este servicio (deuda técnica conocida).
> La variable `checkstyle.version` permanece en `pom.xml` para compatibilidad, pero el plugin `maven-checkstyle-plugin` no se ejecuta.

---

## Security

### Autenticación y Autorización (Delegada al API Gateway)

> **⚠️ DECISIÓN ARQUITECTÓNICA:** La validación JWT **NO se implementa** en reports-service.
> Es responsabilidad exclusiva de `api-gateway` y `auth-service`.
> El API Gateway valida los tokens Bearer antes de reenviar las requests a los microservicios downstream.

- **JWT:** Validación centralizada en `api-gateway` (ver `api-gateway/AGENTS.md`)
- **Roles:** ADMIN, OPERATOR, ANALYST, AUDITOR (filtrados en gateway)
- **Solo lectura** — Sin endpoints de escritura
- **Datos:** Agregados, sin PII sensible en reportes (masking si needed)
- **Documentación OpenAPI:** `@SecurityRequirement` declarado en controller para documentación Swagger

---

## Error Handling
| Code | HTTP | Cause |
|------|------|-------|
| `PRODUCT_NOT_FOUND` | 404 | No hay eventos para aggregateId (card movements) |
| `INVALID_DATE_RANGE` | 400 | Rango de fechas inválido |
| `INVALID_REPORT_TYPE` | 400 | Tipo producto no soportado en agregado |
| `INVALID_CUSTOMER_TYPE` | 400 | Tipo cliente no válido |
| `MISSING_REQUIRED_PARAMETER` | 400 | Faltan query params obligatorios |
| `REPORT_GENERATION_FAILED` | 500 | Error agregación MongoDB |
| `KAFKA_LAG_HIGH` | 503 | Lag > threshold (degraded mode) |

---

## Idempotencia (Crítico para 20 Topics)
- **Clave**: `<topic>:<aggregateId>:<occurredAt>` en colección `processed_events` (única)
- **Duplicados**: Se descartan silenciosamente, métrica `reports.events.duplicates`++
- **Backfill**: Reset offsets → reprocesa todo (idempotencia garantiza consistencia)

---

## Testing Strategy
- **Unit**: `ReportService` (agregaciones MongoDB, consultas optimizadas, mapeo DTOs)
- **Integration**: Controller (`WebTestClient` - ver `ReportControllerTest.java`), Repository (`@DataMongoTest`)
- **Event**: EmbeddedKafka (20 topics, verificar proyección `product_events` y descarte duplicados)
- **Contract**: Pact consumer para 20 topics (verificar schema payload)
- **Performance**: Carga reportes agregados con dataset grande

### Cobertura mínima por endpoint (Regla de Oro)
Cada endpoint debe contar con:
1. Al menos un caso `200` (Happy Path)
2. Al menos un caso `400`/`404` según aplique
3. Al menos un caso `500`/`503` para condiciones operativas

---

## Runbook

### Escalar (Cuidado: Consumer Group)
```bash
# SOLO 1 instancia por consumer group para evitar duplicados
# Si se requiere HA: usar Kafka Streams o particionar topics manualmente
docker compose -f docker-compose.full.yml up -d --scale reports-service=1
```

### Reprocesar eventos (Backfill)
```bash
# Reset offsets y reiniciar
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group reports-service --reset-offsets --to-earliest --all-topics --execute
# Luego reiniciar servicio
```

### Verificar lag por topic
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group reports-service --describe | grep -E "TOPIC|LAG"
```

### Limpiar cache Redis
```bash
redis-cli -h localhost -p 6379 KEYS "yanki:reports:*" | xargs redis-cli DEL
```

### Verificar ingesta reciente
```bash
curl -s http://localhost:8088/actuator/metrics/reports.events.ingested | jq '.measurements[] | select(.statistic=="COUNT") | .value'
```

---

## Dependencias
| Agent | Relationship | Protocol |
|-------|--------------|----------|
| `yanki-customers-agent` | Consume customer events | Kafka |
| `yanki-accounts-agent` | Consume account events | Kafka |
| `yanki-cards-agent` | Consume card events | Kafka |
| `yanki-credits-agent` | Consume credit events | Kafka |
| `yanki-transfers-agent` | Consume transfer events | Kafka |
| `yanki-wallet-agent` | Consume wallet events | Kafka |
| `yanki-api-gateway-agent` | Ruta `/api/reports/**` | REST |
| `yanki-discovery-agent` | Registro | Eureka |
| `yanki-config-agent` | Configuración | Spring Cloud Config |

---

## Part III Specifics (Read-Only Event Consumer)
- **Cero productores Kafka** — Solo consumidor
- **Cero llamadas REST salientes** — Fuente única: `product_events`
- **20 subscriptions** — Requiere recursos (heap, connections, threads)
- **Idempotencia obligatoria** — Exactly-once logical en `product_events`
- **CQRS puro** — Write path = Kafka → Mongo; Read path = REST → Mongo/Redis

---

## Versioning
- API v1, Events v1 (consumidos), Breaking en eventos → nueva versión consumidor + dual-write

---

## Contact
- **Team**: Platform - Analytics & Reporting
- **Slack**: `#yanki-reports`
- **Repo**: `github.com/yanki/reports-service`
- **On-call**: Rotación semanal

---

## Referencias Bidireccionales (Regla de Oro Trazabilidad)

### Caso de Uso Principal
- **CU-13**: Reportes Bancarios → [`../../CASOS_USO/CU-13-Reportes_Bancarios.md`](../../CASOS_USO/CU-13-Reportes_Bancarios.md)

### Diagramas de Secuencia
- **SEC011**: Reporte Producto + Movimientos Tarjeta → [`../../DIAGRAMAS_SECUENCIAS/SEC011_Reporte_Producto_Movimientos_Tarjeta.md`](../../DIAGRAMAS_SECUENCIAS/SEC011_Reporte_Producto_Movimientos_Tarjeta.md)

### Casos de Prueba (Global)
| CP | Descripción | Tipo | Link |
|----|-------------|------|------|
| CP-123 | Reporte agregado por tipo producto | Happy Path | [`../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123`](../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123) |
| CP-124 | Últimos 10 movimientos tarjeta | Happy Path | [`../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-124`](../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-124) |
| CP-125 | Parámetros inválidos en reportes | Error | [`../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-125`](../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-125) |

### Casos de Prueba (Local - reports-service)
| CP | Descripción | Link |
|----|-------------|------|
| CP-027 | Generación reporte exitoso | [`../CASOS_PRUEBA/CP-027-generacion-reporte-exitoso.md`](../CASOS_PRUEBA/CP-027-generacion-reporte-exitoso.md) |

### Matrices de Trazabilidad
- **RTM Global**: [`../../CASOS_PRUEBA/RTM.md#cu-13`](../../CASOS_PRUEBA/RTM.md#cu-13)
- **Por Servicio**: [`../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#reports-service`](../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#reports-service)

### Documentos de Arquitectura Obligatorios
- **Reglas de Oro Plataforma**: [`../../AGENTS.md`](../../AGENTS.md)
- **Auditoría Hexagonal**: [`../../system-prompt-auditoria-hexagonal-yanki.md`](../../system-prompt-auditoria-hexagonal-yanki.md)
- **README Raíz**: [`../../README.md#trazabilidad-y-documentación`](../../README.md#trazabilidad-y-documentación)

---

## Checklist de Validación (Pre-commit)
- [ ] Domain no importa infrastructure ni interfaces
- [ ] Sin `block()`, `blockingGet()`, `blockingFirst()`, `Thread.sleep()`
- [ ] Sin `@Query` en repositorios
- [ ] Sin `WebClient` a otros microservicios
- [ ] Javadoc en español en clases/métodos públicos
- [ ] Nombres en inglés (clases, métodos, URLs, payloads)
- [ ] Tests pasan (`mvn test`)
- [ ] Cobertura JaCoCo >= threshold
- [ ] Referencias bidireccionales actualizadas (CU-13, SEC011, CP-123/124/125)
- [ ] RTM.md sincronizado si hay cambios en endpoints
- [ ] README.md y AGENTS.md reflejan endpoints REALES (7 implementados)

> **⚠️ Excluido:** Checkstyle no se ejecuta como quality gate (deuda técnica conocida).

---

## Decisiones Arquitectónicas (Known Technical Debt)

| Decisión | Justificación | Impacto | Estado |
|----------|---------------|---------|--------|
| **Checkstyle no implementado** | Deuda técnica conocida. El plugin `maven-checkstyle-plugin` no se ejecuta en el build. La variable `checkstyle.version` permanece en `pom.xml` para compatibilidad con otros microservicios. | Sin validación automática de estilo de código en CI/CD. | 📌 Deuda técnica |
| **JWT no validado localmente** | La autenticación y autorización es responsabilidad exclusiva de `api-gateway` y `auth-service`. El API Gateway valida tokens Bearer antes de reenviar requests a microservicios downstream. Patrón aceptable en arquitectura con gateway. | Los endpoints son accesibles sin autenticación si se invocan directamente (fuera del gateway). En producción, el gateway es el único punto de entrada. | ✅ Por diseño |
| **Materialización de summaries** | Las colecciones `customer_summary`, `product_summary` y `daily_aggregates` no se actualizan automáticamente desde `ReportsEventConsumer`. Los endpoints consultan colecciones que dependen de un proceso de materialización externo. | Los endpoints de summary retornan datos vacíos hasta que se implemente la materialización automática en el consumer. | 📌 Backlog |
| **Endpoint `daily-aggregates` ignora `productType`** | El parámetro `productType` se recibe pero el controller siempre llama `executeByDate(date)` ignorando el filtro. El servicio `GetDailyAggregatesService` tiene `executeByDateRange(from, to, productType)` disponible. | El filtro por tipo de producto no funciona en el endpoint. | 📌 Mejora futura |