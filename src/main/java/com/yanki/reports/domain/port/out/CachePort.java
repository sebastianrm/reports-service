package com.yanki.reports.domain.port.out;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;

/**
 * Puerto de salida para caché distribuido (Redis).
 *
 * <p>Implementa patrón cache-aside con TTL configurable.
 * Claves con prefijo {@code yanki:reports:cache:}.
 */
public interface CachePort {

    /**
     * Obtiene un valor del caché.
     *
     * @param key   clave del caché
     * @param type  tipo de la clase para deserialización
     * @param <T>   tipo del valor
     * @return {@link Maybe} con el valor si existe, vacío si no (cache miss)
     */
    <T> Maybe<T> get(String key, Class<T> type);

    /**
     * Almacena un valor en el caché con TTL.
     *
     * @param key      clave del caché
     * @param value    valor a almacenar
     * @param ttl      tiempo de vida
     * @return {@link Completable} que completa cuando se almacena
     */
    <T> Single<T> put(String key, T value, Duration ttl);

    /**
     * Elimina un valor del caché.
     *
     * @param key clave a eliminar
     * @return {@link Completable} que completa cuando se elimina
     */
    Single<Void> evict(String key);

    /**
     * Elimina múltiples claves por patrón.
     *
     * @param pattern patrón de claves (ej. "yanki:reports:cache:*")
     * @return {@link Completable} que completa cuando se eliminan
     */
    Single<Void> evictByPattern(String pattern);
}