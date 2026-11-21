# Colección Postman — reports-service

Colección completa de pruebas para el microservicio **reports-service** (puerto 8088).

## 📋 Resumen

| Métrica | Valor |
|---------|-------|
| **Endpoints cubiertos** | 7 de 7 (100%) |
| **Requests totales** | 20 |
| **Casos Happy Path (CP-123)** | 7 requests |
| **Casos Error (CP-124)** | 4 requests |
| **Casos Borde (CP-125)** | 7 requests |
| **Errores del Sistema (5xx)** | 2 requests |
| **Schemas de Referencia** | 8 schemas |

## 📂 Estructura de la Colección

```
Yanki - Reports Service
├── 1. CP-123 — Happy Path (7 requests)
│   ├── 1.1 Reporte Agregado — Happy Path
│   ├── 1.2 Eventos de Producto por Rango — Happy Path (Streaming)
│   ├── 1.3 Reporte Cliente (Vista 360°) — Happy Path
│   ├── 1.4 Últimos Movimientos de Tarjeta — Happy Path (Req. Parte II)
│   ├── 1.5 Resumen del Cliente (Customer Summary) — Happy Path
│   ├── 1.6 Resumen del Producto (Product Summary) — Happy Path
│   └── 1.7 Agregados Diarios (Daily Aggregates) — Happy Path
│
├── 2. CP-124 — Rango de Fechas Inválido (Error) (4 requests)
│   ├── 2.1 Reporte Agregado — from > to (Fecha inválida)
│   ├── 2.2 Eventos de Producto — from > to (Fecha inválida)
│   ├── 2.3 Reporte Agregado — Tipo de producto inválido
│   └── 2.4 Reporte Agregado — Tipo de cliente inválido
│
├── 3. CP-125 — Sin Datos en Rango Válido (Caso Borde) (7 requests)
│   ├── 3.1 Reporte Agregado — Sin eventos (count = 0)
│   ├── 3.2 Eventos de Producto — Stream vacío
│   ├── 3.3 Reporte Cliente — Sin eventos (products = [])
│   ├── 3.4 Movimientos de Tarjeta — Tarjeta no encontrada (404)
│   ├── 3.5 Customer Summary — Cliente no encontrado (404)
│   ├── 3.6 Product Summary — Producto no encontrado (404)
│   └── 3.7 Agregados Diarios — Sin datos para la fecha
│
├── 4. Errores del Sistema (5xx) (2 requests)
│   ├── 4.1 Lag de Kafka Alto — Servicio no disponible (503)
│   └── 4.2 Error Interno del Servidor (500)
│
└── 5. Schemas de Referencia (8 schemas)
    ├── Schema — ApiError (Error estándar)
    ├── Schema — AggregatedReportResponse
    ├── Schema — ProductEventResponse
    ├── Schema — CustomerReportResponse
    ├── Schema — CardMovementsResponse
    ├── Schema — CustomerSummaryResponse
    ├── Schema — ProductSummaryResponse
    └── Schema — DailyAggregateResponse
```

## 🔧 Variables de Colección

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `base_url` | `http://localhost:8088` | URL base del reports-service |
| `auth_token` | `` (vacío) | Token JWT Bearer (delegado al API Gateway) |
| `customer_id` | `6af39f59b089e973a5ffe9b6` | ID de cliente de prueba |
| `card_id` | `CARD-789` | ID de tarjeta de prueba |
| `product_id` | `ACC-12345` | ID de producto de prueba |

## 🚀 Cómo Usar

### 1. Importar la Colección

1. Abre Postman
2. **File → Import** → Selecciona `Yanki-Reports-Service.postman_collection.json`
3. La colección se cargará con todas las variables y requests configurados

### 2. Configurar el Entorno

- Si el reports-service corre en otro puerto, cambia la variable `base_url`.
- Si tienes un token JWT del auth-service, colócalo en `auth_token`.
- En desarrollo local con bypass de autenticación, deja `auth_token` vacío.

### 3. Ejecutar los Tests

Cada request incluye una respuesta guardada con el cuerpo esperado y las validaciones documentadas en la sección "Validaciones Postman test" de cada respuesta.

