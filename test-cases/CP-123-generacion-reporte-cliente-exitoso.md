# CP-123 — Generación de Reporte Cliente (Vista 360°) Exitosa

> **Derivado de:** [CP-123 Global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) — Adaptado al endpoint real `GET /api/reports/customers/{customerId}`

## Información General

| Campo | Valor |
|-------|-------|
| **CP Global** | CP-123 (Happy Path) |
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) (Fase 2: Consulta productos por cliente) |
| **Servicio** | reports-service (puerto 8088) |
| **Endpoint** | `GET /api/reports/customers/{customerId}` |
| **Tipo** | Happy Path |

---

## Precondiciones

1. Usuario autenticado con rol `OPERATOR`, `ADMIN`, `ANALYST` o `AUDITOR`.
2. Reports Service operativo: MongoDB, Redis, Kafka (lag ≈ 0).
3. Existen eventos en `product_events` asociados al `customerId` consultado.
4. Cache Redis disponible (TTL 5m).

---

## Pasos de Prueba

### 1. Solicitar reporte completo de cliente

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/customers/6af39f59b089e973a5ffe9b6"
```

**Parámetros:**
| Parámetro | Tipo | Ubicación | Obligatorio |
|-----------|------|-----------|-------------|
| `customerId` | String | Path | ✅ Sí |

---

### 2. Validar respuesta exitosa

**Esperado:** `HTTP 200 OK`

```json
{
  "customerId": "6af39f59b089e973a5ffe9b6",
  "products": [
    {
      "aggregateId": "ACC-123",
      "productType": "ACCOUNT",
      "subType": "SAVINGS",
      "events": [
        {
          "id": "evt-001",
          "eventType": "ACCOUNT_OPENED",
          "aggregateId": "ACC-123",
          "customerId": "6af39f59b089e973a5ffe9b6",
          "productType": "ACCOUNT",
          "subType": "SAVINGS",
          "amount": 1000.00,
          "balanceAfter": 1000.00,
          "occurredAt": "2026-06-15T10:00:00",
          "ingestedAt": "2026-06-15T10:00:05"
        },
        {
          "id": "evt-002",
          "eventType": "ACCOUNT_MOVEMENT",
          "aggregateId": "ACC-123",
          "customerId": "6af39f59b089e973a5ffe9b6",
          "productType": "ACCOUNT",
          "subType": "SAVINGS",
          "amount": 500.00,
          "balanceAfter": 1500.00,
          "occurredAt": "2026-06-16T14:30:00",
          "ingestedAt": "2026-06-16T14:30:02"
        }
      ]
    },
    {
      "aggregateId": "CARD-789",
      "productType": "CARD",
      "subType": "CREDIT",
      "events": [
        {
          "id": "evt-003",
          "eventType": "CARD_OPENED",
          "aggregateId": "CARD-789",
          "customerId": "6af39f59b089e973a5ffe9b6",
          "productType": "CARD",
          "subType": "CREDIT",
          "amount": 5000.00,
          "balanceAfter": 5000.00,
          "occurredAt": "2026-07-01T09:00:00",
          "ingestedAt": "2026-07-01T09:00:03"
        }
      ]
    }
  ]
}
```

**Validaciones:**
- [ ] Status code = 200
- [ ] `customerId` en respuesta coincide con el solicitado
- [ ] `products` es un array (puede estar vacío `[]` si no hay eventos)
- [ ] Cada producto tiene: `aggregateId`, `productType`, `subType`, `events[]`
- [ ] `productType` ∈ {`ACCOUNT`, `CARD`, `CREDIT`, `WALLET`}
- [ ] Eventos dentro de cada producto ordenados por `occurredAt`
- [ ] No retorna 404 aunque el cliente no tenga eventos (retorna `products: []`)

---

### 3. Verificar Cache Redis (Cache HIT)

1. Repetir la misma petición.
2. Verificar latencia reducida (cache hit).
3. Métrica `reports.cache.hit_ratio` incrementada.

---

### 4. Caso borde: Cliente sin eventos

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/customers/CLIENTE_SIN_EVENTOS"
```

**Esperado:** `HTTP 200 OK`
```json
{
  "customerId": "CLIENTE_SIN_EVENTOS",
  "products": []
}
```
- **No** 404 — el cliente puede existir en el sistema pero no haber generado eventos proyectados aún.

---

## Postcondiciones

- Reporte cliente generado y devuelto.
- Métricas actualizadas: `reports.queries.executed`++.
- Cache Redis actualizado (si era MISS) con TTL 5m.
- No se modifican datos en MongoDB.

---

## Criterios de Aceptación

| Criterio | Resultado Esperado |
|----------|-------------------|
| Código HTTP | 200 OK |
| Estructura | `CustomerReportResponse` con `products[]` |
| Cliente sin eventos | 200 + `products: []` (no 404) |
| Agrupación | Por `aggregateId` + `productType` + `subType` |
| Cache | Funcional (TTL 5m) |
| Seguridad | Requiere JWT válido con rol autorizado |

---

## Trazabilidad Bidireccional (Regla de Oro)

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) — Sección 8.2 |
| **Diagrama Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) — Fase 2 |
| **CP Global** | [CP-123](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) |
| **RTM** | [RTM.md#cu-13](../../../CASOS_PRUEBA/RTM.md#cu-13) |
| **Trazabilidad Servicio** | [TRACEABILIDAD_POR_SERVICIO.md#8-reports-service](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto) |
| **Endpoint Implementado** | [README.md#53-get-apireportscustomerscustomerid](../README.md#53-get-apireportscustomerscustomerid) |
| **Controller Test** | [ReportControllerTest.java#testGetCustomerReport_Success](../../src/test/java/com/yanki/reports/ReportControllerTest.java#L115) |

---

## Casos Relacionados

| CP | Descripción | Relación |
|----|-------------|----------|
| [CP-124](./CP-124-rango-fechas-invalido.md) | Rango fechas inválido | No aplica (este endpoint no tiene parámetros de fecha) |
| [CP-125](./CP-125-reporte-sin-datos.md) | Sin datos | Caso borde: `products: []` |
| [CP-123 Agregado](./CP-123-generacion-reporte-agregado-exitoso.md) | Reporte agregado | Otro endpoint |
| [CP-123 Tarjeta](./CP-123-generacion-reporte-movimientos-tarjeta-exitoso.md) | Últimos 10 mov. tarjeta | Req. Parte II |

---

## Notas de Implementación

- **Consulta**: Agrega eventos por `customerId` → agrupa por `aggregateId` + `productType` + `subType`.
- **Cache**: Clave `yanki:reports:cache:customer:{customerId}`, TTL 5m.
- **Orden**: Productos ordenados por último `occurredAt` descendente; eventos dentro de cada producto descendente.
- **Sin 404**: Diseño intencional — cliente puede no tener eventos pero ser válido en el sistema.
- **Rendimiento**: Índice MongoDB `{ "customerId": 1, "occurredAt": -1 }` crítico.