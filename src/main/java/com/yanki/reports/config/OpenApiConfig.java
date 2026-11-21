package com.yanki.reports.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración global de OpenAPI 3 para la documentación de la API.
 *
 * <p>Define la metadata del API (título, descripción, versión, contacto, licencia),
 * los servidores disponibles (producción y desarrollo), el esquema de seguridad JWT
 * y las tags que agrupan los endpoints por categoría funcional.
 *
 * <p>Author: Sebastian Retamal
 * <p>Email: semorale@emeal.nttdata.com
 */
@Configuration
public class OpenApiConfig {

    /**
     * Bean principal de OpenAPI con toda la configuración global.
     *
     * @return instancia de OpenAPI configurada
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(apiComponents())
                .tags(apiTags());
    }

    /**
     * Metadata del API: título, descripción, versión, contacto y licencia.
     *
     * @return objeto Info de OpenAPI
     */
    private Info apiInfo() {
        return new Info()
                .title("Yanki Reports Service API")
                .description(
                        "API de reportes y analítica — Arquitectura CQRS read-only.\n\n"
                        + "Este servicio consume eventos de dominio de 20 topics Kafka (clientes, cuentas, tarjetas, "
                        + "créditos, transferencias, wallets) y los proyecta en colecciones MongoDB (`product_events`, "
                        + "`customer_summary`, `product_summary`, `daily_aggregates`) "
                        + "para generar reportes sin depender de llamadas REST a otros servicios.\n\n"
                        + "**Capacidades (7 endpoints):**\n"
                        + "1. Reporte agregado por tipo de producto y rango de fechas\n"
                        + "2. Eventos de producto en un rango de fechas (streaming)\n"
                        + "3. Reporte completo de cliente con todos sus productos (Vista 360°)\n"
                        + "4. Últimos movimientos de tarjeta (crédito o débito)\n"
                        + "5. Resumen 360° del cliente (colección customer_summary)\n"
                        + "6. Resumen de producto específico (colección product_summary)\n"
                        + "7. Agregados diarios para dashboards (colección daily_aggregates)\n\n"
                        + "**Tecnologías:** Java 17, Spring Boot 3.3.5 (WebFlux reactivo), "
                        + "MongoDB Reactive, Redis Cache, Spring Kafka, Resilience4j, RxJava 3\n\n"
                        + "**Arquitectura:** Hexagonal (Puertos y Adaptadores), Database-per-service, "
                        + "reactividad pura, idempotencia en ingesta de eventos, cache distribuido Redis (TTL 5m)."
                )
                .version("1.0.0")
                .contact(new Contact()
                        .name("Platform - Analytics & Reporting")
                        .email("semorale@emeal.nttdata.com")
                        .url("https://github.com/yanki/reports-service"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://yanki.internal/license"));
    }

    /**
     * Lista de servidores disponibles para la API.
     *
     * @return lista de Server con descripción y URL
     */
    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8088")
                        .description("Entorno local de desarrollo"),
                new Server()
                        .url("http://reports-service:8088")
                        .description("Entorno Docker Compose (interno)"),
                new Server()
                        .url("https://reports.yanki-staging.internal/api")
                        .description("Entorno de staging"),
                new Server()
                        .url("https://api.yanki.com/reports")
                        .description("Entorno de producción (a través de API Gateway)")
        );
    }

