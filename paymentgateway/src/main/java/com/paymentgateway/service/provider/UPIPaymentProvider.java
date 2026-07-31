package com.paymentgateway.service.provider;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.model.entity.Transaction;
import com.paymentgateway.model.entity.UpiPaymentDetails;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import com.paymentgateway.service.validation.PaymentValidator;
import com.paymentgateway.service.validation.UpiValidator;
import com.paymentgateway.service.validation.ValidationAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Handles UPI payment processing.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li><b>Validate</b> – delegates to {@link UpiValidator}:
 *       UPI ID present → format regex → bank handle in master data → amount positive.</li>
 *   <li><b>Initiate</b> – UPI is real-time; resolves immediately with no separate verify step.
 *       20 % → FAILED | 80 % → SUCCESS. Persists {@link UpiPaymentDetails} with VPA,
 *       bank handle, and a simulated NPCI reference ID.</li>
 *   <li><b>Verify</b>  – intentional no-op.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UPIPaymentProvider implements PaymentProvider {

    private final UpiValidator           upiValidator;
    private final ValidationAuditService auditService;

    private static final Random   RANDOM        = new Random();
    private static final int      MIN_DELAY_MS  = 300;
    private static final int      MAX_DELAY_MS  = 1200;

    private static final String[] FAILURE_REASONS = {
            "UPI ID not registered with any bank.",
            "Payment declined by the payer's bank.",
            "Payer's UPI daily transaction limit exceeded.",
            "Transaction blocked by bank risk engine."
    };

    @Override
    public PaymentMode getSupportedMode() {
        return PaymentMode.UPI;
    }

    // ── initiate ─────────────────────────────────────────────────────────────

    @Override
    public InitiateResult initiate(InitiatePaymentRequest request) {
        // 1. Validate and persist audit rows under temp key
        String tempRef = "PRE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        List<PaymentValidator.ValidationResult> results = upiValidator.validate(request, tempRef);
        auditService.saveAll(results, tempRef);

        simulateDelay();

        // 2. Resolve outcome immediately (UPI is real-time)
        double roll = RANDOM.nextDouble();
        PaymentStatus status;
        String failureReason = null;
        LocalDateTime completedAt = null;

        if (roll < 0.20) {
            status        = PaymentStatus.FAILED;
            failureReason = FAILURE_REASONS[RANDOM.nextInt(FAILURE_REASONS.length)];
            log.warn("UPI failed | vpa={} | reason={}", request.getUpiId(), failureReason);
        } else {
            status      = PaymentStatus.SUCCESS;
            completedAt = LocalDateTime.now();
            log.info("UPI success | vpa={} | orderId={}", request.getUpiId(), request.getOrderId());
        }

        String transactionId = generateTransactionId();
        String bankHandle    = UpiValidator.extractHandle(request.getUpiId());

        // 3. Build transaction shell
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMode(PaymentMode.UPI)
                .status(status)
                .failureReason(failureReason)
                .completedAt(completedAt)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .description(request.getDescription())
                .gatewayReferenceId(generateGatewayRef())
                .build();

        // 4. Build UPI detail entity
        UpiPaymentDetails details = UpiPaymentDetails.builder()
                .transaction(transaction)
                .vpa(request.getUpiId().toLowerCase().trim())
                .bankHandle(bankHandle)
                .upiTxnRefId("NPCI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .build();

        transaction.setUpiDetails(details);
        return new InitiateResult(transaction, tempRef);
    }

    /** UPI is finalised in {@link #initiate} — this is intentionally a no-op. */
    @Override
    public Transaction verify(Transaction transaction, VerifyPaymentRequest verifyRequest) {
        log.debug("UPI verify no-op | txnId={}", transaction.getTransactionId());
        return transaction;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void simulateDelay() {
        try {
            int d = MIN_DELAY_MS + RANDOM.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
            log.debug("UPI processing delay: {}ms", d);
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateTransactionId() {
        return "TXN-UPI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String generateGatewayRef() {
        return "GW-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
