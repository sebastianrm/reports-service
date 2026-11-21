# CP-123 — Generación de Reporte Agregado Exitosa

> **Derivado de:** [CP-123 Global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) — Adaptado al endpoint real `GET /api/reports/aggregated`

## Información General

| Campo | Valor |
|-------|-------|
| **CP Global** | CP-123 (Happy Path) |
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) (Fase 2: Consulta Reporte General) |
| **Servicio** | reports-service (puerto 8088) |
| **Endpoint** | `GET /api/reports/aggregated` |
| **Tipo** | Happy Path |

---

## Precondiciones

1. Usuario autenticado con rol `OPERATOR`, `ADMIN`, `ANALYST` o `AUDITOR` (validado por API Gateway via JWT).
2. Reports Service operativo: MongoDB (`yanki_reports`), Redis, Kafka (20 topics consumidos, lag ≈ 0).
3. Existen eventos proyectados en `product_events` para el tipo de producto, rango de fechas y tipo de cliente solicitados.
4. Cache Redis disponible (TTL configurado en `YANKI_REPORTS_CACHE_TTL`, default 5m).

---

## Pasos de Prueba

### 1. Solicitar reporte agregado con parámetros válidos

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&from=2026-01-01&to=2026-12-31&customerType=NATURAL"
```

**Parámetros:**
| Parámetro | Valor | Obligatorio |
|-----------|-------|-------------|
| `type` | `ACCOUNT` (o `CARD`, `CREDIT`, `WALLET`) | ✅ Sí |
| `from` | `2026-01-01` (ISO-8601 date) | ✅ Sí |
| `to` | `2026-12-31` (ISO-8601 date) | ✅ Sí |
| `customerType` | `NATURAL` (o `JURIDICA`) | ✅ Sí |

---

### 2. Validar respuesta exitosa

**Esperado:** `HTTP 200 OK`

```json
{
  "count": 120,
  "sumAmount": 24000.50,
  "averageAmount": 200.00,
  "type": "ACCOUNT",
  "dateRange": {
    "from": "2026-01-01",
    "to": "2026-12-31"
  },
  "customerType": "NATURAL"
}
```

**Validaciones:**
- [ ] Status code = 200
- [ ] `count` > 0 (cantidad de eventos en el rango)
- [ ] `sumAmount` ≥ 0 (suma total de montos)
- [ ] `averageAmount` = `sumAmount` / `count` (redondeado a 2 decimales)
- [ ] `type` coincide con el solicitado
- [ ] `dateRange.from` y `dateRange.to` coinciden con los parámetros
- [ ] `customerType` coincide con el solicitado
- [ ] Header `Content-Type: application/json`
- [ ] Respuesta en < 2s (Resilience4j timeout)

---

### 3. Verificar uso de Cache Redis (Opcional - Cache HIT)

1. Repetir la misma petición inmediatamente.
2. Verificar que la respuesta proviene de cache (latencia significativamente menor).
3. Métrica `reports.cache.hit_ratio` incrementada.

---

### 4. Verificar métricas de observabilidad

```bash
# Verificar contador de consultas
curl -s http://localhost:8088/actuator/metrics/reports.queries.executed | jq '.measurements[] | select(.statistic=="COUNT") | .value'

# Verificar latencia
curl -s http://localhost:8088/actuator/metrics/reports.query.latency | jq '.measurements[] | select(.statistic=="MEAN") | .value'
```

---

## Postcondiciones

- Reporte agregado generado y devuelto correctamente.
- Métricas actualizadas: `reports.queries.executed`++, `reports.query.latency` registrada.
- Si era Cache MISS: datos almacenados en Redis con TTL 5m.
- No se modifican datos en MongoDB (solo lectura).

---

## Criterios de Aceptación

| Criterio | Resultado Esperado |
|----------|-------------------|
| Código HTTP | 200 OK |
| Estructura JSON | Coincide con `AggregatedReportResponse` |
| Valores numéricos | Correctos según datos en `product_events` |
| Filtros aplicados | `type`, `from`, `to`, `customerType` respetados |
| Rendimiento | < 2s (timeout Resilience4j) |
| Seguridad | Requiere JWT válido con rol autorizado |

---

## Trazabilidad Bidireccional (Regla de Oro)

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) — Sección 8.1 |
| **Diagrama Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) — Fase 2 |
| **CP Global** | [CP-123](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) |
| **RTM** | [RTM.md#cu-13](../../../CASOS_PRUEBA/RTM.md#cu-13) |
| **Trazabilidad Servicio** | [TRACEABILIDAD_POR_SERVICIO.md#8-reports-service](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto) |
| **Endpoint Implementado** | [README.md#51-get-apireportsaggregated](../README.md#51-get-apireportsaggregated) |
| **Controller Test** | [ReportControllerTest.java#testGetAggregatedReport_Success](../../src/test/java/com/yanki/reports/ReportControllerTest.java#L41) |

---

## Casos Relacionados

| CP | Descripción | Relación |
|----|-------------|----------|
| [CP-124](./CP-124-rango-fechas-invalido.md) | Rango de fechas inválido | Mismo endpoint, caso error |
| [CP-125](./CP-125-reporte-sin-datos.md) | Sin datos en rango válido | Mismo endpoint, caso borde |
| [CP-123 Productos](./CP-123-generacion-reporte-productos-exitoso.md) | Reporte eventos por rango | Otro endpoint (streaming) |
| [CP-123 Cliente](./CP-123-generacion-reporte-cliente-exitoso.md) | Vista 360° cliente | Otro endpoint |
| [CP-123 Tarjeta](./CP-123-generacion-reporte-movimientos-tarjeta-exitoso.md) | Últimos 10 mov. tarjeta | Req. Parte II obligatorio |

---

## Notas de Implementación

- **Arquitectura**: CQRS read-only — Consulta `product_events` via `ReactiveMongoTemplate` + Aggregation Framework.
- **Cache**: Cache-aside con Redis, clave `yanki:reports:cache:{hash(query)}`, TTL 5m.
- **Degradación**: Si Kafka lag > `YANKI_REPORTS_LAG_THRESHOLD` (default 1000) → HTTP 503 `KAFKA_LAG_HIGH`.
- **Idempotencia**: No aplica (solo lectura), pero la proyección subyacente usa `processed_events` para deduplicar ingestión.