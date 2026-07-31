package com.paymentgateway.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Master data for banks supported by the gateway.
 *
 * <p>This replaces the hard-coded {@code BankCode} enum for validation.
 * Validators query this table to ensure a bank code is active and supports
 * the requested payment channel (net banking / UPI).</p>
 */
@Entity
@Table(name = "banks",
        indexes = {
                @Index(name = "idx_banks_bank_code", columnList = "bank_code", unique = true),
                @Index(name = "idx_banks_is_active", columnList = "is_active")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short code used in API requests (e.g. HDFC, SBI). */
    @Column(name = "bank_code", nullable = false, unique = true, length = 20)
    private String bankCode;

    /** Human-readable full name (e.g. "HDFC Bank"). */
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    /** IFSC prefix used to identify this bank in transfers (e.g. "HDFC"). */
    @Column(name = "ifsc_prefix", length = 10)
    private String ifscPrefix;

    /** UPI handle(s) associated with this bank, comma-separated (e.g. "okhdfc,hdfcbank"). */
    @Column(name = "upi_handles", length = 255)
    private String upiHandles;

    /** Whether this bank supports Net Banking payments. */
    @Column(name = "supports_net_banking", nullable = false)
    @Builder.Default
    private boolean supportsNetBanking = true;

    /** Whether this bank supports UPI payments. */
    @Column(name = "supports_upi", nullable = false)
    @Builder.Default
    private boolean supportsUpi = true;

    /** Whether this bank is currently active (can be toggled to disable). */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
