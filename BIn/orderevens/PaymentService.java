package com.platform.payment.service;

import com.platform.order.saga.event.OrderSagaEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Payment Service — charges the customer once inventory is confirmed reserved.
 * On failure it publishes both its own failure event AND the compensation
 * request for Inventory Service — in choreography, the service that detects
 * the failure is responsible for triggering the compensations for steps
 * that already succeeded before it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOPIC_CONFIRMED = "payment.confirmed";
    private static final String TOPIC_FAILED = "payment.failed";
    private static final String TOPIC_INVENTORY_RELEASE = "inventory.release-requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentClient paymentClient;

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service")
    public void onInventoryReserved(InventoryReservedEvent event) {
        try {
            paymentClient.charge(event.orderId());
            kafkaTemplate.send(TOPIC_CONFIRMED, event.orderId().toString(),
                    new PaymentConfirmedEvent(event.orderId()));
            log.info("Payment confirmed for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Payment failed for order {}, requesting inventory release", event.orderId(), e);
            kafkaTemplate.send(TOPIC_FAILED, event.orderId().toString(),
                    new PaymentFailedEvent(event.orderId(), e.getMessage()));
            kafkaTemplate.send(TOPIC_INVENTORY_RELEASE, event.orderId().toString(),
                    new InventoryReleaseRequestedEvent(event.orderId()));
        }
    }

    /** Compensation: fulfilment failed downstream, refund the charge. */
    @KafkaListener(topics = "payment.refund-requested", groupId = "payment-service")
    public void onRefundRequested(PaymentRefundRequestedEvent event) {
        try {
            paymentClient.refund(event.orderId());
            log.info("Payment refunded (compensation) for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to refund payment for order {} during compensation", event.orderId(), e);
            // In production: retry queue / DLQ rather than swallow
        }
    }

    public interface PaymentClient {
        void charge(java.util.UUID orderId);
        void refund(java.util.UUID orderId);
    }
}
