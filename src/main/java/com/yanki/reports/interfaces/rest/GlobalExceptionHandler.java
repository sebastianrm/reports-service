package com.yanki.reports.interfaces.rest;

import com.yanki.reports.domain.exception.BusinessException;
import com.yanki.reports.domain.exception.InvalidDateRangeException;
import com.yanki.reports.domain.exception.KafkaLagHighException;
import com.yanki.reports.domain.exception.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para mapear excepciones de dominio a respuestas HTTP estandarizadas.
 *
 * <p>Formato de error unificado {@code ApiError}:
 * <pre>
 * {
 *   "timestamp": "2026-11-14T18:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "code": "PRODUCT_NOT_FOUND",
 *   "message": "No report data found for cardId CARD-123",
 *   "path": "/api/reports/movements/cards/CARD-123",
 *   "details": []
 * }
 * </pre>
 */
@Slf4j
@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    /**
     * Maneja errores de negocio del dominio (ProductNotFound, InvalidDateRange, KafkaLagHigh).
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiError>> handleBusinessException(BusinessException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = mapToHttpStatus(ex.getCode());
        ApiError error = buildError(status, ex.getCode(), ex.getMessage(), path);

        if (status.value() >= 500) {
            log.error("Error de negocio (5xx): code={}, message={}, path={}", ex.getCode(), ex.getMessage(), path, ex);
        } else if (ex instanceof com.yanki.reports.domain.exception.KafkaLagHighException) {
            log.warn("Lag de Kafka alto detectado: path={}, message={}", path, ex.getMessage());
        } else if (ex instanceof com.yanki.reports.domain.exception.ProductNotFoundException) {
            log.warn("Recurso no encontrado: code={}, message={}, path={}", ex.getCode(), ex.getMessage(), path);
        } else {
            log.warn("Error de negocio: code={}, message={}, path={}", ex.getCode(), ex.getMessage(), path);
        }
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    /**
     * Maneja violaciones de validación de parámetros (@Valid, @NotNull, etc.).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ApiError>> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        ApiError error = buildError(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, path);
        log.warn("Violación de restricción de validación: path={}, violations={}", path, message);
        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    /**
     * Maneja errores inesperados (500).
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiError>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.error("Error interno no controlado: path={}, error={}, className={}",
                path, ex.getMessage(), ex.getClass().getName(), ex);
        ApiError error = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_GENERATION_FAILED",
                "Error interno generando el reporte", path);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

    private HttpStatus mapToHttpStatus(String code) {
        return switch (code) {
            case "PRODUCT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_DATE_RANGE", "INVALID_REPORT_TYPE", "INVALID_CUSTOMER_TYPE", "MISSING_REQUIRED_PARAMETER", "INVALID_PARAMETER" -> HttpStatus.BAD_REQUEST;
            case "KAFKA_LAG_HIGH" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ApiError buildError(HttpStatus status, String code, String message, String path) {
        return ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(path)
                .details(Collections.emptyList())
                .build();
    }
}