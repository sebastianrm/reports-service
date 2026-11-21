package com.yanki.reports.domain.exception;

/**
 * Excepción base para errores de negocio en el dominio de reportes.
 *
 * <p>Se utiliza para señalar violaciones de reglas de negocio, validaciones fallidas
 * o condiciones operativas esperadas (ej. lag de Kafka alto) que deben traducirse
 * a códigos HTTP específicos por el {@code GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    private final String code;

    /**
     * Constructor con código de error y mensaje.
     *
     * @param code    código de error técnico (ej. PRODUCT_NOT_FOUND, INVALID_DATE_RANGE)
     * @param message mensaje descriptivo para el usuario/cliente
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructor con código, mensaje y causa raíz.
     *
     * @param code    código de error técnico
     * @param message mensaje descriptivo
     * @param cause   excepción original que causó el error
     */
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Obtiene el código de error técnico.
     *
     * @return código de error (ej. PRODUCT_NOT_FOUND)
     */
    public String getCode() {
        return code;
    }
}