package com.ecommerce.commons;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to inventory.stock-confirmed.v1 after payment succeeds.
 * Consumed by Notification Service to inform the customer.
 */
public record StockConfirmedEvent(UUID eventId, UUID orderId, UUID reservationId,
                                   String paymentTransactionId, Instant occurredAt) {

    @JsonCreator
    public StockConfirmedEvent(
            @JsonProperty("eventId")              UUID eventId,
            @JsonProperty("orderId")              UUID orderId,
            @JsonProperty("reservationId")        UUID reservationId,
            @JsonProperty("paymentTransactionId") String paymentTransactionId,
            @JsonProperty("occurredAt")           Instant occurredAt) {
        this.eventId              = eventId;
        this.orderId              = orderId;
        this.reservationId        = reservationId;
        this.paymentTransactionId = paymentTransactionId;
        this.occurredAt           = occurredAt;
    }

    public StockConfirmedEvent(UUID orderId, UUID reservationId, String paymentTransactionId) {
        this(UUID.randomUUID(), orderId, reservationId, paymentTransactionId, Instant.now());
    }
}
