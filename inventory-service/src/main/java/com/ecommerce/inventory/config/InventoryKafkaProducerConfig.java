/*
 * package com.ecommerce.inventory.config;
 * 
 * import java.util.HashMap; import java.util.Map;
 * 
 * import org.apache.kafka.clients.producer.ProducerConfig; import
 * org.apache.kafka.common.serialization.StringSerializer; import
 * org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration; import
 * org.springframework.kafka.core.DefaultKafkaProducerFactory; import
 * org.springframework.kafka.core.KafkaTemplate; import
 * org.springframework.kafka.core.ProducerFactory;
 * 
 * import com.ecommerce.inventory.events.InventoryReservedEventSerializer;
 * 
 * @Configuration public class InventoryKafkaProducerConfig {
 * 
 * @Bean public ProducerFactory<String, Object> producerConfig(){ Map<String,
 * Object> map = new HashMap<>();
 * map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
 * map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
 * StringSerializer.class.getName());
 * map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
 * InventoryReservedEventSerializer.class.getName());
 * map.put(ProducerConfig.ACKS_CONFIG, "all");
 * map.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // dedupes producer
 * retries map.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); return new
 * DefaultKafkaProducerFactory<>(map);
 * 
 * }
 * 
 * 
 * @Bean public KafkaTemplate<String, Object>
 * kafkaTemplate(ProducerFactory<String, Object> pf){ return new
 * KafkaTemplate<>(pf);
 * 
 * }
 * 
 * }
 */