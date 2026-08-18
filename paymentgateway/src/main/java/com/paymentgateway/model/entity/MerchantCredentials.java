package com.paymentgateway.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores API credentials for each merchant — modelled after Razorpay's
 * key_id / key_secret system.
 *
 * Security design:
 *  - apiKey    : public identifier (like Razorpay's rzp_live_XXXXX)
 *  - apiSecret : BCrypt-hashed — raw secret is shown ONCE at creation and never stored plain
 *  - connectTimeoutMs / readTimeoutMs : per-merchant timeout overrides
 */
@Entity
@Table(name = "merchant_credentials",
        indexes = {
                @Index(name = "idx_merchant_api_key", columnList = "api_key", unique = true),
                @Index(name = "idx_merchant_id",      columnList = "merchant_id", unique = true)
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique merchant identifier (e.g. "MERCH_001"). */
    @Column(name = "merchant_id", nullable = false, unique = true, length = 64)
    private String merchantId;

    /** Human-readable merchant name. */
    @Column(name = "merchant_name", nullable = false, length = 255)
    private String merchantName;

    /**
     * Public API key — sent in every request header as X-Api-Key.
     * Format: pgw_test_XXXXXXXX (test) or pgw_live_XXXXXXXX (live)
     */
    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    /**
     * BCrypt-hashed API secret — NEVER stored in plain text.
     * Raw secret is shown once at merchant onboarding.
     */
    @Column(name = "api_secret_hash", nullable = false, length = 255)
    private String apiSecretHash;

    /** Max milliseconds to wait for a connection (default 5000). */
    @Column(name = "connect_timeout_ms", nullable = false)
    @Builder.Default
    private int connectTimeoutMs = 5000;

    /** Max milliseconds to wait for a response (default 30000). */
    @Column(name = "read_timeout_ms", nullable = false)
    @Builder.Default
    private int readTimeoutMs = 30000;

    /** Whether this merchant is active. Inactive merchants are rejected. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Environment: TEST or LIVE */
    @Column(name = "environment", nullable = false, length = 10)
    @Builder.Default
    private String environment = "TEST";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
