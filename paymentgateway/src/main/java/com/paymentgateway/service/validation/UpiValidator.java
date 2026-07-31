package com.paymentgateway.service.validation;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a UPI payment request.
 *
 * <p>Rules executed (in order):</p>
 * <ol>
 *   <li><b>UPI_ID_PRESENT</b>         – upiId field is non-blank</li>
 *   <li><b>UPI_FORMAT_CHECK</b>        – matches {@code username@bankhandle} regex</li>
 *   <li><b>UPI_BANK_HANDLE_EXISTS</b>  – bank handle resolves to an active UPI-enabled bank
 *                                         in the {@code banks} master table (soft check –
 *                                         unknown handles are warned but not blocked, to allow
 *                                         realistic simulation with arbitrary VPAs)</li>
 *   <li><b>AMOUNT_POSITIVE</b>         – amount &gt; 0</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpiValidator implements PaymentValidator {

    /** Regex: 2–256 alphanumeric/dot/dash/underscore chars, then '@', then 2–64 alpha chars. */
    private static final String UPI_ID_REGEX = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$";

    private final BankRepository bankRepository;

    @Override
    public PaymentMode getSupportedMode() {
        return PaymentMode.UPI;
    }

    @Override
    public List<ValidationResult> validate(InitiatePaymentRequest request, String txnId) {
        List<ValidationResult> results = new ArrayList<>();

        // ── 1. UPI ID present ─────────────────────────────────────────────────
        if (request.getUpiId() == null || request.getUpiId().isBlank()) {
            results.add(fail("UPI_ID_PRESENT", "UPI ID is required."));
            throw new PaymentException("UPI ID is required for UPI payment mode.",
                    "MISSING_UPI_ID", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("UPI_ID_PRESENT", "UPI ID field is present."));

        // ── 2. Format check ───────────────────────────────────────────────────
        if (!request.getUpiId().matches(UPI_ID_REGEX)) {
            results.add(fail("UPI_FORMAT_CHECK", "Invalid UPI ID format: " + request.getUpiId()));
            throw new PaymentException(
                    "Invalid UPI ID format. Expected: username@bankhandle (e.g. rajesh@okaxis).",
                    "INVALID_UPI_ID", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("UPI_FORMAT_CHECK", "UPI ID format is valid: " + request.getUpiId()));

        // ── 3. Bank handle in master data (soft check) ────────────────────────
        String bankHandle = extractHandle(request.getUpiId());
        boolean handleKnown = bankRepository.findBySupportsUpiTrueAndIsActiveTrueOrderByBankName()
                .stream()
                .anyMatch(bank -> bank.getUpiHandles() != null
                        && containsHandle(bank.getUpiHandles(), bankHandle));

        if (!handleKnown) {
            // Soft PASS — handle not in master list but we allow it for simulation flexibility
            results.add(pass("UPI_BANK_HANDLE_EXISTS",
                    "Bank handle '@" + bankHandle
                            + "' not in master list; proceeding (simulation mode)."));
            log.warn("UPI bank handle '{}' not found in active bank master list | txnId={}",
                    bankHandle, txnId);
        } else {
            results.add(pass("UPI_BANK_HANDLE_EXISTS",
                    "Bank handle '@" + bankHandle + "' is registered and active."));
        }

        // ── 4. Amount positive ────────────────────────────────────────────────
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            results.add(fail("AMOUNT_POSITIVE", "Amount must be > 0."));
            throw new PaymentException("Payment amount must be greater than zero.",
                    "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("AMOUNT_POSITIVE", "Amount " + request.getAmount() + " is positive."));

        log.debug("UPI validation passed | {} rules | txnId={}", results.size(), txnId);
        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Extracts the part after '@' from a VPA string. */
    public static String extractHandle(String vpa) {
        int at = vpa.indexOf('@');
        return at >= 0 ? vpa.substring(at + 1).toLowerCase() : vpa.toLowerCase();
    }

    /** Checks if a comma-separated list of handles contains the target (case-insensitive). */
    private boolean containsHandle(String handles, String target) {
        for (String h : handles.split(",")) {
            if (h.trim().equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    private ValidationResult pass(String rule, String message) {
        return ValidationResult.pass(rule, message, PaymentMode.UPI);
    }

    private ValidationResult fail(String rule, String message) {
        return ValidationResult.fail(rule, message, PaymentMode.UPI);
    }
}
