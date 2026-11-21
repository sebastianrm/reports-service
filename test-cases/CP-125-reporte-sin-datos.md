# CP-125 — Reporte Sin Datos en Rango Válido (Caso Borde)

> **Derivado de:** [CP-125 Global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-125--generacion-de-reporte-sin-datos-en-rango) — Adaptado a los endpoints reales

## Información General

| Campo | Valor |
|-------|-------|
| **CP Global** | CP-125 (Happy Path - Caso Borde) |
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) (Fase 2: Consulta sin resultados) |
| **Servicio** | reports-service (puerto 8088) |
| **Endpoints Afectados** | Todos los 4 endpoints |
| **Tipo** | Happy Path (caso borde / empty state) |

---

## Precondiciones

1. Usuario autenticado con rol válido.
2. Reports Service operativo.
3. Rango de fechas válido (`from <= to`, formato ISO-8601).
4. **No existen eventos** en `product_events` que coincidan con los filtros consultados.

---

## Casos de Prueba por Endpoint

### 1. `GET /api/reports/aggregated` — Sin eventos para filtros

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&from=2020-01-01&to=2020-12-31&customerType=NATURAL"
```

**Esperado:** `HTTP 200 OK`

```json
{
  "count": 0,
  "sumAmount": 0,
  "averageAmount": 0,
  "type": "ACCOUNT",
  "dateRange": {
    "from": "2020-01-01",
    "to": "2020-12-31"
  },
  "customerType": "NATURAL"
}
```

**Validaciones:**
- [ ] Status code = 200 (NO 404 — los filtros son válidos, solo no hay datos)
- [ ] `count` = 0
- [ ] `sumAmount` = 0
- [ ] `averageAmount` = 0
- [ ] Estructura completa de `AggregatedReportResponse` presente

---

### 2. `GET /api/reports/products` — Streaming vacío

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  -H "Accept: application/stream+json" \
  "http://localhost:8088/api/reports/products?from=2020-01-01&to=2020-01-31"
```

**Esperado:** `HTTP 200 OK` + `Content-Type: application/stream+json` + **cuerpo vacío** (stream sin elementos)

**Validaciones:**
- [ ] Status code = 200
- [ ] `Content-Type: application/stream+json`
- [ ] Stream se cierra sin emitir ningún objeto JSON
- [ ] No hay error, no hay 404

---

### 3. `GET /api/reports/customers/{customerId}` — Cliente sin eventos

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/customers/CLIENTE_SIN_EVENTOS_123"
```

**Esperado:** `HTTP 200 OK`

```json
{
  "customerId": "CLIENTE_SIN_EVENTOS_123",
  "products": []
}
```

**Validaciones:**
- [ ] Status code = 200 (NO 404 — diseño intencional)
- [ ] `customerId` devuelto correctamente
- [ ] `products` = array vacío `[]`
- [ ] Estructura `CustomerReportResponse` completa

---

### 4. `GET /api/reports/movements/cards/{cardId}` — Tarjeta sin movimientos

> **Diferencia clave:** Este endpoint **SÍ retorna 404** si no hay eventos para el cardId (ver notas).

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/movements/cards/CARD_INEXISTENTE_999"
```

**Esperado:** `HTTP 404 Not Found`

```json
{
  "timestamp": "2026-11-14T18:30:00",
  "status": 404,
  "error": "Not Found",
  "code": "PRODUCT_NOT_FOUND",
  "message": "No report data found for cardId CARD_INEXISTENTE_999",
  "path": "/api/reports/movements/cards/CARD_INEXISTENTE_999",
  "details": []
}
```

**Validaciones:**
- [ ] Status code = 404
- [ ] `code` = `PRODUCT_NOT_FOUND`
- [ ] `message` indica que no hay datos para el cardId

---

## Tabla Resumen de Comportamiento "Sin Datos"

