package com.paymentgateway.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores Net Banking-specific details for a NET_BANKING payment transaction.
 *
 * <p>Linked to {@link Transaction} via FK on {@code transaction_id} (composition pattern).</p>
 */
@Entity
@Table(name = "netbanking_payment_details",
        indexes = {
                @Index(name = "idx_nb_details_txn_id",   columnList = "transaction_id", unique = true),
                @Index(name = "idx_nb_details_bank_code", columnList = "bank_code")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetBankingPaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK back to the parent transaction. One net banking record per transaction. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Transaction transaction;

    /** Bank code (e.g. HDFC, SBI). Matches {@code banks.bank_code}. */
    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    /** Full bank name at the time of transaction (denormalized for audit trail). */
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    /**
     * Simulated bank portal authentication reference ID.
     * Represents the token returned by the bank after the customer authenticates
     * on the bank's portal. In a real gateway this would be a signed callback token.
     */
    @Column(name = "mock_auth_ref_id", length = 64)
    private String mockAuthRefId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
