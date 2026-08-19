package com.ecommerce.notification.service;

import com.ecommerce.notification.dispatcher.EmailDispatcher;
import com.ecommerce.notification.dispatcher.SmsDispatcher;
import com.ecommerce.notification.events.OrderConfirmedEvent;
import com.ecommerce.notification.events.PaymentCompletedEvent;
import com.ecommerce.notification.events.StockConfirmedEvent;
import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.model.NotificationStatus;
import com.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Kafka consumer — listens to domain events and dispatches
 * email/SMS notifications.
 *
 * Topics:
 *   payment.completed.v1  → PAYMENT_RECEIPT (email)
 *   inventory.stock-confirmed.v1 → ORDER_CONFIRMED (email + SMS)
 *
 * Idempotency: each event is checked against notification_log.event_id
 * before dispatching — Kafka at-least-once delivery is handled safely.
 *
 * DLQ: after 3 retries (see KafkaNotificationConfig), failed messages
 * go to {topic}.DLQ for manual review.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailDispatcher          emailDispatcher;
    private final SmsDispatcher            smsDispatcher;
    private final NotificationLogRepository logRepository;
    private String toEmail  = "ponaganti.raju@gmail.com";
    private String tomobileNum= "+919441495509";

    // ── payment.completed.v1 → PAYMENT_RECEIPT ───────────────────────────────

    @KafkaListener(
            topics                  = "payment.completed.v1",
            groupId                 = "notification-service",
            containerFactory        = "notificationListenerFactory"
    )
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("NotificationConsumer: onPaymentCompleted | orderId={}", event.orderId());

        // Idempotency — skip if already processed
        if (logRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate payment.completed event {} — skipping", event.eventId());
            return;
        }

        String subject = "Payment Confirmed — Order " + event.orderId();
        String body    = String.format(
                "Your payment (Transaction ID: %s) for order %s has been confirmed. " +
                "We are now preparing your order.",
                event.paymentTransactionId(), event.orderId());

        // EMAIL channel
        dispatchEmail(toEmail, subject, body,
                event.eventId(), event.orderId(), "PAYMENT_RECEIPT");
    }

    // ── inventory.stock-confirmed.v1 → ORDER_CONFIRMED ───────────────────────

    @KafkaListener(
            topics                  = "inventory.stock-confirmed.v1",
            groupId                 = "notification-service",
            containerFactory        = "notificationListenerFactory"
    )
    @Transactional
    public void onStockConfirmed(StockConfirmedEvent event) {
        log.info("NotificationConsumer: onStockConfirmed | orderId={}", event.orderId());

        if (logRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate stock.confirmed event {} — skipping", event.eventId());
            return;
        }

        // EMAIL channel
        String subject = "Your order " + event.orderId() + " is confirmed!";
        String body    = String.format(
                "Hi, your order %s has been confirmed and stock is reserved. " +
                "Payment transaction: %s. We will notify you once it ships.",
                event.orderId(), event.paymentTransactionId());

        dispatchEmail("customer@ecommerce.com", subject, body,
                event.eventId(), event.orderId(), "ORDER_CONFIRMED");

        // SMS channel
        String smsMessage = String.format(
                "Order %s confirmed! Payment: %s. We'll notify you when it ships.",
                event.orderId(), event.paymentTransactionId());

        dispatchSms(tomobileNum, smsMessage,
                event.eventId(), event.orderId(), "ORDER_CONFIRMED");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void dispatchEmail(String to, String subject, String body,
                                java.util.UUID eventId, java.util.UUID orderId,
                                String template) {
        NotificationStatus status = NotificationStatus.SIMULATED;
        String errorMsg = null;
        try {
            emailDispatcher.send(to, subject, body);
        } catch (Exception ex) {
            log.error("Email dispatch failed for orderId={}", orderId, ex);
            status   = NotificationStatus.FAILED;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            logRepository.save(NotificationLog.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .template(template)
                    .channel("EMAIL")
                    .recipient(to)
                    .status(status)
                    .errorMessage(errorMsg)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    private void dispatchSms(String to, String message,
                              java.util.UUID eventId, java.util.UUID orderId,
                              String template) {
        NotificationStatus status = NotificationStatus.SIMULATED;
        String errorMsg = null;
        try {
            smsDispatcher.send(to, message);
        } catch (Exception ex) {
            log.error("SMS dispatch failed for orderId={}", orderId, ex);
            status   = NotificationStatus.FAILED;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            logRepository.save(NotificationLog.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .template(template)
                    .channel("SMS")
                    .recipient(to)
                    .status(status)
                    .errorMessage(errorMsg)
                    .createdAt(Instant.now())
                    .build());
        }
    }
}
