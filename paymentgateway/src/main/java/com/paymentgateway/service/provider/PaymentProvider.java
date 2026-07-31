package com.paymentgateway.service.provider;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.model.entity.Transaction;
import com.paymentgateway.model.enums.PaymentMode;

/**
 * Strategy interface for payment mode providers.
 *
 * <p>Each implementation handles one mode (CARD, NET_BANKING, UPI).
 * The service layer routes requests here by {@link PaymentMode}.</p>
 *
 * <p>To add a new payment mode, implement this interface and annotate
 * the class with {@code @Component} — Spring auto-registers it.</p>
 */
public interface PaymentProvider {

    /** Returns the payment mode this provider handles. */
    PaymentMode getSupportedMode();

    /**
     * Initiates a payment. Runs validation, builds the {@link Transaction}
     * + its mode-specific detail entity (not yet persisted), and returns
     * both together with the audit temp-reference so the service can
     * re-key audit records after the transaction is saved.
     *
     * @param request the initiation request
     * @return {@link InitiateResult} carrying the unsaved transaction and the audit temp-ref
     */
    InitiateResult initiate(InitiatePaymentRequest request);

    /**
     * Verifies / confirms a PENDING transaction (OTP for card, bank-callback for net banking).
     * Mutates and returns the same transaction with an updated status.
     * Implementations where initiation already finalises (e.g. UPI) should be a no-op.
     */
    Transaction verify(Transaction transaction, VerifyPaymentRequest verifyRequest);

    // ── Result carrier ────────────────────────────────────────────────────────

    /**
     * Carries the result of {@link #initiate} back to the service layer.
     *
     * @param transaction the built (unsaved) transaction with its detail entity attached
     * @param auditTempRef the temporary reference used to key validation-audit rows;
     *                     the service updates these to the real transaction ID after save
     */
    record InitiateResult(Transaction transaction, String auditTempRef) {}
}
