# CP-123 — Generación de Últimos 10 Movimientos de Tarjeta Exitosa

> **Derivado de:** [CP-123 Global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) — Adaptado al endpoint real `GET /api/reports/movements/cards/{cardId}`
>
> **⚠️ REQUERIMIENTO OBLIGATORIO PARTE II:** "Implementar un reporte con los últimos 10 movimientos de la tarjeta de débito y de crédito"

## Información General

| Campo | Valor |
|-------|-------|
| **CP Global** | CP-123 (Happy Path) |
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) (Fase 3: Últimos 10 Movimientos Tarjeta) |
| **Servicio** | reports-service (puerto 8088) |
| **Endpoint** | `GET /api/reports/movements/cards/{cardId}` |
| **Tipo** | Happy Path |
| **Prioridad** | **CRÍTICA** — Requerimiento obligatorio Parte II |

---

## Precondiciones

1. Usuario autenticado con rol `OPERATOR`, `ADMIN`, `ANALYST` o `AUDITOR`.
2. Reports Service operativo: MongoDB, Redis, Kafka (lag ≈ 0).
3. Existen eventos de tipo `CARD_CONSUMPTION`, `DEBIT_PAYMENT_SETTLED`, `CARD_PAYMENT` (o equivalentes) para el `cardId` consultado.
4. La tarjeta tiene `productType = CARD` y `subType = CREDIT` o `DEBIT`.
5. Cache Redis disponible (TTL 2m para este endpoint).

---

## Pasos de Prueba

### 1. Solicitar últimos 10 movimientos de tarjeta (default)

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/movements/cards/CARD-789"
```

**Parámetros:**
| Parámetro | Tipo | Ubicación | Obligatorio | Default |
|-----------|------|-----------|-------------|---------|
| `cardId` | String | Path | ✅ Sí | - |
| `limit` | Integer | Query | No | 10 (máx recomendado 100) |

---

### 2. Validar respuesta exitosa

**Esperado:** `HTTP 200 OK`

```json
{
  "cardId": "CARD-789",
  "productType": "CARD",
  "subType": "CREDIT",
  "movements": [
    {
      "id": "evt-010",
      "eventType": "CARD_CONSUMPTION",
      "aggregateId": "CARD-789",
      "customerId": "CUST-456",
      "productType": "CARD",
      "subType": "CREDIT",
      "amount": 150.50,
      "balanceAfter": 4850.00,
      "occurredAt": "2026-11-15T12:30:00",
      "ingestedAt": "2026-11-15T12:30:02"
    },
    {
      "id": "evt-009",
      "eventType": "CARD_CONSUMPTION",
      "aggregateId": "CARD-789",
      "customerId": "CUST-456",
      "productType": "CARD",
      "subType": "CREDIT",
      "amount": 89.99,
      "balanceAfter": 5000.00,
      "occurredAt": "2026-11-14T18:45:00",
      "ingestedAt": "2026-11-14T18:45:01"
    }
  ]
}
```

**Validaciones:**
- [ ] Status code = 200
- [ ] `cardId` en respuesta coincide con el solicitado
- [ ] `productType` = `CARD`
- [ ] `subType` = `CREDIT` o `DEBIT`
- [ ] `movements` es array con ≤ `limit` elementos (default 10)
- [ ] Movimientos ordenados **descendente por `occurredAt`** (más reciente primero)
- [ ] Solo eventos relevantes de tarjeta: `CARD_CONSUMPTION`, `DEBIT_PAYMENT_SETTLED`, `CARD_PAYMENT`, `CARD_REFUND` (si existen)
- [ ] `balanceAfter` o `availableLimit` presente según subType
- [ ] Header `Content-Type: application/json`

---

### 3. Probar con límite personalizado

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/movements/cards/CARD-789?limit=5"
```

**Esperado:** `HTTP 200 OK` + `movements.length ≤ 5`

---

### 4. Verificar Cache Redis (TTL 2m)

1. Repetir petición inmediatamente.
2. Verificar latencia reducida (Cache HIT).
3. TTL más corto (2m) que otros endpoints por ser datos más volátiles.

---