Para agregar tests automatizados en Postman, usa el panel **Tests** de cada request con scripts como:

```javascript
// Ejemplo: Validar 200 OK
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// Ejemplo: Validar schema de respuesta
pm.test("Response has required fields", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("count");
    pm.expect(jsonData).to.have.property("sumAmount");
    pm.expect(jsonData).to.have.property("averageAmount");
});

// Ejemplo: Validar error 400
pm.test("Status code is 400", function () {
    pm.response.to.have.status(400);
});

pm.test("Error code is INVALID_DATE_RANGE", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql("INVALID_DATE_RANGE");
});
```

## ✅ Validación Cruzada con el Código

### Parámetros de los Endpoints

Cada request valida que los parámetros coinciden con `ReportController.java`:

| Endpoint | Parámetros en Controller | Parámetros en Postman | Coincide |
|----------|-------------------------|----------------------|----------|
| `GET /aggregated` | `type`, `from`, `to`, `customerType` (Query) | `type`, `from`, `to`, `customerType` (Query) | ✅ |
| `GET /products` | `from`, `to` (Query) | `from`, `to` (Query) | ✅ |
| `GET /customers/{customerId}` | `customerId` (Path) | `{{customer_id}}` (Path) | ✅ |
| `GET /movements/cards/{cardId}` | `cardId` (Path), `limit` (Query, default=10) | `{{card_id}}` (Path), `limit=10` (Query) | ✅ |
| `GET /customer-summary/{customerId}` | `customerId` (Path) | `{{customer_id}}` (Path) | ✅ |
| `GET /products/{productId}` | `productId` (Path) | `{{product_id}}` (Path) | ✅ |
| `GET /daily-aggregates` | `date` (Query), `productType` (Query, optional) | `date`, `productType` (Query) | ✅ |

### Códigos de Error

Cada error valida que el código y mensaje coinciden con `GlobalExceptionHandler.java`:

| Excepción | Code | HTTP | Mensaje | Validado en Postman |
|-----------|------|------|---------|---------------------|
| `InvalidDateRangeException` | `INVALID_DATE_RANGE` | 400 | "Fecha inicial (from) debe ser anterior o igual a fecha final (to)" | ✅ 2.1, 2.2 |
| `InvalidDateRangeException` | `INVALID_DATE_RANGE` | 400 | "Tipo de producto no soportado: {type}. Valores: ACCOUNT, CARD, CREDIT, WALLET" | ✅ 2.3 |
| `InvalidDateRangeException` | `INVALID_DATE_RANGE` | 400 | "Tipo de cliente no válido: {type}. Valores: NATURAL, JURIDICA" | ✅ 2.4 |
| `ProductNotFoundException` | `PRODUCT_NOT_FOUND` | 404 | "No report data found for {resourceType} {resourceId}" | ✅ 3.4, 3.5, 3.6 |
| `KafkaLagHighException` | `KAFKA_LAG_HIGH` | 503 | "Proyección degradada: consumer lag ({lag}) supera umbral ({threshold})" | ✅ 4.1 |
| `Exception` (genérico) | `REPORT_GENERATION_FAILED` | 500 | "Error interno generando el reporte" | ✅ 4.2 |
| `ConstraintViolationException` | `INVALID_PARAMETER` | 400 | Mensaje de validación | 📝 Documentado |

### Rutas de los Endpoints

| Ruta en Controller | Ruta en Postman | Coincide |
|--------------------|-----------------|----------|
| `/api/reports/aggregated` | `/api/reports/aggregated` | ✅ |
| `/api/reports/products` | `/api/reports/products` | ✅ |
| `/api/reports/customers/{customerId}` | `/api/reports/customers/{{customer_id}}` | ✅ |
| `/api/reports/movements/cards/{cardId}` | `/api/reports/movements/cards/{{card_id}}` | ✅ |
| `/api/reports/customer-summary/{customerId}` | `/api/reports/customer-summary/{{customer_id}}` | ✅ |
| `/api/reports/products/{productId}` | `/api/reports/products/{{product_id}}` | ✅ |
| `/api/reports/daily-aggregates` | `/api/reports/daily-aggregates` | ✅ |

