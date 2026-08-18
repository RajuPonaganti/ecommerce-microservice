package com.ecommerce.inventory.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InventoryReservationFailedEvent(UUID eventId, UUID orderId, Instant occurredAt, String reason) {
	@JsonCreator
	public InventoryReservationFailedEvent(@JsonProperty("eventId") UUID eventId, @JsonProperty("orderId") UUID orderId,
			@JsonProperty("occurrredAt") Instant occurredAt, @JsonProperty("reason") String reason) {
		this.eventId = eventId;
		this.occurredAt = occurredAt;
		this.orderId = orderId;
		this.reason = reason;

	}
	
}
