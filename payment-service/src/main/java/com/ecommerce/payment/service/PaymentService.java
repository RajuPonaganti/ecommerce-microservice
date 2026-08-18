package com.ecommerce.payment.service;

import com.ecommerce.payment.client.PaymentGatewayClient;
import com.ecommerce.payment.dtos.GatewayApiResponse;
import com.ecommerce.payment.dtos.GatewayInitiateRequest;
import com.ecommerce.payment.events.*;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository      paymentRepository;
    private final PaymentGatewayClient   gatewayClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Injected from bootstrap.yml / Config Server
    @Value("${payment.gateway.api-key}")
    private String gatewayApiKey;

    @Value("${payment.gateway.api-secret}")
    private String gatewayApiSecret;

    @Value("${payment.gateway.merchant-id:MERCH_ECOMM_001}")
    private String merchantId;

    /**
     * Listens for inventory.stock-reserved.v1.
     * When inventory is reserved for an order, this triggers the payment charge.
     *
     * Flow:
     *  1. Idempotency check — skip if already processed
     *  2. Call payment-gateway POST /api/v1/payments/initiate (UPI mode — resolves inline)
     *  3. If SUCCESS → publish payment.completed.v1
     *  4. If FAILED  → publish payment.failed.v1
     *     (Order Service cancels order → Inventory Service releases reservation)
     */
    @KafkaListener(
            topics   = EventName.INVENTORY_STOCK_RESERVED_EVENT_V1,
            groupId  = GroupIdName.PAYMENT_SERVICE
    )
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("PaymentService: onInventoryReserved | orderId={} | reservationId={}",
                event.orderId(), event.reservationId());

        // 1. Idempotency — skip if this order was already charged
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.warn("Duplicate inventory.reserved event for orderId={} — skipping",
                    event.orderId());
            return;
        }

        try {
            // 2. Build request to payment-gateway
            // Using UPI mode for internal service-to-service — resolves immediately
            GatewayInitiateRequest gatewayRequest = GatewayInitiateRequest.builder()
                    .orderId(event.orderId().toString())
                    .merchantId(merchantId)
                    .amount(BigDecimal.valueOf(1))   // placeholder — amount from order
                    .currency("INR")
                    .paymentMode("UPI")
                    .upiId("payment-service@upi")    // internal service UPI handle
                    .customerName("Internal Service")
                    .customerEmail("payment-service@ecommerce.com")
                    .customerPhone("9999999999")
                    .description("Payment for order " + event.orderId())
                    .build();

            // 3. Call payment-gateway — credentials auto-injected by PaymentGatewayFeignConfig
            GatewayApiResponse gatewayResponse =
                    gatewayClient.initiatePayment(gatewayApiKey, gatewayApiSecret, gatewayRequest);

            String transactionId = gatewayResponse.getData() != null
                    ? gatewayResponse.getData().getTransactionId()
                    : null;
            String gatewayStatus = gatewayResponse.getData() != null
                    ? gatewayResponse.getData().getStatus()
                    : "FAILED";

            log.info("Gateway response | orderId={} | txnId={} | status={}",
                    event.orderId(), transactionId, gatewayStatus);

            if ("SUCCESS".equalsIgnoreCase(gatewayStatus)) {
                // 4a. Persist SUCCESS
                paymentRepository.save(Payment.builder()
                        .orderId(event.orderId())
                        .transactionId(transactionId)
                        .status(PaymentStatus.SUCCESS)
                        .createdAt(Instant.now())
                        .build());

                // 4b. Publish payment.completed.v1
                //     → Order Service listens and marks order PAYMENT_CONFIRMED
                kafkaTemplate.send(
                        EventName.PAYMENT_COMPLETED_EVENT_V1,
                        event.orderId().toString(),
                        new PaymentCompletedEvent(
                                event.orderId(),
                                transactionId
                        ));

                log.info("Payment SUCCESS | orderId={} | txnId={}", event.orderId(), transactionId);

            } else {
                // 5. Gateway returned FAILED
                String reason = gatewayResponse.getData() != null
                        ? gatewayResponse.getData().getMessage()
                        : "Payment gateway declined";
                handleFailure(event.orderId().toString(), reason);
            }

        } catch (Exception ex) {
            // 6. Network error / gateway down / unexpected exception
            log.error("Payment gateway call failed for orderId={}", event.orderId(), ex);
            handleFailure(event.orderId().toString(),
                    "Gateway error: " + ex.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void handleFailure(String orderIdStr, String reason) {
        java.util.UUID orderId = java.util.UUID.fromString(orderIdStr);

        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .status(PaymentStatus.FAILED)
                .failureReason(reason)
                .createdAt(Instant.now())
                .build());

        // Publish payment.failed.v1
        // → Order Service marks order CANCELLED
        // → Inventory Service releases reservation (via OrderCancelled event)
        kafkaTemplate.send(
                EventName.PAYMENT_FAILED_EVENT_V1,
                orderIdStr,
                new PaymentFailedEvent(orderId, reason));

        log.warn("Payment FAILED | orderId={} | reason={}", orderId, reason);
    }
}
