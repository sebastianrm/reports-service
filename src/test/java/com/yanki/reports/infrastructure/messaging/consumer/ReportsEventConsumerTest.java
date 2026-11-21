package com.yanki.reports.infrastructure.messaging.consumer;

import com.yanki.reports.domain.event.CustomerRegisteredEvent;
import com.yanki.reports.domain.model.ProcessedEvent;
import com.yanki.reports.domain.model.ProductEvent;
import com.yanki.reports.domain.port.in.IngestEventUseCase;
import com.yanki.reports.domain.port.out.ProcessedEventPort;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ReportsEventConsumer.
 *
 * <p>Cubre los errores ya solucionados:
 * <ul>
 *   <li>NPE en buildEventId cuando occurredAt es null (fallback a LocalDateTime.now())</li>
 *   <li>Evento duplicado descartado correctamente por idempotencia</li>
 *   <li>Error en ingesta de evento sin hacer ack</li>
 * </ul>
 */
class ReportsEventConsumerTest {

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private IngestEventUseCase ingestEventUseCase;

    @Mock
    private Acknowledgment ack;

    private ReportsEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new ReportsEventConsumer(processedEventPort, ingestEventUseCase);
    }

    /**
     * Verifica que cuando occurredAt es null (como en CustomerRegisteredEvent de customers-service),
     * el consumidor no lanza NPE y usa LocalDateTime.now() como fallback.
     *
     * <p>Este era el bug original:
     * {@code NullPointerException: Cannot invoke "java.time.LocalDateTime.toString()" because "occurredAt" is null}
     */
    @Test
    void consumeCustomerRegistered_withNullOccurredAt_shouldNotThrowNPE() {
        // Arrange: evento sin occurredAt (schema mismatch con customers-service)
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId("6afc499276003a643d588965")
                .documentType("DNI")
                .documentNumber("70555001")
                .customerType("PERSONAL")
                .profile("STANDARD")
                .email("test@test.com")
                .phone("+51999000001")
                .occurredAt(null) // Null - este era el bug
                .build();

        String topic = "yanki.customer.registered.v1";

        // El puerto de idempotencia registra el evento nuevo
        when(processedEventPort.tryRegister(any(ProcessedEvent.class)))
                .thenReturn(Single.just(true)) // true = nuevo evento
                .thenReturn(Completable.error(new RuntimeException("unexpected")).toSingleDefault(false));

        when(ingestEventUseCase.execute(any(ProductEvent.class)))
                .thenReturn(Single.just(ProductEvent.builder().id("evt-1").build()));

        // Act
        consumer.consumeCustomerRegistered(event, topic, ack);

        // Give RxJava time to complete
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Assert: no NPE lanzado
        // Verificar que tryRegister fue llamado con un eventId que NO contiene "null"
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventPort, atLeastOnce()).tryRegister(captor.capture());

        String eventId = captor.getValue().getId();
        // El eventId debe tener formato topic:aggregateId:timestamp (sin "null")
        assert eventId.startsWith(topic + ":") : "eventId debe empezar con el topic: " + eventId;
        assert !eventId.contains("null") : "eventId no debe contener 'null', debe usar fallback de timestamp: " + eventId;
        // El timestamp debe contener un patrón de fecha válida al final del eventId
        // Formato: yanki.customer.registered.v1:6afc499276003a643d588965:2026-11-17T07:55:29.123
        assert eventId.matches(".+:\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*") :
                "El eventId debe terminar con un timestamp LocalDateTime válido: " + eventId;
    }

    /**
     * Verifica que cuando occurredAt tiene valor, se usa correctamente en el eventId.
     */
    @Test
    void consumeCustomerRegistered_withOccurredAt_shouldUseItInEventId() {
        // Arrange
        LocalDateTime fixedTime = LocalDateTime.of(2026, 11, 17, 12, 33, 22, 318251521);
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId("6afc499276003a643d588965")
                .occurredAt(fixedTime)
                .build();

        String topic = "yanki.customer.registered.v1";

        when(processedEventPort.tryRegister(any(ProcessedEvent.class)))
                .thenReturn(Single.just(true));
        when(ingestEventUseCase.execute(any(ProductEvent.class)))
                .thenReturn(Single.just(ProductEvent.builder().id("evt-1").build()));

        // Act
        consumer.consumeCustomerRegistered(event, topic, ack);

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Assert
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventPort, atLeastOnce()).tryRegister(captor.capture());

        String eventId = captor.getValue().getId();
        assert eventId.contains("2026-11-17T12:33:22") :
                "eventId debe contener el timestamp del evento: " + eventId;
    }

    /**
     * Verifica que cuando el evento es duplicado (tryRegister devuelve false),
     * se hace ack y no se intenta ingerir.
     */
    @Test
    void consumeCustomerRegistered_duplicateEvent_shouldAckAndNotIngest() {
        // Arrange
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId("6afc499276003a643d588965")
                .occurredAt(LocalDateTime.now())
                .build();

        String topic = "yanki.customer.registered.v1";

        // false = evento duplicado
        when(processedEventPort.tryRegister(any(ProcessedEvent.class)))
                .thenReturn(Single.just(false));

        // Act
        consumer.consumeCustomerRegistered(event, topic, ack);

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Assert: se hace ack y NO se llama a ingestEventUseCase
        verify(ack).acknowledge();
        verify(ingestEventUseCase, never()).execute(any(ProductEvent.class));
    }

    /**
     * Verifica que cuando tryRegister falla, no se hace ack (para permitir reintento).
     */
    @Test
    void consumeCustomerRegistered_tryRegisterFails_shouldNotAck() {
        // Arrange
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId("6afc499276003a643d588965")
                .occurredAt(LocalDateTime.now())
                .build();

        String topic = "yanki.customer.registered.v1";

        when(processedEventPort.tryRegister(any(ProcessedEvent.class)))
                .thenReturn(Single.error(new RuntimeException("DB connection failed")));

        // Act
        consumer.consumeCustomerRegistered(event, topic, ack);

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Assert: NO se hace ack para permitir reintento
        verify(ack, never()).acknowledge();
    }

    /**
     * Verifica que cuando la ingesta falla, no se hace ack (para permitir reintento).
     */
    @Test
    void consumeCustomerRegistered_ingestFails_shouldNotAck() {
        // Arrange
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId("6afc499276003a643d588965")
                .occurredAt(LocalDateTime.now())
                .build();

        String topic = "yanki.customer.registered.v1";

        when(processedEventPort.tryRegister(any(ProcessedEvent.class)))
                .thenReturn(Single.just(true)); // Nuevo evento
        when(ingestEventUseCase.execute(any(ProductEvent.class)))
                .thenReturn(Single.error(new RuntimeException("Ingest failed")));

        // Act
        consumer.consumeCustomerRegistered(event, topic, ack);

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Assert: NO se hace ack para permitir reintento
        verify(ack, never()).acknowledge();
    }

    /**
     * Verifica el flujo exitoso completo: evento nuevo -> registrado -> ingerido -> ack.
     */
    @Test
    void consumeCustomerRegistered_happyPath_shouldAck() {
        // Arrange
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId("6afc499276003a643d588965")
                .occurredAt(LocalDateTime.now())
                .build();

        String topic = "yanki.customer.registered.v1";

        when(processedEventPort.tryRegister(any(ProcessedEvent.class)))
                .thenReturn(Single.just(true)); // Nuevo evento
        when(ingestEventUseCase.execute(any(ProductEvent.class)))
                .thenReturn(Single.just(ProductEvent.builder().id("evt-1").build()));

        // Act
        consumer.consumeCustomerRegistered(event, topic, ack);

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Assert: se hace ack
        verify(ack).acknowledge();
        verify(ingestEventUseCase).execute(any(ProductEvent.class));
    }
}
