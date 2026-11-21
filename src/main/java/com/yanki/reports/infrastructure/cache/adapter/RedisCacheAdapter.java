package com.yanki.reports.infrastructure.cache.adapter;

import com.yanki.reports.domain.port.out.CachePort;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Adaptador de salida para caché distribuido Redis.
 *
 * <p>Implementa patrón cache-aside con serialización JSON.
 * Claves con prefijo {@code yanki:reports:cache:}.
 * TTL configurable por operación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> Maybe<T> get(String key, Class<T> type) {
        log.debug("Consultando caché Redis: key={}", key);
        return RxJava3Adapter.monoToMaybe(
                redisTemplate.opsForValue().get(key)
                        .cast(type)
                        .doOnNext(v -> log.info("Cache HIT en Redis: key={}", key))
                        .doOnError(e -> log.warn("Error en cache GET de Redis: key={}, error={}", key, e.getMessage()))
                        .onErrorResume(e -> {
                            log.warn("Cache GET falló, tratando como MISS: key={}", key);
                            return Mono.empty();
                        })
        );
    }

    @Override
    public <T> Single<T> put(String key, T value, Duration ttl) {
        log.debug("Guardando en caché Redis: key={}, TTL={}", key, ttl);
        return RxJava3Adapter.monoToSingle(
                redisTemplate.opsForValue().set(key, value, ttl)
                        .thenReturn(value)
                        .doOnSuccess(v -> log.info("Cache PUT en Redis exitoso: key={}, TTL={}", key, ttl))
                        .doOnError(e -> log.error("Error en cache PUT de Redis: key={}, error={}", key, e.getMessage(), e))
        );
    }

    @Override
    public Single<Void> evict(String key) {
        log.info("Evictando clave de caché Redis: key={}", key);
        return RxJava3Adapter.monoToSingle(
                redisTemplate.delete(key)
                        .then()
                        .doOnSuccess(v -> log.info("Cache EVICT en Redis exitoso: key={}", key))
                        .doOnError(e -> log.error("Error en cache EVICT de Redis: key={}, error={}", key, e.getMessage(), e))
        );
    }

    @Override
    public Single<Void> evictByPattern(String pattern) {
        log.info("Evictando claves de caché Redis por patrón: pattern={}", pattern);
        return RxJava3Adapter.monoToSingle(
                redisTemplate.keys(pattern)
                        .collect(Collectors.toSet())
                        .flatMapMany(keys -> {
                            if (keys.isEmpty()) {
                                log.warn("No se encontraron claves para el patrón: pattern={}", pattern);
                                return Flux.empty();
                            }
                            log.debug("Se encontraron {} claves para el patrón: pattern={}", keys.size(), pattern);
                            return redisTemplate.delete(keys.toArray(new String[0]));
                        })
                        .then()
                        .doOnSuccess(v -> log.info("Cache EVICT BY PATTERN en Redis exitoso: pattern={}", pattern))
                        .doOnError(e -> log.error("Error en cache EVICT BY PATTERN de Redis: pattern={}, error={}",
                                pattern, e.getMessage(), e))
        );
    }
}