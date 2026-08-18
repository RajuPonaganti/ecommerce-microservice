package com.ecommerce.payment.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(UUID eventId, UUID orderId, Instant occurredAt, String reason) {

    @JsonCreator
    public PaymentFailedEvent(
            @JsonProperty("eventId")    UUID eventId,
            @JsonProperty("orderId")    UUID orderId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("reason")     String reason) {
        this.eventId    = eventId;
        this.orderId    = orderId;
        this.occurredAt = occurredAt;
        this.reason     = reason;
    }

    /** Convenience constructor — auto-generates eventId and timestamp. */
    public PaymentFailedEvent(UUID orderId, String reason) {
        this(UUID.randomUUID(), orderId, Instant.now(), reason);
    }
}
