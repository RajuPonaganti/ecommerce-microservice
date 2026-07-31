package com.paymentgateway.service.validation;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.entity.Bank;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates a NET_BANKING payment request.
 *
 * <p>Rules executed (in order):</p>
 * <ol>
 *   <li><b>BANK_CODE_PRESENT</b>          – bankCode field is non-null</li>
 *   <li><b>BANK_CODE_EXISTS</b>            – bank code exists in the {@code banks} master table</li>
 *   <li><b>BANK_ACTIVE</b>                 – the bank record has {@code is_active = true}</li>
 *   <li><b>BANK_SUPPORTS_NETBANKING</b>    – the bank record has {@code supports_net_banking = true}</li>
 *   <li><b>AMOUNT_POSITIVE</b>             – amount &gt; 0</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NetBankingValidator implements PaymentValidator {

    private final BankRepository bankRepository;

    @Override
    public PaymentMode getSupportedMode() {
        return PaymentMode.NET_BANKING;
    }

    @Override
    public List<ValidationResult> validate(InitiatePaymentRequest request, String txnId) {
        List<ValidationResult> results = new ArrayList<>();

        // 1. Bank code present
        if (request.getBankCode() == null) {
            results.add(fail("BANK_CODE_PRESENT", "Bank code is required."));
            throw new PaymentException("Bank code is required for NET_BANKING payment mode.",
                    "MISSING_BANK_CODE", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("BANK_CODE_PRESENT", "Bank code field is present: " + request.getBankCode()));

        String code = request.getBankCode().name();

        // 2. Bank exists in master data
        Optional<Bank> bankOpt = bankRepository.findByBankCodeIgnoreCase(code);
        if (bankOpt.isEmpty()) {
            results.add(fail("BANK_CODE_EXISTS", "Bank code not found: " + code));
            throw new PaymentException("Bank code '" + code + "' is not registered in the system.",
                    "UNKNOWN_BANK_CODE", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("BANK_CODE_EXISTS", "Bank code '" + code + "' exists in master data."));

        Bank bank = bankOpt.get();

        // 3. Bank is active
        if (!bank.isActive()) {
            results.add(fail("BANK_ACTIVE", "Bank '" + bank.getBankName() + "' is currently inactive."));
            throw new PaymentException(
                    "Bank '" + bank.getBankName() + "' is currently unavailable. Please try another bank.",
                    "BANK_INACTIVE", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("BANK_ACTIVE", "Bank '" + bank.getBankName() + "' is active."));

        // 4. Bank supports net banking
        if (!bank.isSupportsNetBanking()) {
            results.add(fail("BANK_SUPPORTS_NETBANKING",
                    "Bank '" + bank.getBankName() + "' does not support net banking."));
            throw new PaymentException(
                    "Bank '" + bank.getBankName() + "' does not support Net Banking payments.",
                    "BANK_NO_NETBANKING", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("BANK_SUPPORTS_NETBANKING",
                "Bank '" + bank.getBankName() + "' supports net banking."));

        // 5. Amount positive
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            results.add(fail("AMOUNT_POSITIVE", "Amount must be > 0."));
            throw new PaymentException("Payment amount must be greater than zero.",
                    "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
        }
        results.add(pass("AMOUNT_POSITIVE", "Amount " + request.getAmount() + " is positive."));

        log.debug("Net banking validation passed | bank={} | {} rules | txnId={}", code, results.size(), txnId);
        return results;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    private ValidationResult pass(String rule, String message) {
        return ValidationResult.pass(rule, message, PaymentMode.NET_BANKING);
    }

    private ValidationResult fail(String rule, String message) {
        return ValidationResult.fail(rule, message, PaymentMode.NET_BANKING);
    }
}
