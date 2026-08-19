package com.ecommerce.commons;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderCreatedEvent(UUID eventId, UUID orderId, Instant occurrredAt, List<OrderItemDetailDTO> items,
		BigDecimal finalAmount, UUID userId) {
	

	@JsonCreator
	public OrderCreatedEvent(@JsonProperty("eventId") UUID eventId, @JsonProperty("orderId") UUID orderId,
			@JsonProperty("occurrredAt") Instant occurrredAt, @JsonProperty("items") List<OrderItemDetailDTO> items,
			@JsonProperty("finalAmount") BigDecimal finalAmount, @JsonProperty("userId") UUID userId) {
		this.eventId = eventId;
		this.orderId = orderId;
		this.occurrredAt = occurrredAt;
		this.items = items;
		this.finalAmount = finalAmount;
		this.userId = userId;
	}



	

}
