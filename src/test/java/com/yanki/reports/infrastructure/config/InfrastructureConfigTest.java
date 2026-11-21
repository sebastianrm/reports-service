package com.yanki.reports.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yanki.reports.domain.model.CustomerReport;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.model.ProductGroup;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas unitarias de la configuración de serialización JSON de Redis.
 *
 * <p>Estas pruebas verifican que el {@code ObjectMapper} utilizado por
 * {@code Jackson2JsonRedisSerializer} puede serializar correctamente los tipos
 * {@code java.time.LocalDateTime} presentes en los modelos de dominio
 * (CustomerReport → ProductGroup → ProductEvent.occurredAt).
 *
 * <p>Antes del fix, intentar serializar un {@code CustomerReport} con eventos
 * que contienen {@code LocalDateTime} lanzaba:
 * <pre>
 * InvalidDefinitionException: Java 8 date/time type {@code java.time.LocalDateTime}
 * not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
 * </pre>
 */
class InfrastructureConfigTest {

    /**
     * Verifica que el ObjectMapper con JavaTimeModule registrado puede serializar
     * un CustomerReport completo (que contiene LocalDateTime en ProductEvent.occurredAt)
     * sin lanzar InvalidDefinitionException.
     *
     * <p>Este es el escenario exacto que fallaba en producción:
     * CustomerReport → products → ProductGroup → events → ProductEvent.occurredAt (LocalDateTime).
     */
    @Test
    void testRedisSerializer_serializesCustomerReportWithLocalDateTime_withoutException() {
        // Arrange: configurar el ObjectMapper igual que InfrastructureConfig
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(mapper);

        // Crear un CustomerReport con LocalDateTime en la cadena completa
        ProductEvent event = ProductEvent.builder()
                .id("event-001")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("acc-123")
                .customerId("cust-456")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .amount(BigDecimal.valueOf(1000))
                .balanceAfter(BigDecimal.valueOf(1000))
                .metadata(Collections.emptyMap())
                .occurredAt(LocalDateTime.of(2026, 1, 15, 10, 30, 0))
                .ingestedAt(LocalDateTime.of(2026, 1, 15, 10, 30, 5))
                .build();

        ProductGroup productGroup = ProductGroup.builder()
                .aggregateId("acc-123")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .events(List.of(event))
                .build();

        CustomerReport report = CustomerReport.builder()
                .customerId("cust-456")
                .products(List.of(productGroup))
                .build();

        // Act & Assert: la serialización no debe lanzar excepción
        assertDoesNotThrow(() -> {
            byte[] serialized = serializer.serialize(report);
            assertNotNull(serialized);
        }, "Serialización de CustomerReport con LocalDateTime no debe lanzar excepción");
    }

    /**
     * Verifica que el ObjectMapper sin JavaTimeModule lanza InvalidDefinitionException
     * al intentar serializar un objeto con LocalDateTime.
     *
     * <p>Esta prueba demuestra el comportamiento defectuoso original que motivó el fix.
     */
    @Test
    void testRedisSerializer_withoutJavaTimeModule_throwsInvalidDefinitionException() {
        // Arrange: ObjectMapper sin JavaTimeModule (configuración defectuosa)
        ObjectMapper mapper = new ObjectMapper();
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(mapper);

        ProductEvent event = ProductEvent.builder()
                .id("event-001")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("acc-123")
                .customerId("cust-456")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .occurredAt(LocalDateTime.of(2026, 1, 15, 10, 30, 0))
                .ingestedAt(LocalDateTime.of(2026, 1, 15, 10, 30, 5))
                .build();

        ProductGroup productGroup = ProductGroup.builder()
                .aggregateId("acc-123")
                .productType("ACCOUNT")
                .subType("SAVINGS")
                .events(List.of(event))
                .build();

        CustomerReport report = CustomerReport.builder()
                .customerId("cust-456")
                .products(List.of(productGroup))
                .build();

        // Act & Assert: sin JavaTimeModule, debe lanzar SerializationException
        // (Jackson2JsonRedisSerializer envuelve InvalidDefinitionException en SerializationException,
        //  exactamente como ocurrió en producción)
        SerializationException ex = assertThrows(
                SerializationException.class,
                () -> serializer.serialize(report),
                "Sin JavaTimeModule, la serialización de LocalDateTime debe fallar"
        );
        // Verifica que la causa raíz es InvalidDefinitionException
        assertNotNull(ex.getCause());
        assert ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidDefinitionException :
                "La causa raíz debe ser InvalidDefinitionException";
    }

    /**
     * Verifica que la serialización produce JSON válido con fechas en formato ISO-8601
     * (no como timestamps numéricos), gracias a WRITE_DATES_AS_TIMESTAMPS deshabilitado.
     */
    @Test
    void testRedisSerializer_serializesLocalDateTime_asIso8601String() {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(mapper);

        ProductEvent event = ProductEvent.builder()
                .id("event-001")
                .eventType("ACCOUNT_OPENED")
                .aggregateId("acc-123")
                .customerId("cust-456")
                .productType("ACCOUNT")
                .occurredAt(LocalDateTime.of(2026, 6, 15, 14, 30, 0))
                .ingestedAt(LocalDateTime.of(2026, 6, 15, 14, 30, 5))
                .build();

        ProductGroup productGroup = ProductGroup.builder()
                .aggregateId("acc-123")
                .productType("ACCOUNT")
                .events(List.of(event))
                .build();

        CustomerReport report = CustomerReport.builder()
                .customerId("cust-456")
                .products(List.of(productGroup))
                .build();

        // Act
        byte[] serialized = serializer.serialize(report);
        String json = new String(serialized);

        // Assert: el JSON debe contener la fecha en formato ISO-8601
        assertNotNull(serialized);
        assertDoesNotThrow(() -> {
            // Verifica que la fecha aparece como string ISO-8601, no como timestamp numérico
            assert (json.contains("\"occurredAt\"") && json.contains("2026-06-15")) :
                    "El JSON debe contener occurredAt en formato ISO-8601: " + json;
        });
    }
}
