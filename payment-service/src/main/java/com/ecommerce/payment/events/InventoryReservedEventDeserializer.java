package com.ecommerce.payment.events;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class InventoryReservedEventDeserializer implements Deserializer<InventoryReservedEvent> {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule());

    @Override
    public InventoryReservedEvent deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return mapper.readValue(data, InventoryReservedEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing InventoryReservedEvent", e);
        }
    }
}