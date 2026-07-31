package com.paymentgateway.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores card-specific details for a CARD payment transaction.
 *
 * <p><strong>Security notes:</strong></p>
 * <ul>
 *   <li>Only the last 4 digits of the card number are persisted ({@code cardNumberMasked}).</li>
 *   <li>The first 6 digits (BIN) are stored for network/issuer identification.</li>
 *   <li>CVV is <em>never</em> persisted — it is validated in-memory and discarded immediately.</li>
 * </ul>
 *
 * <p>Linked to {@link Transaction} via a FK on {@code transaction_id} (composition pattern).</p>
 */
@Entity
@Table(name = "card_payment_details",
        indexes = {
                @Index(name = "idx_card_details_txn_id", columnList = "transaction_id", unique = true)
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardPaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK back to the parent transaction. One card record per transaction. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Transaction transaction;

    /**
     * Masked card number – last 4 digits only (e.g. "**** **** **** 1234").
     * The full number is never stored.
     */
    @Column(name = "card_number_masked", nullable = false, length = 20)
    private String cardNumberMasked;

    /**
     * BIN (Bank Identification Number) – first 6 digits.
     * Used for issuer / network identification without exposing the full PAN.
     */
    @Column(name = "card_bin", nullable = false, length = 8)
    private String cardBin;

    /**
     * Card network derived from BIN: VISA, Mastercard, RuPay, Amex.
     */
    @Column(name = "card_network", nullable = false, length = 20)
    private String cardNetwork;

    /**
     * Card type: CREDIT or DEBIT.
     * In a real system this is determined via BIN lookup against an issuer database.
     */
    @Column(name = "card_type", nullable = false, length = 10)
    private String cardType;

    /** Expiry month (1–12). */
    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth;

    /** Expiry year (4-digit, e.g. 2026). */
    @Column(name = "expiry_year", nullable = false)
    private int expiryYear;

    /** Name of the card holder as printed on the card. */
    @Column(name = "card_holder_name", nullable = false, length = 255)
    private String cardHolderName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