### 5. Validar ambos subtipos de tarjeta

| SubTipo | Endpoint | Eventos Esperados |
|---------|----------|-------------------|
| **CREDIT** | `GET /api/reports/movements/cards/CARD-CREDIT-001` | `CARD_CONSUMPTION`, `CARD_PAYMENT`, `CARD_REFUND` |
| **DEBIT** | `GET /api/reports/movements/cards/CARD-DEBIT-001` | `DEBIT_PAYMENT_SETTLED`, `CARD_CONSUMPTION` (si aplica) |

---

## Postcondiciones

- Últimos N movimientos de tarjeta devueltos correctamente.
- Métricas actualizadas: `reports.queries.executed`++.
- Cache Redis actualizado (TTL 2m).
- No se modifican datos en MongoDB.

---

## Criterios de Aceptación (Requerimiento Parte II)

| Criterio | Resultado Esperado |
|----------|-------------------|
| **Código HTTP** | 200 OK |
| **Límite default** | 10 movimientos |
| **Orden** | Descendente por `occurredAt` (más reciente primero) |
| **Tarjeta Crédito** | Incluye consumos, pagos, devoluciones |
| **Tarjeta Débito** | Incluye pagos liquidados (DEBIT_PAYMENT_SETTLED) |
| **Límite configurable** | Parámetro `limit` (1-100) funcional |
| **Cache** | TTL 2m, Cache HIT/MISS funcional |
| **Error 404** | Si no existen eventos para el `cardId` |
| **Seguridad** | Requiere JWT válido con rol autorizado |

---

## Trazabilidad Bidireccional (Regla de Oro)

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) — Sección 8.3 (Req. Parte II) |
| **Diagrama Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) — **Fase 3** |
| **CP Global** | [CP-123](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-123--generacion-de-reporte-bancario-exitosa) |
| **Req. Parte II** | [Proyecto General vf.md#parte-ii](../../../Proyecto General vf.md#L104) — "últimos 10 movimientos tarjeta" |
| **RTM** | [RTM.md#cu-13](../../../CASOS_PRUEBA/RTM.md#cu-13) |
| **Trazabilidad Servicio** | [TRACEABILIDAD_POR_SERVICIO.md#8-reports-service](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto) |
| **Endpoint Implementado** | [README.md#54-get-apireportsmovementscardscardid](../README.md#54-get-apireportsmovementscardscardid) |
| **Controller Test** | [ReportControllerTest.java#testGetCardMovements_Success](../../src/test/java/com/yanki/reports/ReportControllerTest.java#L181) |

---

## Casos Relacionados

| CP | Descripción | Relación |
|----|-------------|----------|
| [CP-124](./CP-124-rango-fechas-invalido.md) | Rango fechas inválido | No aplica (sin parámetros fecha) |
| [CP-125](./CP-125-reporte-sin-datos.md) | Sin datos | Caso borde: 404 `PRODUCT_NOT_FOUND` |
| [CP-123 Agregado](./CP-123-generacion-reporte-agregado-exitoso.md) | Reporte agregado | Otro endpoint |
| [CP-123 Cliente](./CP-123-generacion-reporte-cliente-exitoso.md) | Vista 360° cliente | Otro endpoint |

---

## Notas de Implementación

- **Req. Parte II**: Este endpoint **cumple directamente** el requerimiento obligatorio "Implementar un reporte con los últimos 10 movimientos de la tarjeta de débito y de crédito".
- **Consulta optimizada**: Query MongoDB por `productId` + `productType=CARD` + sort `occurredAt DESC` + limit.
- **Índice crítico**: `{ "aggregateId": 1, "productType": 1, "occurredAt": -1 }` o `{ "productId": 1, "occurredAt": -1 }`.
- **Cache**: TTL 2m (más agresivo que otros endpoints por volatilidad de movimientos).
- **SubType detection**: El servicio deriva `subType` (CREDIT/DEBIT) del primer evento encontrado o del `product_summary`.
- **404 vs 200 vacío**: Retorna 404 `PRODUCT_NOT_FOUND` si **no existe ningún evento** para ese `cardId` (diferente a cliente sin eventos).