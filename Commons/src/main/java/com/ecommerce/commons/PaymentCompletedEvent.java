package com.ecommerce.commons;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(UUID eventId, UUID orderId, Instant occurredAt, String paymentTransactionId) {

    @JsonCreator
    public PaymentCompletedEvent(
            @JsonProperty("eventId")              UUID eventId,
            @JsonProperty("orderId")              UUID orderId,
            @JsonProperty("occurredAt")           Instant occurredAt,
            @JsonProperty("paymentTransactionId") String paymentTransactionId) {
        this.eventId              = eventId;
        this.orderId              = orderId;
        this.occurredAt           = occurredAt;
        this.paymentTransactionId = paymentTransactionId;
    }
}