    /**
     * Componentes de seguridad: esquema JWT Bearer token.
     *
     * @return objeto Components con el security scheme definido
     */
    private Components apiComponents() {
        return new Components()
                .addSecuritySchemes(
                        "Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description(
                                        "Autenticación JWT a través del API Gateway.\n\n"
                                        + "Roles disponibles:\n"
                                        + "- **ADMIN**: Acceso completo a todos los reportes y operaciones administrativas\n"
                                        + "- **OPERATOR**: Acceso a reportes operativos (productos, movimientos)\n"
                                        + "- **ANALYST**: Acceso a reportes agregados y analítica\n"
                                        + "- **AUDITOR**: Acceso de solo lectura a reportes históricos\n\n"
                                        + "El token se obtiene a través del servicio de autenticación (auth-service) "
                                        + "y se propaga automáticamente por el API Gateway."
                                )
                );
    }

    /**
     * Tags que agrupan los endpoints por categoría funcional.
     *
     * @return lista de Tag con nombre y descripción
     */
    private List<Tag> apiTags() {
        return List.of(
                new Tag()
                        .name("Reports - Aggregated")
                        .description(
                                "Reportes agregados por tipo de producto.\n\n"
                                + "Proporciona estadísticas consolidadas (count, sum, average) "
                                + "filtradas por tipo de producto, rango de fechas y tipo de cliente.\n\n"
                                + "**Casos de uso:** Dashboard ejecutivo, KPIs por producto, "
                                + "análisis de tendencias temporales."
                        ),
                new Tag()
                        .name("Reports - Products")
                        .description(
                                "Eventos de producto en un rango de fechas.\n\n"
                                + "Retorna el flujo completo de eventos proyectados desde Kafka "
                                + "para cualquier tipo de producto (cuentas, tarjetas, créditos, wallets).\n\n"
                                + "**Casos de uso:** Auditoría de eventos, reconstrucción de estado, "
                                + "debugging de proyecciones."
                        ),
                new Tag()
                        .name("Reports - Customers")
                        .description(
                                "Reporte completo de un cliente.\n\n"
                                + "Agrega todos los productos (cuentas, tarjetas, créditos, wallets) "
                                + "asociados a un cliente con sus eventos correspondientes.\n\n"
                                + "**Casos de uso:** Vista 360° del cliente, "
                                + "atención al cliente, análisis de cross-selling."
                        ),
                new Tag()
                        .name("Reports - Card Movements")
                        .description(
                                "Últimos movimientos de una tarjeta.\n\n"
                                + "Consulta optimizada para obtener los movimientos más recientes "
                                + "de una tarjeta de crédito o débito, con paginación limitada.\n\n"
                                + "**Casos de uso:** Estado de cuenta en tiempo real, "
                                + "disputas de transacciones, monitoreo de actividad sospechosa."
                        ),
                new Tag()
                        .name("Reports - Customer Summary")
                        .description(
                                "Resumen 360° consolidado del cliente.\n\n"
                                + "Obtiene totales de cuentas, tarjetas, créditos y wallets, "
                                + "así como saldos acumulados y estado de deudas vencidas.\n\n"
                                + "**Fuente:** Colección customer_summary materializada por eventos Kafka.\n\n"
                                + "**Casos de uso:** Dashboard de cliente, scoring crediticio, "
                                + "análisis de relación cliente-banco."
                        ),
                new Tag()
                        .name("Reports - Product Summary")
                        .description(
                                "Resumen del estado actual de un producto bancario.\n\n"
                                + "Obtiene saldos, límites, estado y timestamps de un producto "
                                + "específico (cuenta, tarjeta, crédito o wallet).\n\n"
                                + "**Fuente:** Colección product_summary materializada por eventos Kafka.\n\n"
                                + "**Casos de uso:** Consulta de estado de producto, "
                                + "validación de límites, monitoreo de productos."
                        ),
                new Tag()
                        .name("Reports - Daily Aggregates")
                        .description(
                                "Agregados diarios para dashboards y analítica.\n\n"
                                + "Obtiene métricas diarias consolidadas por tipo de producto y tipo de cliente: "
                                + "cantidad de movimientos, montos totales y promedios, clientes únicos.\n\n"
                                + "**Fuente:** Colección daily_aggregates materializada por eventos Kafka.\n\n"
                                + "**Casos de uso:** Dashboards operativos, reportes diarios, "
                                + "análisis de volumen transaccional."
                        )
        );
    }
}
