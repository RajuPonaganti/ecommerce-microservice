package com.ecommerce.payment.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InventoryReservedEvent(UUID eventId, UUID orderId, Instant occurredAt, UUID reservationId) {
	@JsonCreator
	public InventoryReservedEvent(@JsonProperty("eventId") UUID eventId, @JsonProperty("orderId") UUID orderId,
			@JsonProperty("occurrredAt") Instant occurredAt, @JsonProperty("reservationId") UUID reservationId) {
		this.eventId = eventId;
		this.occurredAt = occurredAt;
		this.orderId = orderId;
		this.reservationId = reservationId;

	}

}