| Endpoint | HTTP | Respuesta | Razón |
|----------|------|-----------|-------|
| `/api/reports/aggregated` | 200 | Objeto con ceros | Agregado válido, resultado cero |
| `/api/reports/products` | 200 | Stream vacío | Rango válido, sin eventos |
| `/api/reports/customers/{id}` | 200 | `{products: []}` | Cliente válido, sin eventos proyectados |
| `/api/reports/movements/cards/{id}` | 404 | `PRODUCT_NOT_FOUND` | Tarjeta no existe en proyección / sin eventos |

---

## Postcondiciones

- Respuestas correctas según tabla arriba.
- Métricas: `reports.queries.executed`++.
- Cache: Respuestas 200 cacheadas en Redis (TTL 5m/2m); 404 **no** se cachea.
- No modificación de datos en MongoDB.

---

## Criterios de Aceptación

| Endpoint | HTTP | Estructura | Clave |
|----------|------|------------|-------|
| Agregado | 200 | `AggregatedReportResponse` | `count=0`, `sumAmount=0`, `averageAmount=0` |
| Productos (stream) | 200 | Stream vacío | Sin elementos JSON |
| Cliente | 200 | `CustomerReportResponse` | `products=[]` |
| Movimientos Tarjeta | 404 | `ApiError` | `code=PRODUCT_NOT_FOUND` |

---

## Trazabilidad Bidireccional (Regla de Oro)

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) — Consulta sin resultados |
| **CP Global** | [CP-125](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-125--generacion-de-reporte-sin-datos-en-rango) |
| **RTM** | [RTM.md#cu-13](../../../CASOS_PRUEBA/RTM.md#cu-13) |
| **Trazabilidad Servicio** | [TRACEABILIDAD_POR_SERVICIO.md#8-reports-service](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto) |

---

## Casos Relacionados

| CP | Descripción | Relación |
|----|-------------|----------|
| [CP-123 Agregado](./CP-123-generacion-reporte-agregado-exitoso.md) | Happy Path con datos | Mismo endpoint, con datos |
| [CP-123 Productos](./CP-123-generacion-reporte-productos-exitoso.md) | Happy Path streaming con datos | Mismo endpoint, con datos |
| [CP-123 Cliente](./CP-123-generacion-reporte-cliente-exitoso.md) | Happy Path cliente con datos | Mismo endpoint, con datos |
| [CP-123 Tarjeta](./CP-123-generacion-reporte-movimientos-tarjeta-exitoso.md) | Happy Path tarjeta con datos | Mismo endpoint, con datos (200 vs 404) |
| [CP-124](./CP-124-rango-fechas-invalido.md) | Rango inválido | 400 vs 200/404 |

---

## Notas de Implementación

### Diferencia de comportamiento: 200 vs 404

| Endpoint | Sin datos = 200 | Sin datos = 404 | Justificación |
|----------|-----------------|-----------------|---------------|
| Agregado | ✅ | | Agregación matemática válida (suma=0) |
| Productos (stream) | ✅ | | Rango válido, stream vacío es respuesta correcta |
| Cliente | ✅ | | Cliente puede existir sin eventos (no es error) |
| **Movimientos Tarjeta** | | ✅ | **Identificador específico** — si no hay eventos, la tarjeta no existe en la proyección |

### Cache Behavior
- Respuestas 200 con datos vacíos **SÍ se cachean** (evita consultas repetidas a MongoDB).
- Respuestas 404 **NO se cachean** (puede ser temporal — evento aún no ingerido).
- TTL: 5m (agregado, cliente), 2m (tarjeta), sin cache (streaming productos).

### Validación en Código
```java
// ReportService.getAggregatedReport()
AggregatedReportResponse response = AggregatedReportResponse.builder()
    .count(count)  // 0 si no hay datos
    .sumAmount(sumAmount)  // BigDecimal.ZERO
    .averageAmount(count > 0 ? sumAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
    .build();

// ReportService.getCardMovements()
return repository.findByCardId(cardId)
    .switchIfEmpty(Mono.error(new ProductNotFoundException("No report data found for cardId " + cardId)));
```