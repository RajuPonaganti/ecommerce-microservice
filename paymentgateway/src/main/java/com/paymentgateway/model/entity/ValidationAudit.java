package com.paymentgateway.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit log for each validation rule executed against a payment request.
 *
 * <p>Every call to a {@code PaymentValidator} appends one or more records here,
 * one per rule checked. This enables full traceability of why a payment was
 * accepted or rejected — useful for testing, compliance, and debugging.</p>
 *
 * <p>Example rules: AMOUNT_POSITIVE, CARD_LUHN_CHECK, CARD_EXPIRY_CHECK,
 * UPI_FORMAT_CHECK, UPI_BANK_HANDLE_EXISTS, BANK_CODE_EXISTS, BANK_ACTIVE,
 * BANK_SUPPORTS_NETBANKING.</p>
 */
@Entity
@Table(name = "validation_audit",
        indexes = {
                @Index(name = "idx_val_audit_txn_id",  columnList = "transaction_id"),
                @Index(name = "idx_val_audit_rule",    columnList = "rule_name"),
                @Index(name = "idx_val_audit_result",  columnList = "result"),
                @Index(name = "idx_val_audit_created", columnList = "created_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The transaction ID this validation record belongs to.
     * Nullable on the rare occasion validation fails before a transaction ID is assigned
     * (e.g. amount validation fires before any provider creates the transaction).
     */
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    /** Human-readable rule identifier (e.g. "CARD_LUHN_CHECK"). */
    @Column(name = "rule_name", nullable = false, length = 64)
    private String ruleName;

    /** PASS or FAIL. */
    @Column(name = "result", nullable = false, length = 10)
    private String result;

    /** Descriptive message – success note or the reason for failure. */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /** Payment mode being validated: CARD, NET_BANKING, UPI. */
    @Column(name = "payment_mode", length = 20)
    private String paymentMode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
