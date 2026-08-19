package com.ecommerce.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log of every notification dispatched.
 * Idempotency key = eventId — prevents duplicate notifications on Kafka replay.
 */
@Entity
@Table(name = "notification_log",
        indexes = @Index(name = "idx_notification_event_id", columnList = "event_id", unique = true))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Kafka event ID — used for idempotency check */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "order_id")
    private UUID orderId;

    /** Template name triggered: ORDER_CONFIRMED, PAYMENT_RECEIPT, etc. */
    @Column(name = "template", nullable = false, length = 50)
    private String template;

    /** EMAIL, SMS, PUSH */
    @Column(name = "channel", nullable = false, length = 10)
    private String channel;

    /** Recipient address (email / phone / device token) */
    @Column(name = "recipient", length = 320)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
