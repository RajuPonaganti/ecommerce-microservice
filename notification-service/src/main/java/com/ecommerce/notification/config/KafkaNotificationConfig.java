package com.ecommerce.notification.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer config with DLQ.
 * After 3 failures (1s apart), the message is sent to {topic}.DLQ.
 */
@Configuration
public class KafkaNotificationConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> notificationListenerFactory(
            ConsumerFactory<String, Object> cf,
            KafkaTemplate<String, Object> kafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(5); // 5 consumer threads

        // Dead-letter: after 3 failures → send to {topic}.DLQ
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, ex) -> new TopicPartition(
                                record.topic() + ".DLQ",
                                record.partition())),
                new FixedBackOff(1000L, 3L) // 1s delay, 3 retries
        ));

        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            org.springframework.kafka.core.ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}
