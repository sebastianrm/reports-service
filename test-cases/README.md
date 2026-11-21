# Test Cases — reports-service

Directorio con los casos de prueba aplicables al microservicio **reports-service** (puerto 8088).

## Origen
Estos casos de prueba derivan de la **Matriz de Trazabilidad por Microservicio** (`../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto`) y del documento global `../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md` (sección **CU-13: Reportes Bancarios**).

## Casos de Prueba Globales (CU-13)

| CP | Tipo | Descripción | Endpoint Relacionado (Actual) |
|----|------|-------------|-------------------------------|
| **CP-123** | Happy Path | Generación de reporte bancario exitosa | `GET /api/reports/aggregated`, `GET /api/reports/products`, `GET /api/reports/customers/{customerId}`, `GET /api/reports/movements/cards/{cardId}` |
| **CP-124** | Error | Rango de fechas inválido (`dateFrom > dateTo` o > 1 año) | Todos los endpoints con parámetros `from`/`to` |
| **CP-125** | Happy Path (borde) | Reporte sin datos en rango válido | Todos los endpoints de consulta |

> **Nota:** Los CPs globales (CP-123 a CP-125) fueron definidos para un endpoint `POST /api/reports` que **no existe en la implementación actual**. Los archivos en este directorio adaptan dichos casos a los **4 endpoints reales implementados** (ver `../README.md#5-endpoints-rest-implementados`).

## Archivos en este directorio

| Archivo | CP Global | Endpoint(s) Cubierto(s) |
|---------|-----------|-------------------------|
| `CP-123-generacion-reporte-agregado-exitoso.md` | CP-123 | `GET /api/reports/aggregated` |
| `CP-123-generacion-reporte-productos-exitoso.md` | CP-123 | `GET /api/reports/products` |
| `CP-123-generacion-reporte-cliente-exitoso.md` | CP-123 | `GET /api/reports/customers/{customerId}` |
| `CP-123-generacion-reporte-movimientos-tarjeta-exitoso.md` | CP-123 | `GET /api/reports/movements/cards/{cardId}` |
| `CP-124-rango-fechas-invalido.md` | CP-124 | Todos (parámetros `from`/`to`) |
| `CP-125-reporte-sin-datos.md` | CP-125 | Todos |

## Trazabilidad Bidireccional (Regla de Oro)

Cada archivo mantiene referencias explícitas a:
- **Caso de Uso**: [CU-13 Reportes Bancarios](../../../CASOS_USO/CU-13-Reportes_Bancarios.md)
- **Diagrama de Secuencia**: [SEC011_reportes_bancarios.md](../../../DIAGRAMAS_SECUENCIAS/SEC011_reportes_bancarios.md)
- **Caso de Prueba Global**: CP-123/124/125 en [`../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cu-13-reportes-bancarios-event-driven`](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md#cu-13-reportes-bancarios-event-driven)
- **RTM**: [`../../../CASOS_PRUEBA/RTM.md#cu-13`](../../../CASOS_PRUEBA/RTM.md#cu-13)
- **Trazabilidad por Servicio**: [`../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto`](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md#8-reports-service-puerto-8088---esqueleto)

## Ejecución de Pruebas

### Unitarias / Integración (Maven)
```powershell
cd reports-service
$env:JAVA_HOME="C:\Program Files\Java\jdk-17.0.21.0.3"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn test
```

### Cobertura (JaCoCo)
```powershell
mvn clean verify
# Reporte en target/site/jacoco/index.html
```

### Validación Checkstyle
```powershell
mvn checkstyle:check
```

## Referencias
- [README principal del servicio](../README.md)
- [AGENTS.md del servicio](../AGENTS.md)
- [CASOS_PRUEBA_QA.md global](../../../CASOS_PRUEBA/CASOS_PRUEBA_QA.md)
- [TRACEABILIDAD_POR_SERVICIO.md](../../../CASOS_PRUEBA/TRACEABILIDAD_POR_SERVICIO.md)