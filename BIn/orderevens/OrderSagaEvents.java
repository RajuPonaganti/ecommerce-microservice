package com.platform.order.saga.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payloads exchanged between services in the choreography saga.
 * Each service only knows about the events it publishes and the events
 * it listens to — there is no central orchestrator.
 *
 * Topic naming convention used below: "order.<event-name>"
 */
public class OrderSagaEvents {

    // Published by Order Service after the order row is persisted.
    public record OrderCreatedEvent(UUID orderId, UUID customerId, BigDecimal amount) implements Serializable {}

    // Published by Inventory Service.
    public record InventoryReservedEvent(UUID orderId) implements Serializable {}
    public record InventoryReservationFailedEvent(UUID orderId, String reason) implements Serializable {}

    // Published by Payment Service.
    public record PaymentConfirmedEvent(UUID orderId) implements Serializable {}
    public record PaymentFailedEvent(UUID orderId, String reason) implements Serializable {}

    // Compensation request published by Payment Service (or Order Service)
    // when a later step fails and already-reserved stock must be released.
    public record InventoryReleaseRequestedEvent(UUID orderId) implements Serializable {}

    // Published by Fulfilment Service.
    public record FulfilmentStartedEvent(UUID orderId) implements Serializable {}
    public record FulfilmentFailedEvent(UUID orderId, String reason) implements Serializable {}

    // Refund compensation request published when fulfilment fails after payment succeeded.
    public record PaymentRefundRequestedEvent(UUID orderId) implements Serializable {}
}
