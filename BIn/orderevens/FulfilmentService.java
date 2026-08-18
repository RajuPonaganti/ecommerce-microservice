package com.platform.fulfilment.service;

import com.platform.order.saga.event.OrderSagaEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Fulfilment Service — final step of the saga. On failure it must trigger
 * BOTH compensations for everything before it: refund the payment and
 * release the inventory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfilmentService {

    private static final String TOPIC_STARTED = "fulfilment.started";
    private static final String TOPIC_FAILED = "fulfilment.failed";
    private static final String TOPIC_REFUND_REQUEST = "payment.refund-requested";
    private static final String TOPIC_RELEASE_REQUEST = "inventory.release-requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FulfilmentClient fulfilmentClient;

    @KafkaListener(topics = "payment.confirmed", groupId = "fulfilment-service")
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        try {
            fulfilmentClient.start(event.orderId());
            kafkaTemplate.send(TOPIC_STARTED, event.orderId().toString(),
                    new FulfilmentStartedEvent(event.orderId()));
            log.info("Fulfilment started for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Fulfilment failed for order {}, requesting refund + inventory release", event.orderId(), e);
            kafkaTemplate.send(TOPIC_FAILED, event.orderId().toString(),
                    new FulfilmentFailedEvent(event.orderId(), e.getMessage()));
            kafkaTemplate.send(TOPIC_REFUND_REQUEST, event.orderId().toString(),
                    new PaymentRefundRequestedEvent(event.orderId()));
            kafkaTemplate.send(TOPIC_RELEASE_REQUEST, event.orderId().toString(),
                    new InventoryReleaseRequestedEvent(event.orderId()));
        }
    }

    public interface FulfilmentClient {
        void start(java.util.UUID orderId);
    }
}
