package com.ecommerce.inventory.events;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class OrderCreatedEventDeserializer implements Deserializer<OrderCreatedEvent> {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule());

    @Override
    public OrderCreatedEvent deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return mapper.readValue(data, OrderCreatedEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing OrderCreatedEvent", e);
        }
    }
}