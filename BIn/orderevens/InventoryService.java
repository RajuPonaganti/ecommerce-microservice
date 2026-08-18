package com.platform.inventory.service;

import com.platform.order.saga.event.OrderSagaEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Inventory Service — reacts to order creation by attempting a reservation,
 * and reacts to downstream failures by releasing stock it previously reserved.
 * It has no knowledge of the payment or fulfilment services beyond the
 * event contracts it consumes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String TOPIC_RESERVED = "inventory.reserved";
    private static final String TOPIC_RESERVE_FAILED = "inventory.reservation-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryClient inventoryClient;

    @KafkaListener(topics = "order.created", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            inventoryClient.reserve(event.orderId());
            kafkaTemplate.send(TOPIC_RESERVED, event.orderId().toString(),
                    new InventoryReservedEvent(event.orderId()));
            log.info("Inventory reserved for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Inventory reservation failed for order {}", event.orderId(), e);
            kafkaTemplate.send(TOPIC_RESERVE_FAILED, event.orderId().toString(),
                    new InventoryReservationFailedEvent(event.orderId(), e.getMessage()));
        }
    }

    /** Compensation: payment failed downstream, release the stock we reserved. */
    @KafkaListener(topics = "inventory.release-requested", groupId = "inventory-service")
    public void onInventoryReleaseRequested(InventoryReleaseRequestedEvent event) {
        try {
            inventoryClient.release(event.orderId());
            log.info("Inventory released (compensation) for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to release inventory for order {} during compensation", event.orderId(), e);
            // In production: retry queue / DLQ rather than swallow
        }
    }

    public interface InventoryClient {
        void reserve(java.util.UUID orderId);
        void release(java.util.UUID orderId);
    }
}
