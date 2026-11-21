# CP-124 — Rango de Fechas Inválido

> **Derivado de:** [CP-124 Global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-124--generacion-de-reporte-rechazada-rango-de-fechas-invalido) — Adaptado a los endpoints reales con parámetros `from`/`to`

## Información General

| Campo | Valor |
|-------|-------|
| **CP Global** | CP-124 (Error) |
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama de Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) (Fase 2: Validación entrada) |
| **Servicio** | reports-service (puerto 8088) |
| **Endpoints Afectados** | `GET /api/reports/aggregated`, `GET /api/reports/products` |
| **Tipo** | Error / Validación |

---

## Precondiciones

1. Usuario autenticado con rol válido (`OPERATOR`, `ADMIN`, `ANALYST`, `AUDITOR`).
2. Reports Service operativo.

---

## Casos de Prueba

### Caso 1: `from` > `to` (fecha inicial posterior a final)

#### Endpoint: `GET /api/reports/aggregated`

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&from=2026-12-31&to=2026-01-01&customerType=NATURAL"
```

#### Endpoint: `GET /api/reports/products`

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/products?from=2026-12-31&to=2026-01-01"
```

**Esperado:** `HTTP 400 Bad Request`

```json
{
  "timestamp": "2026-11-14T18:30:00",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_DATE_RANGE",
  "message": "Fecha inicial (from) debe ser anterior o igual a fecha final (to)",
  "path": "/api/reports/aggregated",
  "details": []
}
```

**Validaciones:**
- [ ] Status code = 400
- [ ] `code` = `INVALID_DATE_RANGE`
- [ ] `message` descriptivo del error
- [ ] `path` corresponde al endpoint invocado

---

### Caso 2: Formato de fecha inválido

#### Endpoint: `GET /api/reports/aggregated`

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&from=2026/01/01&to=2026-12-31&customerType=NATURAL"
```

#### Endpoint: `GET /api/reports/products`

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/products?from=01-01-2026&to=2026-12-31"
```

**Esperado:** `HTTP 400 Bad Request`

```json
{
  "timestamp": "2026-11-14T18:30:00",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_DATE_RANGE",
  "message": "Formato de fecha inválido. Use ISO-8601 (yyyy-MM-dd)",
  "path": "/api/reports/products",
  "details": []
}
```

---

### Caso 3: Rango excesivo (> 1 año) — Opcional según reglas de negocio

> **Nota:** El CP-124 global menciona "rango > 1 año" como error. Verificar si la implementación actual valida esto.

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&from=2020-01-01&to=2026-12-31&customerType=NATURAL"
```

**Si implementado:** `HTTP 400 Bad Request` con `code: INVALID_DATE_RANGE` y mensaje sobre rango máximo permitido.

**Si NO implementado:** `HTTP 200 OK` (comportamiento actual — sin validación de rango máximo).

---

### Caso 4: Parámetros de fecha faltantes (endpoints que los requieren)

#### `GET /api/reports/aggregated` (requiere `from`, `to`, `type`, `customerType`)

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/aggregated?type=ACCOUNT&customerType=NATURAL"
```

#### `GET /api/reports/products` (requiere `from`, `to`)

```bash
curl -H "Authorization: Bearer <token_OPERATOR>" \
  "http://localhost:8088/api/reports/products?from=2026-01-01"
```

**Esperado:** `HTTP 400 Bad Request`

```json
{
  "timestamp": "2026-11-14T18:30:00",
  "status": 400,
  "error": "Bad Request",
  "code": "MISSING_REQUIRED_PARAMETER",
  "message": "Parámetro obligatorio 'to' no proporcionado",
  "path": "/api/reports/products",
  "details": []
}
```

---

## Postcondiciones

- Error controlado devuelto sin consultar MongoDB/Redis.
- Métricas: `reports.queries.executed`++ (contador de intentos).
- No hay impacto en cache ni datos.

---

## Criterios de Aceptación

| Caso | HTTP | Code | Mensaje |
|------|------|------|---------|
| `from > to` | 400 | `INVALID_DATE_RANGE` | "Fecha inicial debe ser anterior o igual a fecha final" |
| Formato inválido | 400 | `INVALID_DATE_RANGE` | "Formato de fecha inválido. Use ISO-8601 (yyyy-MM-dd)" |
| Parámetro faltante | 400 | `MISSING_REQUIRED_PARAMETER` | "Parámetro obligatorio 'X' no proporcionado" |
| Rango > 1 año* | 400* | `INVALID_DATE_RANGE`* | "Rango máximo permitido: 1 año"* |

*Solo si implementado en reglas de negocio.

---

## Trazabilidad Bidireccional (Regla de Oro)

| Artefacto | Referencia |
|-----------|------------|
| **Caso de Uso** | [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md) |
| **Diagrama Secuencia** | [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md) — Validación entrada |
| **CP Global** | [CP-124](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cp-124--generacion-de-reporte-rechazada-rango-de-fechas-invalido) |
| **RTM** | [RTM.md#cu-13](../../../CASOS_PRUEBA/RTM.md#cu-13) |
| **Trazabilidad Servicio** | [TRACEABILIDAD_POR_SERVICIO.md#8-reports-service](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto) |
| **Endpoints** | [README.md#5-endpoints-rest-implementados](../README.md#5-endpoints-rest-implementados) |

---

## Casos Relacionados

| CP | Descripción | Relación |
|----|-------------|----------|
| [CP-123 Agregado](./CP-123-generacion-reporte-agregado-exitoso.md) | Happy Path agregado | Caso exitoso mismo endpoint |
| [CP-123 Productos](./CP-123-generacion-reporte-productos-exitoso.md) | Happy Path productos | Caso exitoso mismo endpoint |
| [CP-125](./CP-125-reporte-sin-datos.md) | Sin datos en rango válido | Rango válido pero vacío |

---

## Notas de Implementación

- **Validación temprana**: En `ReportController` o `ReportService` antes de consultar MongoDB/Redis.
- **Spring Validation**: Usar `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` + validación custom `from <= to`.
- **GlobalExceptionHandler**: Mapea `IllegalArgumentException` / `ConstraintViolationException` → `ApiError` con código `INVALID_DATE_RANGE`.
- **Sin Cache**: Errores 400 no se cachean en Redis.
- **Parámetros opcionales**: `GET /api/reports/customers/{customerId}` y `GET /api/reports/movements/cards/{cardId}` **no tienen** parámetros `from`/`to` → no aplican estos casos.