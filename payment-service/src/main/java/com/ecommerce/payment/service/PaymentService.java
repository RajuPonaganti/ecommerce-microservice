package com.ecommerce.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.commons.EventName;
import com.ecommerce.commons.GroupIdName;
import com.ecommerce.commons.InventoryReleaseRequestedEvent;
import com.ecommerce.commons.InventoryReservedEvent;
import com.ecommerce.commons.PaymentCompletedEvent;
import com.ecommerce.commons.PaymentFailedEvent;
import com.ecommerce.payment.client.PaymentGatewayClient;
import com.ecommerce.payment.dtos.GatewayApiResponse;
import com.ecommerce.payment.dtos.GatewayInitiateRequest;
import com.ecommerce.payment.exception.PaymentGatewayUnavailableException;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentGatewayInvoker paymentGatewayInvoker;
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
    *     via PaymentGatewayInvoker, which wraps the call with CircuitBreaker + Retry
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
           GatewayApiResponse gatewayResponse = initiatedPayment(event);

           String transactionId = gatewayResponse.getData() != null
                   ? gatewayResponse.getData().getTransactionId()
                   : null;
           String gatewayStatus = gatewayResponse.getData() != null
                   ? gatewayResponse.getData().getStatus()
                   : "FAILED";

           log.info("Gateway response | orderId={} | txnId={} | status={}",
                   event.orderId(), transactionId, gatewayStatus);

           if ("SUCCESS".equalsIgnoreCase(gatewayStatus)) {
               paymentRepository.save(Payment.builder()
                       .orderId(event.orderId())
                       .transactionId(transactionId)
                       .status(PaymentStatus.SUCCESS)
                       .createdAt(Instant.now())
                       .build());

               kafkaTemplate.send(
                       EventName.PAYMENT_COMPLETED_EVENT_V1,
                       event.orderId().toString(),
                       new PaymentCompletedEvent(UUID.randomUUID(),
                               event.orderId(), Instant.now(),
                               transactionId
                       ));

               log.info("Payment SUCCESS | orderId={} | txnId={}", event.orderId(), transactionId);

           } else {
               String reason = gatewayResponse.getData() != null
                       ? gatewayResponse.getData().getMessage()
                       : "Payment gateway declined";
               handleFailure(event.orderId().toString(), reason);
           }

       } catch (PaymentGatewayUnavailableException ex) {
           // Circuit open / retries exhausted — treat as a transient
           // failure the same way as any other gateway error for now.
           // (If you later want "hold and re-attempt" behavior instead
           // of immediately cancelling the order, branch here.)
           log.error("Payment gateway unavailable for orderId={}", event.orderId(), ex);
           handleFailure(event.orderId().toString(),
                   "Gateway unavailable: " + ex.getMessage());

       } catch (Exception ex) {
           log.error("Payment gateway call failed for orderId={}", event.orderId(), ex);
           handleFailure(event.orderId().toString(),
                   "Gateway error: " + ex.getMessage());
       }
   }

	private GatewayApiResponse initiatedPayment(InventoryReservedEvent event) {
		GatewayInitiateRequest gatewayRequest = GatewayInitiateRequest.builder()
		        .orderId(event.orderId().toString())
		        .merchantId(merchantId)
		        .amount(BigDecimal.valueOf(1))   // placeholder — amount from order
		        .currency("INR")
		        .paymentMode("UPI")
		        .upiId("payment-service@upi")
		        .customerName("Internal Service")
		        .customerEmail("payment-service@ecommerce.com")
		        .customerPhone("9999999999")
		        .description("Payment for order " + event.orderId())
		        .build();

		// Delegate to the CircuitBreaker/Retry-wrapped bean.
		// orderId is passed as the idempotency key (see Feign client).
		return paymentGatewayInvoker.initiatePayment(gatewayApiKey, gatewayApiSecret, gatewayRequest);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private void handleFailure(String orderIdStr, String reason) {
		java.util.UUID orderId = java.util.UUID.fromString(orderIdStr);

		paymentRepository.save(Payment.builder().orderId(orderId).status(PaymentStatus.FAILED).failureReason(reason)
				.createdAt(Instant.now()).build());

		kafkaTemplate.send(EventName.PAYMENT_FAILED_EVENT_V1, orderIdStr, new PaymentFailedEvent(UUID.randomUUID(), orderId, Instant.now(),reason));

		kafkaTemplate.send(EventName.INVENTORY_RELEASE_REQUESTED_V1, orderIdStr,
				new InventoryReleaseRequestedEvent(orderId, reason));

		log.warn("Payment FAILED | orderId={} | reason={}", orderId, reason);
	}
}
