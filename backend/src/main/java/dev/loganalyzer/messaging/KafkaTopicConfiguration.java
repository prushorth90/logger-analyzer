package dev.loganalyzer.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {
    @Bean
    NewTopic logsRawTopic() {
        return TopicBuilder.name(LogIngestionPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}