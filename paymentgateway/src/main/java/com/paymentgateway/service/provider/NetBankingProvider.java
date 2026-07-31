package com.paymentgateway.service.provider;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.entity.Bank;
import com.paymentgateway.model.entity.NetBankingPaymentDetails;
import com.paymentgateway.model.entity.Transaction;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import com.paymentgateway.repository.BankRepository;
import com.paymentgateway.service.validation.NetBankingValidator;
import com.paymentgateway.service.validation.PaymentValidator;
import com.paymentgateway.service.validation.ValidationAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Handles NET_BANKING payment processing.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li><b>Validate</b> – delegates to {@link NetBankingValidator}:
 *       bank code present → exists in {@code banks} master table → is active
 *       → supports net banking → amount positive.</li>
 *   <li><b>Initiate</b> – sets status {@code PENDING}, builds
 *       {@link NetBankingPaymentDetails} with bank name from master data.</li>
 *   <li><b>Verify</b>  – simulates bank authentication callback:
 *       5 % timeout | 10 % invalid credentials | 85 % success.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NetBankingProvider implements PaymentProvider {

    private final NetBankingValidator    netBankingValidator;
    private final ValidationAuditService auditService;
    private final BankRepository         bankRepository;

    private static final Random RANDOM       = new Random();
    private static final int    MIN_DELAY_MS = 800;
    private static final int    MAX_DELAY_MS = 2500;

    @Override
    public PaymentMode getSupportedMode() {
        return PaymentMode.NET_BANKING;
    }

    // ── initiate ─────────────────────────────────────────────────────────────

    @Override
    public InitiateResult initiate(InitiatePaymentRequest request) {
        // 1. Validate and persist audit rows under temp key
        String tempRef = "PRE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        List<PaymentValidator.ValidationResult> results = netBankingValidator.validate(request, tempRef);
        auditService.saveAll(results, tempRef);

        simulateDelay();

        // 2. Resolve full bank name from master data
        String bankCode = request.getBankCode().name();
        Bank bank = bankRepository.findByBankCodeIgnoreCase(bankCode)
                .orElseThrow(() -> new PaymentException(
                        "Bank not found: " + bankCode, "UNKNOWN_BANK_CODE", HttpStatus.BAD_REQUEST));

        String transactionId = generateTransactionId();
        log.debug("Net banking initiated | txnId={} | bank={}", transactionId, bank.getBankName());

        // 3. Build transaction shell
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMode(PaymentMode.NET_BANKING)
                .status(PaymentStatus.PENDING)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .description(request.getDescription())
                .gatewayReferenceId(generateGatewayRef())
                .build();

        // 4. Build net banking detail entity (mockAuthRefId filled during verify)
        NetBankingPaymentDetails details = NetBankingPaymentDetails.builder()
                .transaction(transaction)
                .bankCode(bank.getBankCode())
                .bankName(bank.getBankName())
                .build();

        transaction.setNetBankingDetails(details);
        return new InitiateResult(transaction, tempRef);
    }

    // ── verify ────────────────────────────────────────────────────────────────

    @Override
    public Transaction verify(Transaction transaction, VerifyPaymentRequest verifyRequest) {
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(
                    "Transaction is not in PENDING state. Current status: " + transaction.getStatus(),
                    "INVALID_TRANSACTION_STATE", HttpStatus.BAD_REQUEST);
        }

        simulateDelay();

        // Record the simulated bank-portal auth reference on the detail entity
        if (transaction.getNetBankingDetails() != null) {
            transaction.getNetBankingDetails().setMockAuthRefId(
                    "BANK-AUTH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        }

        double roll = RANDOM.nextDouble();
        if (roll < 0.05) {
            log.warn("Net banking timeout | txnId={}", transaction.getTransactionId());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Bank server timeout. Please try again.");
        } else if (roll < 0.15) {
            log.warn("Net banking invalid credentials | txnId={}", transaction.getTransactionId());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Invalid net banking credentials. Access denied.");
        } else {
            log.info("Net banking success | txnId={}", transaction.getTransactionId());
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
        }
        return transaction;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void simulateDelay() {
        try {
            int d = MIN_DELAY_MS + RANDOM.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
            log.debug("Net banking processing delay: {}ms", d);
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateTransactionId() {
        return "TXN-NB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String generateGatewayRef() {
        return "GW-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
