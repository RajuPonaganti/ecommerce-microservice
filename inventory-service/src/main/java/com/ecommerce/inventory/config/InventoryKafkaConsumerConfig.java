package com.ecommerce.inventory.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ecommerce.inventory.events.OrderCreatedEventDeserializer;

@Configuration
public class InventoryKafkaConsumerConfig {
	@Bean
	ConsumerFactory<String, Object> consumerfactory(){
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderCreatedEventDeserializer.class.getName());
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.inventory.events");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		
		return new DefaultKafkaConsumerFactory<>(config);
	  
  }
	
	@Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cf) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Dead-letter topic for events that fail after retries — don't let
        // a poison message block the whole partition forever
		/*
		 * factory.setCommonErrorHandler(new DefaultErrorHandler( new
		 * DeadLetterPublishingRecoverer(kafkaTemplate()), new FixedBackOff(1000L, 3)));
		 */
        return factory;
    }
}
