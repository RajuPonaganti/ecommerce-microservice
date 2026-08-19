package com.ecommerce.commons;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Published by Payment Service when payment fails.
 * Consumed by Inventory Service to release the stock reservation.
 */
public record InventoryReleaseRequestedEvent(UUID eventId, UUID orderId, Instant occurredAt, String reason) {

    @JsonCreator
    public InventoryReleaseRequestedEvent(
            @JsonProperty("eventId")    UUID eventId,
            @JsonProperty("orderId")    UUID orderId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("reason")     String reason) {
        this.eventId    = eventId;
        this.orderId    = orderId;
        this.occurredAt = occurredAt;
        this.reason     = reason;
    }

    public InventoryReleaseRequestedEvent(UUID orderId, String reason) {
        this(UUID.randomUUID(), orderId, Instant.now(), reason);
    }
}