### Schemas de Respuesta

Cada schema valida que los campos coinciden con los DTOs:

| DTO | Campos en Java | Campos en Postman | Coincide |
|-----|----------------|-------------------|----------|
| `AggregatedReportResponse` | count, sumAmount, averageAmount, type, dateRange, customerType | count, sumAmount, averageAmount, type, dateRange, customerType | ✅ |
| `ProductEventResponse` | id, eventType, aggregateId, customerId, productType, subType, amount, balanceAfter, metadata, occurredAt, ingestedAt | id, eventType, aggregateId, customerId, productType, subType, amount, balanceAfter, metadata, occurredAt, ingestedAt | ✅ |
| `CustomerReportResponse` | customerId, products[] | customerId, products[] | ✅ |
| `CardMovementsResponse` | cardId, productType, subType, movements[] | cardId, productType, subType, movements[] | ✅ |
| `CustomerSummaryResponse` | 14 campos (customerId, customerType, profile, status, totalAccounts, etc.) | 14 campos | ✅ |
| `ProductSummaryResponse` | 14 campos (productId, productType, subType, customerId, status, etc.) | 14 campos | ✅ |
| `DailyAggregateResponse` | 10 campos (id, date, productType, customerType, movementCount, etc.) | 10 campos | ✅ |
| `ApiError` | timestamp, status, error, code, message, path, details | timestamp, status, error, code, message, path, details | ✅ |

## 📊 Cobertura de Casos de Prueba

| CP | Tipo | Requests en Postman | Endpoints Cubiertos |
|----|------|---------------------|---------------------|
| **CP-123** | Happy Path | 7 | /aggregated, /products, /customers/{id}, /movements/cards/{id}, /customer-summary/{id}, /products/{id}, /daily-aggregates |
| **CP-124** | Error | 4 | /aggregated (from>to, type inválido, customerType inválido), /products (from>to) |
| **CP-125** | Caso Borde | 7 | /aggregated (count=0), /products (stream vacío), /customers/{id} (products=[]), /movements/cards/{id} (404), /customer-summary/{id} (404), /products/{id} (404), /daily-aggregates (array vacío) |

## 🔗 Trazabilidad Bidireccional

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios](../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) |
| **Casos de Prueba** | [CP-123](../test-cases/CP-123-generacion-reporte-agregado-exitoso.md), [CP-124](../test-cases/CP-124-rango-fechas-invalido.md), [CP-125](../test-cases/CP-125-reporte-sin-datos.md) |
| **Controller** | [ReportController.java](../src/main/java/com/yanki/reports/interfaces/rest/ReportController.java) |
| **GlobalExceptionHandler** | [GlobalExceptionHandler.java](../src/main/java/com/yanki/reports/interfaces/rest/GlobalExceptionHandler.java) |
| **RTM** | [RTM.md](../../CASOS_PRUEBA/RTM.md#cu-13) |

## ⚠️ Notas Importantes

1. **Autenticación JWT:** La autenticación se delega al API Gateway. En desarrollo local, la variable `auth_token` puede estar vacía si el gateway está configurado para bypass.

2. **Streaming NDJSON:** El endpoint `/api/reports/products` retorna `application/x-ndjson` (Server-Sent Events). Postman puede no mostrar el streaming en tiempo real; usa la respuesta guardada como referencia.

3. **Comportamiento 200 vs 404:**
   - `/aggregated` sin datos → 200 con ceros (agregación válida)
   - `/products` sin datos → 200 con stream vacío (rango válido)
   - `/customers/{id}` sin datos → 200 con `products: []` (cliente válido)
   - `/movements/cards/{id}` sin datos → 404 `PRODUCT_NOT_FOUND` (tarjeta no existe)

4. **Parámetro `productType` en daily-aggregates:** Se recibe pero el endpoint actualmente ignora el filtro y siempre retorna todos los tipos. Documentado en la descripción del request 1.7.

5. **Errores 5xx:** Los casos 4.1 (Kafka lag) y 4.2 (Error interno) se validan en tests unitarios, no en pruebas manuales. Se incluyen para documentación del esquema de error.
