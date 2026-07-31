package com.paymentgateway.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a refund (full or partial) against a successful transaction.
 */
@Entity
@Table(name = "refunds",
        indexes = {
                @Index(name = "idx_refunds_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_refunds_status",         columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_id", unique = true, nullable = false, length = 64)
    private String refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Transaction transaction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Refund status: SUCCESS, PENDING, FAILED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
