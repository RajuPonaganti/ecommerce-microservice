package com.platform.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Order saga orchestrator implemented as a plain sequential try-catch flow
 * (no Spring State Machine). Each step is invoked in order; any failure
 * triggers the compensating actions for the steps that already succeeded,
 * walking backwards through the saga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final FulfilmentClient fulfilmentClient;
    private final OrderRepository orderRepository;

    public enum OrderState {
        CREATED, INVENTORY_RESERVING, INVENTORY_RESERVED,
        PAYMENT_PROCESSING, PAYMENT_CONFIRMED,
        FULFILMENT_STARTED, COMPLETED,
        INVENTORY_RESERVE_FAILED, PAYMENT_FAILED, CANCELLED
    }

    /**
     * Runs the full order saga for the given orderId.
     * Returns the final OrderState the order ended up in.
     */
    public OrderState execute(UUID orderId) {
        updateState(orderId, OrderState.CREATED);

        // Step 1: Reserve inventory
        try {
            updateState(orderId, OrderState.INVENTORY_RESERVING);
            inventoryClient.reserve(orderId);
            updateState(orderId, OrderState.INVENTORY_RESERVED);
        } catch (Exception e) {
            log.error("Inventory reservation failed for order {}", orderId, e);
            updateState(orderId, OrderState.INVENTORY_RESERVE_FAILED);
            updateState(orderId, OrderState.CANCELLED);
            return OrderState.CANCELLED;
        }

        // Step 2: Process payment
        try {
            updateState(orderId, OrderState.PAYMENT_PROCESSING);
            paymentClient.charge(orderId);
            updateState(orderId, OrderState.PAYMENT_CONFIRMED);
        } catch (Exception e) {
            log.error("Payment failed for order {}, releasing inventory", orderId, e);
            updateState(orderId, OrderState.PAYMENT_FAILED);
            compensateInventory(orderId);
            updateState(orderId, OrderState.CANCELLED);
            return OrderState.CANCELLED;
        }

        // Step 3: Start fulfilment
        try {
            updateState(orderId, OrderState.FULFILMENT_STARTED);
            fulfilmentClient.start(orderId);
            updateState(orderId, OrderState.COMPLETED);
        } catch (Exception e) {
            log.error("Fulfilment failed for order {}, refunding payment and releasing inventory", orderId, e);
            compensatePayment(orderId);
            compensateInventory(orderId);
            updateState(orderId, OrderState.CANCELLED);
            return OrderState.CANCELLED;
        }

        return OrderState.COMPLETED;
    }

    // ---- Compensating transactions ----

    private void compensateInventory(UUID orderId) {
        try {
            inventoryClient.release(orderId);
        } catch (Exception e) {
            log.error("Failed to release inventory for order {} during compensation", orderId, e);
            // In production: push to a dead-letter/retry queue rather than swallow
        }
    }

    private void compensatePayment(UUID orderId) {
        try {
            paymentClient.refund(orderId);
        } catch (Exception e) {
            log.error("Failed to refund payment for order {} during compensation", orderId, e);
            // In production: push to a dead-letter/retry queue rather than swallow
        }
    }

    // ---- State persistence ----

    private void updateState(UUID orderId, OrderState state) {
        orderRepository.updateStatus(orderId, state.name());
        log.info("Order {} -> {}", orderId, state);
    }

    // ---- Minimal client/repo contracts referenced above ----

    public interface InventoryClient {
        void reserve(UUID orderId);
        void release(UUID orderId);
    }

    public interface PaymentClient {
        void charge(UUID orderId);
        void refund(UUID orderId);
    }

    public interface FulfilmentClient {
        void start(UUID orderId);
    }

    public interface OrderRepository {
        void updateStatus(UUID orderId, String status);
    }
}
