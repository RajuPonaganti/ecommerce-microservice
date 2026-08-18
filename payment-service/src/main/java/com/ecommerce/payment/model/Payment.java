package com.ecommerce.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "order_id", unique = true)
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** Transaction ID returned by the payment-gateway service */
    @Column(name = "transaction_id", length = 128)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** Failure reason — populated when status = FAILED */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;
}
