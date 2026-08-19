package com.ecommerce.commons;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(UUID eventId, UUID orderId, UUID userId,
                                   BigDecimal finalAmount, Instant occurredAt) {
    @JsonCreator
    public OrderConfirmedEvent(
            @JsonProperty("eventId")     UUID eventId,
            @JsonProperty("orderId")     UUID orderId,
            @JsonProperty("userId")      UUID userId,
            @JsonProperty("finalAmount") BigDecimal finalAmount,
            @JsonProperty("occurredAt")  Instant occurredAt) {
        this.eventId     = eventId;
        this.orderId     = orderId;
        this.userId      = userId;
        this.finalAmount = finalAmount;
        this.occurredAt  = occurredAt;
    }
}
