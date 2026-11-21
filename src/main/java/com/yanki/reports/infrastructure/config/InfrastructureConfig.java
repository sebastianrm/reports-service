package com.yanki.reports.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yanki.reports.domain.port.out.CachePort;
import com.yanki.reports.domain.port.out.KafkaLagPort;
import com.yanki.reports.domain.port.out.ProcessedEventPort;
import com.yanki.reports.infrastructure.cache.adapter.RedisCacheAdapter;
import com.yanki.reports.infrastructure.messaging.adapter.KafkaLagAdapter;
import com.yanki.reports.infrastructure.persistence.adapter.ProcessedEventAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Configuración de beans de infraestructura (Adaptadores secundarios).
 *
 * <p>Registra las implementaciones de los puertos de salida ({@code domain.port.out.*})
 * para que estén disponibles para inyección en los servicios de dominio ({@code domain.service.*}).
 */
@Configuration
public class InfrastructureConfig {

    @Bean
    public ProcessedEventPort processedEventPort(
            com.yanki.reports.infrastructure.persistence.adapter.ProcessedEventAdapter adapter) {
        return adapter;
    }

    @Bean
    public CachePort cachePort(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return new RedisCacheAdapter(reactiveRedisTemplate);
    }

    @Bean
    public KafkaLagPort kafkaLagPort(KafkaAdmin kafkaAdmin) {
        return new KafkaLagAdapter(kafkaAdmin);
    }

    /**
     * Configura ReactiveRedisTemplate con serialización JSON para valores.
     * Se registra JavaTimeModule para soportar tipos java.time (LocalDateTime, Instant, etc.).
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(mapper);
        RedisSerializationContext<String, Object> context = RedisSerializationContext.<String, Object>newSerializationContext()
                .key(new StringRedisSerializer())
                .value(serializer)
                .hashKey(new StringRedisSerializer())
                .hashValue(serializer)
                .build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}