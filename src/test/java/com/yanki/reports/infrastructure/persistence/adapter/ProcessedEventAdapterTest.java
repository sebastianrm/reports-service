package com.yanki.reports.infrastructure.persistence.adapter;

import com.yanki.reports.domain.model.ProcessedEvent;
import com.yanki.reports.infrastructure.persistence.document.ProcessedEventDocument;
import com.yanki.reports.infrastructure.persistence.repository.SpringDataProcessedEventRepository;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ProcessedEventAdapter.
 *
 * <p>Cubre el error ya solucionado:
 * <ul>
 *   <li>NoSuchElementException en tryRegister cuando el evento no existe en MongoDB
 *   (se solucionó cambiando {@code .toSingle()} por {@code .isEmpty()})</li>
 *   <li>Evento duplicado detectado correctamente</li>
 *   <li>Registro exitoso de evento nuevo</li>
 * </ul>
 */
class ProcessedEventAdapterTest {

    @Mock
    private SpringDataProcessedEventRepository repository;

    private ProcessedEventAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new ProcessedEventAdapter(repository);
    }

    /**
     * Verifica que cuando el evento NO existe en MongoDB (caso normal de primer procesamiento),
     * tryRegister devuelve true sin lanzar NoSuchElementException.
     *
     * <p>Este era el bug original:
     * {@code NoSuchElementException: The MaybeSource is empty}
     * causado por {@code .toSingle()} en un Maybe.empty().
     */
    @Test
    void tryRegister_newEvent_shouldReturnTrueWithoutException() {
        // Arrange: evento nuevo, no existe en MongoDB
        ProcessedEvent event = ProcessedEvent.builder()
                .id("yanki.account.opened.v1:6afc4a09cec93304aae75c2d:2026-11-17T12:35:21.288419002")
                .processedAt(LocalDateTime.now())
                .build();

        // findById devuelve Mono.empty() (evento no existe)
        when(repository.findById(anyString())).thenReturn(Mono.empty());
        // save guarda exitosamente
        when(repository.save(any(ProcessedEventDocument.class)))
                .thenReturn(Mono.just(ProcessedEventDocument.builder()
                        .id(event.getId())
                        .processedAt(event.getProcessedAt())
                        .build()));

        // Act
        Single<Boolean> result = adapter.tryRegister(event);

        // Assert: no NoSuchElementException, devuelve true (evento nuevo)
        TestObserver<Boolean> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(true);
    }

    /**
     * Verifica que cuando el evento YA existe en MongoDB (duplicado),
     * tryRegister devuelve false sin error.
     */
    @Test
    void tryRegister_duplicateEvent_shouldReturnFalse() {
        // Arrange: evento ya existe en MongoDB
        ProcessedEvent event = ProcessedEvent.builder()
                .id("yanki.account.opened.v1:6afc4a09cec93304aae75c2d:2026-11-17T12:35:21.288419002")
                .processedAt(LocalDateTime.now())
                .build();

        ProcessedEventDocument existingDoc = ProcessedEventDocument.builder()
                .id(event.getId())
                .processedAt(event.getProcessedAt())
                .build();

        // findById devuelve el documento existente
        when(repository.findById(anyString())).thenReturn(Mono.just(existingDoc));

        // Act
        Single<Boolean> result = adapter.tryRegister(event);

        // Assert: devuelve false (duplicado)
        TestObserver<Boolean> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(false);
    }

    /**
     * Verifica que findById devuelve Maybe.empty() cuando el evento no existe.
     */
    @Test
    void findById_nonExistentEvent_shouldReturnEmpty() {
        // Arrange
        when(repository.findById("nonexistent-id")).thenReturn(Mono.empty());

        // Act
        Maybe<ProcessedEvent> result = adapter.findById("nonexistent-id");

        // Assert
        TestObserver<ProcessedEvent> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertNoValues();
    }

    /**
     * Verifica que findById devuelve el evento cuando existe.
     */
    @Test
    void findById_existingEvent_shouldReturnEvent() {
        // Arrange
        ProcessedEventDocument doc = ProcessedEventDocument.builder()
                .id("existing-id")
                .processedAt(LocalDateTime.of(2026, 11, 17, 12, 0))
                .build();
        when(repository.findById("existing-id")).thenReturn(Mono.just(doc));

        // Act
        Maybe<ProcessedEvent> result = adapter.findById("existing-id");

        // Assert
        TestObserver<ProcessedEvent> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(event -> "existing-id".equals(event.getId()));
    }

    /**
     * Verifica que cuando el repositorio falla, el error se propaga correctamente.
     */
    @Test
    void tryRegister_repositoryError_shouldPropagateError() {
        // Arrange
        ProcessedEvent event = ProcessedEvent.builder()
                .id("yanki.test.event:test:2026-11-17T12:00:00")
                .processedAt(LocalDateTime.now())
                .build();

        when(repository.findById(anyString()))
                .thenReturn(Mono.error(new RuntimeException("DB connection failed")));

        // Act
        Single<Boolean> result = adapter.tryRegister(event);

        // Assert: el error se propaga (no se traga silenciosamente)
        TestObserver<Boolean> testObserver = result.test();
        testObserver.assertError(RuntimeException.class);
    }
}
