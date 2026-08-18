package com.platform.order.service;

import com.platform.order.saga.event.OrderSagaEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order Service — owns order creation and final status updates.
 * It does NOT tell other services what to do (that's the orchestrator
 * pattern); it only publishes facts about itself and reacts to facts
 * published by others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String TOPIC_ORDER_CREATED = "order.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    /** Entry point: create the order, persist it, announce it happened. */
    public UUID createOrder(UUID customerId, BigDecimal amount) {
        UUID orderId = UUID.randomUUID();
        orderRepository.save(orderId, customerId, amount, "CREATED");

        kafkaTemplate.send(TOPIC_ORDER_CREATED, orderId.toString(),
                new OrderCreatedEvent(orderId, customerId, amount));
        log.info("Order {} created, event published", orderId);
        return orderId;
    }

    /** Saga succeeded end-to-end. */
    @KafkaListener(topics = "fulfilment.started", groupId = "order-service")
    public void onFulfilmentStarted(FulfilmentStartedEvent event) {
        orderRepository.updateStatus(event.orderId(), "COMPLETED");
        log.info("Order {} COMPLETED", event.orderId());
    }

    /** Inventory could not be reserved — saga ends immediately, nothing to compensate. */
    @KafkaListener(topics = "inventory.reservation-failed", groupId = "order-service")
    public void onInventoryReservationFailed(InventoryReservationFailedEvent event) {
        orderRepository.updateStatus(event.orderId(), "CANCELLED");
        log.warn("Order {} CANCELLED — inventory reservation failed: {}", event.orderId(), event.reason());
    }

    /** Payment failed — Payment Service has already requested inventory release. */
    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    public void onPaymentFailed(PaymentFailedEvent event) {
        orderRepository.updateStatus(event.orderId(), "CANCELLED");
        log.warn("Order {} CANCELLED — payment failed: {}", event.orderId(), event.reason());
    }

    /** Fulfilment failed after payment succeeded — refund + release already requested by Fulfilment Service. */
    @KafkaListener(topics = "fulfilment.failed", groupId = "order-service")
    public void onFulfilmentFailed(FulfilmentFailedEvent event) {
        orderRepository.updateStatus(event.orderId(), "CANCELLED");
        log.warn("Order {} CANCELLED — fulfilment failed: {}", event.orderId(), event.reason());
    }

    public interface OrderRepository {
        void save(UUID orderId, UUID customerId, BigDecimal amount, String status);
        void updateStatus(UUID orderId, String status);
    }
}
