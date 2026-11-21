# CP-123 — Generación de Reporte de Eventos por Rango (Streaming) Exitoso

> **Derivado de:** [CP-123 Global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) — Adaptado al endpoint real `GET /api/reports/products`

## Información General

| Campo | Valor |
|-------|-------|
| **CP Global** | CP-123 (Happy Path) |
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) (Fase 1: Materialización Eventos → Consulta) |
| **Servicio** | reports-service (puerto 8088) |
| **Endpoint** | `GET /api/reports/products` |
| **Tipo** | Happy Path |
| **Formato Respuesta** | `application/stream+json` (Flux / Server-Sent Events) |

---

## Precondiciones

1. Usuario autenticado con rol `OPERATOR`, `ADMIN`, `ANALYST` o `AUDITOR`.
2. Reports Service operativo: MongoDB, Redis, Kafka (lag ≈ 0).
3. Existen eventos en `product_events` para el rango de fechas solicitado.
4. Los 20 topics Kafka están siendo consumidos y proyectados correctamente.

---

## Pasos de Prueba

### 1. Solicitar eventos de producto por rango de fechas

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  -H "Accept: application/stream+json" \
  "http://localhost:8088/api/reports/products?from=2026-06-01&to=2026-06-30"
```

**Parámetros:**
| Parámetro | Valor | Obligatorio |
|-----------|-------|-------------|
| `from` | `2026-06-01` (ISO-8601 date) | ✅ Sí |
| `to` | `2026-06-30` (ISO-8601 date) | ✅ Sí |

---

### 2. Validar respuesta exitosa (Streaming)

**Esperado:** `HTTP 200 OK` + `Content-Type: application/stream+json`

Respuesta (flujo de objetos JSON, uno por línea):
```json
{"id":"evt-001","eventType":"ACCOUNT_OPENED","aggregateId":"ACC-123","customerId":"CUST-456","productType":"ACCOUNT","subType":"SAVINGS","amount":1000.00,"balanceAfter":1000.00,"occurredAt":"2026-06-15T10:00:00","ingestedAt":"2026-06-15T10:00:05"}
{"id":"evt-002","eventType":"ACCOUNT_MOVEMENT","aggregateId":"ACC-123","customerId":"CUST-456","productType":"ACCOUNT","subType":"SAVINGS","amount":500.00,"balanceAfter":1500.00,"occurredAt":"2026-06-16T14:30:00","ingestedAt":"2026-06-16T14:30:02"}
{"id":"evt-003","eventType":"CARD_CONSUMPTION","aggregateId":"CARD-789","customerId":"CUST-456","productType":"CARD","subType":"CREDIT","amount":150.50,"balanceAfter":4850.00,"occurredAt":"2026-06-20T09:15:00","ingestedAt":"2026-06-20T09:15:01"}
```

**Validaciones:**
- [ ] Status code = 200
- [ ] `Content-Type: application/stream+json`
- [ ] Cada línea es un JSON válido (`ProductEventResponse`)
- [ ] Todos los eventos tienen `occurredAt` dentro del rango [`from`, `to`]
- [ ] Eventos ordenados por `occurredAt` (implícito en query MongoDB)
- [ ] Campos obligatorios presentes: `id`, `eventType`, `aggregateId`, `customerId`, `productType`, `occurredAt`

---

### 3. Verificar caso sin datos (rango válido pero vacío)

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/products?from=2020-01-01&to=2020-01-31"
```

**Esperado:** `HTTP 200 OK` + cuerpo vacío (stream vacío, sin elementos)
- No error 404 — el rango es válido, solo no hay datos.

---

## Postcondiciones

- Flujo de eventos entregado correctamente.
- Métricas actualizadas: `reports.queries.executed`++.
- No se modifican datos en MongoDB (solo lectura).
- Cache: este endpoint **no usa cache Redis** por ser streaming de datos potencialmente grandes.

---

## Criterios de Aceptación

| Criterio | Resultado Esperado |
|----------|-------------------|
| Código HTTP | 200 OK |
| Content-Type | `application/stream+json` |
| Formato | JSON Lines (un objeto por línea) |
| Filtro fechas | Solo eventos con `occurredAt` ∈ [`from`, `to`] |
| Orden | Descendente por `occurredAt` (más recientes primero) |
| Rendimiento | Streaming reactivo sin buffer completo en memoria |
| Seguridad | Requiere JWT válido con rol autorizado |

---

## Trazabilidad Bidireccional (Regla de Oro)

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) — Sección 8.1 |
| **Diagrama Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) — Fase 1 |
| **CP Global** | [CP-123](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) |
| **RTM** | [RTM.md#cu-13](../../../CASOS_PRUEBA/RTM.md#cu-13) |
| **Trazabilidad Servicio** | [TRACEABILIDAD_POR_SERVICIO.md#8-reports-service](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto) |
| **Endpoint Implementado** | [README.md#52-get-apireportsproducts](../README.md#52-get-apireportsproducts) |
| **Controller Test** | [ReportControllerTest.java#testGetProducts_Success](../../src/test/java/com/yanki/reports/ReportControllerTest.java#L79) |

---

## Casos Relacionados

| CP | Descripción | Relación |
|----|-------------|----------|
| [CP-124](./CP-124-rango-fechas-invalido.md) | Rango de fechas inválido | Mismo endpoint, caso error |
| [CP-125](./CP-125-reporte-sin-datos.md) | Sin datos en rango válido | Mismo endpoint, caso borde (stream vacío) |
| [CP-123 Agregado](./CP-123-generacion-reporte-agregado-exitoso.md) | Reporte agregado | Otro endpoint (agregado) |

---

## Notas de Implementación

- **Reactividad**: Retorna `Flux<ProductEventResponse>` — streaming real, no carga todo en memoria.
- **Consulta MongoDB**: `ReactiveMongoTemplate` con query por `occurredAt` range + sort descendente.
- **Índices**: Requiere índice compuesto `{ "occurredAt": -1 }` o `{ "productType": 1, "occurredAt": -1 }` para rendimiento.
- **Sin Cache**: Al ser streaming de datos potencialmente masivos, no se cachea en Redis.
- **Backpressure**: WebFlux maneja backpressure automáticamente (cliente controla ritmo).