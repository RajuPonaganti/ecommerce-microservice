package com.ecommerce.order.events;

import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class OrderCreatedEventSerializer implements Serializer<OrderCreatedEvent> {
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.registerModule(new ParameterNamesModule());

	@Override
	public byte[] serialize(String topic, OrderCreatedEvent data) {
		try {
			return objectMapper.writeValueAsBytes(data);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
