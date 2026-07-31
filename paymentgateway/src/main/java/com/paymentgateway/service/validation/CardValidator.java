package com.paymentgateway.service.validation;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.enums.PaymentMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a CARD payment request.
 *
 * <p>Rules executed in order:</p>
 * <ol>
 *   <li><b>CARD_NUMBER_PRESENT</b>      – non-blank card number field</li>
 *   <li><b>CARD_NUMBER_FORMAT</b>       – 15–19 digits after stripping spaces/dashes</li>
 *   <li><b>CARD_LUHN_CHECK</b>          – passes Luhn (mod-10) algorithm</li>
 *   <li><b>CARD_EXPIRY_FORMAT</b>       – matches MM/YY pattern</li>
 *   <li><b>CARD_EXPIRY_NOT_PAST</b>     – expiry month/year is today or future</li>
 *   <li><b>CARD_CVV_PRESENT</b>         – non-blank CVV field</li>
 *   <li><b>CARD_CVV_FORMAT</b>          – 3–4 digits (CVV discarded immediately after)</li>
 *   <li><b>CARD_HOLDER_NAME_PRESENT</b> – non-blank card holder name</li>
 *   <li><b>AMOUNT_POSITIVE</b>          – amount &gt; 0</li>
 * </ol>
 *
 * <p><strong>Security:</strong> CVV is validated in rules 6–7 then discarded.
 * It is never passed to any persistence layer.</p>
 */
@Component
@Slf4j
public class CardValidator implements PaymentValidator {

    @Override
    public PaymentMode getSupportedMode() {
        return PaymentMode.CARD;
    }

    @Override
    public List<ValidationResult> validate(InitiatePaymentRequest request, String txnId) {
        List<ValidationResult> results = new ArrayList<>();

        // ── 1. Card number present ────────────────────────────────────────────
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
            results.add(fail("CARD_NUMBER_PRESENT", "Card number is required."));
            throw new PaymentException("Card number is required for CARD payment mode.",
                    "MISSING_CARD_NUMBER", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_NUMBER_PRESENT", "Card number field is present."));

        String sanitized = request.getCardNumber().replaceAll("[\\s-]", "");

        // ── 2. Card number format ─────────────────────────────────────────────
        if (!sanitized.matches("\\d{15,19}")) {
            results.add(fail("CARD_NUMBER_FORMAT", "Card number must be 15–19 digits."));
            throw new PaymentException("Invalid card number format. Must be 15–19 digits.",
                    "INVALID_CARD_NUMBER", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_NUMBER_FORMAT", "Card number format is valid (" + sanitized.length() + " digits)."));

        // ── 3. Luhn algorithm ─────────────────────────────────────────────────
        if (!luhnCheck(sanitized)) {
            results.add(fail("CARD_LUHN_CHECK", "Card number failed Luhn algorithm check."));
            throw new PaymentException("Invalid card number: Luhn check failed.",
                    "CARD_LUHN_FAILED", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_LUHN_CHECK", "Card number passed Luhn algorithm check."));

        // ── 4. Expiry format ──────────────────────────────────────────────────
        if (request.getCardExpiry() == null || !request.getCardExpiry().matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            results.add(fail("CARD_EXPIRY_FORMAT", "Expiry must be in MM/YY format."));
            throw new PaymentException("Invalid card expiry format. Use MM/YY (e.g. 12/26).",
                    "INVALID_CARD_EXPIRY", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_EXPIRY_FORMAT", "Expiry format MM/YY is valid."));

        // ── 5. Expiry not in the past ─────────────────────────────────────────
        String[] parts = request.getCardExpiry().split("/");
        int mm = Integer.parseInt(parts[0]);
        int yy = Integer.parseInt(parts[1]) + 2000;
        YearMonth expiry = YearMonth.of(yy, mm);
        if (expiry.isBefore(YearMonth.now())) {
            results.add(fail("CARD_EXPIRY_NOT_PAST",
                    "Card expired: " + request.getCardExpiry()));
            throw new PaymentException(
                    "Card has expired (expiry: " + request.getCardExpiry() + ").",
                    "CARD_EXPIRED", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_EXPIRY_NOT_PAST", "Card expiry " + request.getCardExpiry() + " is valid."));

        // ── 6. CVV present ────────────────────────────────────────────────────
        if (request.getCvv() == null || request.getCvv().isBlank()) {
            results.add(fail("CARD_CVV_PRESENT", "CVV is required."));
            throw new PaymentException("CVV is required for CARD payment mode.",
                    "MISSING_CVV", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_CVV_PRESENT", "CVV field is present."));

        // ── 7. CVV format — validate then DISCARD (never persisted) ──────────
        if (!request.getCvv().matches("\\d{3,4}")) {
            results.add(fail("CARD_CVV_FORMAT", "CVV must be 3–4 digits."));
            throw new PaymentException("Invalid CVV. Must be 3 or 4 digits.",
                    "INVALID_CVV", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_CVV_FORMAT", "CVV format is valid. CVV will not be stored."));
        // ⚠ CVV is intentionally discarded here — no downstream code receives it.

        // ── 8. Card holder name present ───────────────────────────────────────
        if (request.getCardHolderName() == null || request.getCardHolderName().isBlank()) {
            results.add(fail("CARD_HOLDER_NAME_PRESENT", "Card holder name is required."));
            throw new PaymentException("Card holder name is required.",
                    "MISSING_CARD_HOLDER_NAME", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("CARD_HOLDER_NAME_PRESENT", "Card holder name is present."));

        // ── 9. Amount positive ────────────────────────────────────────────────
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            results.add(fail("AMOUNT_POSITIVE", "Amount must be > 0."));
            throw new PaymentException("Payment amount must be greater than zero.",
                    "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("AMOUNT_POSITIVE", "Amount " + request.getAmount() + " is positive."));

        log.debug("Card validation passed | {} rules | txnId={}", results.size(), txnId);
        return results;
    }

    // ── Luhn (mod-10) algorithm ───────────────────────────────────────────────

    /**
     * Standard Luhn algorithm used by Visa, Mastercard, RuPay, and Amex
     * to detect card number typos.
     */
    static boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    // ── Convenience factory shortcuts ─────────────────────────────────────────

    private ValidationResult pass(String rule, String message) {
        return ValidationResult.pass(rule, message, PaymentMode.CARD);
    }

    private ValidationResult fail(String rule, String message) {
        return ValidationResult.fail(rule, message, PaymentMode.CARD);
    }
}
