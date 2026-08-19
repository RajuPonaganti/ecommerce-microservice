package com.ecommerce.order.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationFailedEvent(UUID eventId, UUID orderId, Instant occurredAt, String reason) {

    @JsonCreator
    public InventoryReservationFailedEvent(
            @JsonProperty("eventId")    UUID eventId,
            @JsonProperty("orderId")    UUID orderId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("reason")     String reason) {
        this.eventId    = eventId;
        this.orderId    = orderId;
        this.occurredAt = occurredAt;
        this.reason     = reason;
    }
}
