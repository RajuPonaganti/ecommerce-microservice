package com.ecommerce.notification.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumed from inventory.stock-confirmed.v1.
 * Published by Inventory Service after payment succeeds and reservation is confirmed.
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
}
