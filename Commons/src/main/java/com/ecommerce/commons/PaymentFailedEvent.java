package com.ecommerce.commons;

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
}
