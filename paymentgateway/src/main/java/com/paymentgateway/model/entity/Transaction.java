package com.paymentgateway.model.entity;

import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core payment transaction entity — stores only common fields.
 *
 * <p>Mode-specific details are held in separate composition tables:</p>
 * <ul>
 *   <li>{@link CardPaymentDetails}        — {@code card_payment_details}</li>
 *   <li>{@link UpiPaymentDetails}         — {@code upi_payment_details}</li>
 *   <li>{@link NetBankingPaymentDetails}  — {@code netbanking_payment_details}</li>
 * </ul>
 *
 * <p>Each detail entity has a FK back to {@code transactions.transaction_id}.
 * This composition design keeps this table clean and each mode independently evolvable.</p>
 */
@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_order_id",       columnList = "order_id"),
                @Index(name = "idx_transactions_merchant_id",    columnList = "merchant_id"),
                @Index(name = "idx_transactions_status",         columnList = "status"),
                @Index(name = "idx_transactions_payment_mode",   columnList = "payment_mode"),
                @Index(name = "idx_transactions_customer_email", columnList = "customer_email"),
                @Index(name = "idx_transactions_created_at",     columnList = "created_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 64)
    private String transactionId;

    @Column(name = "order_id", nullable = false, length = 128)
    private String orderId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "gateway_reference_id", length = 64)
    private String gatewayReferenceId;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ── Composition relationships (lazy – fetched only when needed) ───────────

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CardPaymentDetails cardDetails;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UpiPaymentDetails upiDetails;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private NetBankingPaymentDetails netBankingDetails;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Refund> refunds;
}
