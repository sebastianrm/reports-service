package com.yanki.reports.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de consumidores Kafka para reports-service.
 *
 * <p>Define fábricas de contenedores de listener individuales para cada tipo de evento
 * consumido, configurando el {@link JsonDeserializer} con el tipo por defecto correcto.
 * Esto resuelve el error "No type information in headers and no default type provided"
 * que ocurría cuando los productores externos (customers-service, accounts-service,
 * cards-service, credits-service, wallet-service, transfers-service) publicaban eventos
 * sin información de tipo en los headers.</p>
 *
 * <p>Cada factory está asociada a un {@link com.yanki.reports.infrastructure.messaging.consumer.ReportsEventConsumer}
 * mediante el atributo {@code containerFactory} de {@code @KafkaListener}.</p>
 *
 * <p>Reports-service consume eventos de 19 topics diferentes de 6 microservicios distintos.</p>
 *
 * @author Sebastián Retamal
 * @contact semorale@emeal.nttdata.com
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:reports-service}")
    private String groupId;

    /**
     * Crea el mapa de propiedades base para todos los consumidores Kafka.
     *
     * @return propiedades comunes de consumidor
     */
    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    // -----------------------------------------------------------------------
    // Eventos de customers-service (3)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de cliente registrado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CustomerRegisteredEvent> customerRegisteredKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CustomerRegisteredEvent.class);
    }

    /**
     * Factory para eventos de cliente actualizado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CustomerUpdatedEvent> customerUpdatedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CustomerUpdatedEvent.class);
    }

    /**
     * Factory para eventos de cliente eliminado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CustomerDeletedEvent> customerDeletedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CustomerDeletedEvent.class);
    }

    // -----------------------------------------------------------------------
    // Eventos de accounts-service (3)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de cuenta abierta.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.AccountOpenedEvent> accountOpenedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.AccountOpenedEvent.class);
    }

    /**
     * Factory para eventos de cuenta cerrada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.AccountClosedEvent> accountClosedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.AccountClosedEvent.class);
    }

    /**
     * Factory para eventos de movimiento de cuenta registrado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.AccountMovementRegisteredEvent> accountMovementRegisteredKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.AccountMovementRegisteredEvent.class);
    }

    // -----------------------------------------------------------------------
    // Eventos de cards-service (3)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de tarjeta abierta.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CardOpenedEvent> cardOpenedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CardOpenedEvent.class);
    }

    /**
     * Factory para eventos de tarjeta cerrada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CardClosedEvent> cardClosedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CardClosedEvent.class);
    }

    /**
     * Factory para eventos de consumo de tarjeta registrado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CardConsumptionRegisteredEvent> cardConsumptionRegisteredKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CardConsumptionRegisteredEvent.class);
    }

    // -----------------------------------------------------------------------
    // Eventos de credits-service (3)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de crédito abierto.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CreditOpenedEvent> creditOpenedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CreditOpenedEvent.class);
    }

    /**
     * Factory para eventos de pago de crédito registrado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CreditPaymentRegisteredEvent> creditPaymentRegisteredKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CreditPaymentRegisteredEvent.class);
    }

    /**
     * Factory para eventos de crédito en mora.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.CreditOverdueEvent> creditOverdueKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.CreditOverdueEvent.class);
    }

    // -----------------------------------------------------------------------
    // Eventos de wallet-service (4)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de wallet abierta.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.WalletOpenedEvent> walletOpenedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.WalletOpenedEvent.class);
    }

    /**
     * Factory para eventos de transacción de wallet registrada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.WalletTransactionRegisteredEvent> walletTransactionRegisteredKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.WalletTransactionRegisteredEvent.class);
    }

    /**
     * Factory para eventos de recarga de wallet solicitada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.WalletTopUpRequestedEvent> walletTopUpRequestedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.WalletTopUpRequestedEvent.class);
    }

    /**
     * Factory para eventos de recarga de wallet liquidada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.WalletTopUpSettledEvent> walletTopUpSettledKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.WalletTopUpSettledEvent.class);
    }

    // -----------------------------------------------------------------------
    // Eventos de transfers-service (2)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de transferencia solicitada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.TransferRequestedEvent> transferRequestedKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.TransferRequestedEvent.class);
    }

    /**
     * Factory para eventos de transferencia liquidada.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.TransferSettledEvent> transferSettledKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.TransferSettledEvent.class);
    }

    // -----------------------------------------------------------------------
    // Eventos de debit payments (1)
    // -----------------------------------------------------------------------

    /**
     * Factory para eventos de pago débito liquidado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.yanki.reports.domain.event.DebitPaymentSettledEvent> debitPaymentSettledKafkaListenerContainerFactory() {
        return createFactory(com.yanki.reports.domain.event.DebitPaymentSettledEvent.class);
    }

    // -----------------------------------------------------------------------
    // Método auxiliar
    // -----------------------------------------------------------------------

    /**
     * Crea una factory de contenedor de listener para un tipo de evento específico.
     *
     * <p>Se crea una instancia explícita de {@link JsonDeserializer} con {@code useTypeHeaders = false}
     * para ignorar los headers {@code __TypeId__} que envían los productores externos
     * (customers-service, accounts-service, cards-service, etc.). De este modo, el deserializador mapea siempre el JSON
     * a la clase local definida en reports-service, sin depender del paquete original del productor.</p>
     *
     * <p>El {@link ErrorHandlingDeserializer} envuelve al {@link JsonDeserializer} para manejar
     * mensajes corruptos ("poison pills") sin bloquear la partición.</p>
     *
     * @param eventType clase del evento a deserializar
     * @param <T>       tipo del evento
     * @return factory configurada con el tipo por defecto
     */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactory(Class<T> eventType) {
        // JsonDeserializer explícito que ignora los headers __TypeId__ del productor
        JsonDeserializer<T> jsonDeserializer = new JsonDeserializer<>(eventType, false);
        jsonDeserializer.addTrustedPackages("com.yanki.*");

        // ErrorHandlingDeserializer envuelve al JsonDeserializer para manejar poison pills
        ErrorHandlingDeserializer<String> keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
        ErrorHandlingDeserializer<T> valueDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);

        Map<String, Object> props = consumerProps();
        ConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, keyDeserializer, valueDeserializer);

        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
