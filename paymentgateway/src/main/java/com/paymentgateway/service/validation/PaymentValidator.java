package com.paymentgateway.service.validation;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.model.enums.PaymentMode;

import java.util.List;

/**
 * Strategy interface for payment request validation.
 *
 * <p>Each implementation handles one payment mode and executes all rules for
 * that mode. Each rule result is recorded in {@code validation_audit} as either
 * PASS or FAIL, giving full traceability of why a payment was accepted or rejected.</p>
 *
 * <p>To add a new payment mode, implement this interface and annotate the class
 * with {@code @Component} — Spring auto-discovers it.</p>
 */
public interface PaymentValidator {

    /** Returns the payment mode this validator handles. */
    PaymentMode getSupportedMode();

    /**
     * Validates the request and returns all audit results (one per rule executed).
     * Throws {@link com.paymentgateway.exception.PaymentException} immediately on the
     * first failing rule so callers get a clear, specific error message.
     *
     * @param request the initiation request to validate
     * @param txnId   temporary audit key (may be a "PRE-xxx" string before real ID is assigned)
     * @return ordered list of per-rule audit results (all PASS when no exception is thrown)
     */
    List<ValidationResult> validate(InitiatePaymentRequest request, String txnId);

    // ── Inner result carrier ──────────────────────────────────────────────────

    /**
     * Immutable record representing the outcome of a single validation rule check.
     *
     * @param ruleName    human-readable rule identifier, e.g. {@code CARD_LUHN_CHECK}
     * @param result      {@code "PASS"} or {@code "FAIL"}
     * @param message     descriptive outcome message
     * @param paymentMode payment mode this rule belongs to (e.g. {@code "CARD"})
     */
    record ValidationResult(String ruleName, String result, String message, String paymentMode) {

        /** Creates a PASS result using a {@link PaymentMode} enum value. */
        public static ValidationResult pass(String ruleName, String message, PaymentMode mode) {
            return new ValidationResult(ruleName, "PASS", message, mode.name());
        }

        /** Creates a FAIL result using a {@link PaymentMode} enum value. */
        public static ValidationResult fail(String ruleName, String message, PaymentMode mode) {
            return new ValidationResult(ruleName, "FAIL", message, mode.name());
        }

        /** Creates a FAIL result using a plain mode string (convenience overload). */
        public static ValidationResult fail(String ruleName, String message, String paymentModeStr) {
            return new ValidationResult(ruleName, "FAIL", message, paymentModeStr);
        }
    }
}
