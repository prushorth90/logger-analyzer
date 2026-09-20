package dev.loganalyzer.messaging;

import dev.loganalyzer.observability.ApplicationMetrics;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(LogIngestionRetryProperties.class)
public class KafkaTopicConfiguration {
    @Bean
    NewTopic logsRawTopic() {
        return TopicBuilder.name(LogIngestionPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic logsPersistedTopic() {
        return TopicBuilder.name(LogPersistedEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

            @Bean
            NewTopic logsRawDeadLetterTopic() {
            return TopicBuilder.name(LogIngestionPublisher.DEAD_LETTER_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
            }

            @Bean
            DefaultErrorHandler logIngestionErrorHandler(
                KafkaTemplate<Object, Object> kafkaTemplate,
                LogIngestionRetryProperties retryProperties,
                ApplicationMetrics metrics) {
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(LogIngestionPublisher.DEAD_LETTER_TOPIC,
                    record.partition()));
            DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(retryProperties.interval().toMillis(), retryProperties.maxAttempts() - 1L));
            errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
            errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> metrics.ingestionRetried());
            return errorHandler;
            }

        @Bean
        ConcurrentKafkaListenerContainerFactory<Object, Object> deadLetterKafkaListenerContainerFactory(
                ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                ConsumerFactory<Object, Object> consumerFactory,
                LogIngestionRetryProperties retryProperties) {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            configurer.configure(factory, consumerFactory);
            factory.setCommonErrorHandler(new DefaultErrorHandler(
                    new FixedBackOff(retryProperties.interval().toMillis(), retryProperties.maxAttempts() - 1L)));
            return factory;
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<Object, Object> indexingKafkaListenerContainerFactory(
                ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                ConsumerFactory<Object, Object> consumerFactory,
                LogIngestionRetryProperties retryProperties) {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            configurer.configure(factory, consumerFactory);
            DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                    new FixedBackOff(retryProperties.interval().toMillis(), retryProperties.maxAttempts() - 1L));
            errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
            factory.setCommonErrorHandler(errorHandler);
            return factory;
        }
}