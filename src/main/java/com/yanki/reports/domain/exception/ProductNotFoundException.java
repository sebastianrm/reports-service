package com.yanki.reports.domain.exception;

/**
 * Excepción lanzada cuando no se encuentran eventos para un producto/recurso consultado.
 *
 * <p>Se traduce a HTTP 404 con código {@code PRODUCT_NOT_FOUND}.
 */
public class ProductNotFoundException extends BusinessException {

    /**
     * Constructor con identificador del recurso no encontrado.
     *
     * @param resourceType tipo de recurso (productId, cardId, customerId, etc.)
     * @param resourceId   identificador del recurso
     */
    public ProductNotFoundException(String resourceType, String resourceId) {
        super("PRODUCT_NOT_FOUND", "No report data found for " + resourceType + " " + resourceId);
    }
}