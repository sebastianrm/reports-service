package com.yanki.reports.domain.exception;

/**
 * Excepción lanzada cuando el rango de fechas proporcionado es inválido.
 *
 * <p>Se traduce a HTTP 400 con código {@code INVALID_DATE_RANGE}.
 */
public class InvalidDateRangeException extends BusinessException {

    /**
     * Constructor con mensaje descriptivo.
     *
     * @param message mensaje descriptivo del error
     */
    public InvalidDateRangeException(String message) {
        super("INVALID_DATE_RANGE", message);
    }
}