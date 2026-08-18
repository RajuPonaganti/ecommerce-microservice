package com.paymentgateway.service.provider;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.model.entity.CardPaymentDetails;
import com.paymentgateway.model.entity.Transaction;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import com.paymentgateway.service.validation.CardValidator;
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
 * Handles CARD payment processing (Visa, Mastercard, RuPay, Amex).
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li><b>Validate</b> – delegates to {@link CardValidator} (9 rules including Luhn).
 *       All results are written to {@code validation_audit}. CVV is checked here and
 *       <em>never stored</em> — discarded immediately after format validation.</li>
 *   <li><b>Initiate</b> – extracts BIN (first 6 digits), last-4, and derives card network.
 *       Returns transaction shell + {@link CardPaymentDetails} (unsaved, cascade-saved by service).</li>
 *   <li><b>Verify</b>   – simulates 3DS/OTP bank response:
 *       10 % wrong-OTP | 15 % insufficient-balance | 75 % success.</li>
 * </ol>
 *
 * <h3>BIN-based network detection</h3>
 * <ul>
 *   <li>4xxx          → VISA</li>
 *   <li>34xx / 37xx   → Amex</li>
 *   <li>60xx / 6069 / 6521 / 6522 → RuPay</li>
 *   <li>51–55 / 2221–2720 → Mastercard</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CardPaymentProvider implements PaymentProvider {

    private final CardValidator          cardValidator;
    private final ValidationAuditService auditService;

    private static final Random RANDOM        = new Random();
    private static final int    MIN_DELAY_MS  = 500;
    private static final int    MAX_DELAY_MS  = 2000;

    @Override
    public PaymentMode getSupportedMode() {
        return PaymentMode.CARD;
    }

    // ── initiate ─────────────────────────────────────────────────────────────

    @Override
    public InitiateResult initiate(InitiatePaymentRequest request) {
        // 1. Generate a temporary audit key (before the real transaction ID exists)
        String tempRef = "PRE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 2. Run all validation rules; persist audit rows under the temp key
        List<PaymentValidator.ValidationResult> results = cardValidator.validate(request, tempRef);
        auditService.saveAll(results, tempRef);

        simulateDelay();

        // 3. Extract card metadata — CVV is discarded after validation above
        String sanitized  = request.getCardNumber().replaceAll("[\\s-]", "");
        String bin        = sanitized.substring(0, Math.min(6, sanitized.length()));
        String lastFour   = sanitized.substring(sanitized.length() - 4);
        String masked     = "**** **** **** " + lastFour;
        String network    = detectNetwork(sanitized);
        String cardType   = RANDOM.nextDouble() < 0.6 ? "CREDIT" : "DEBIT"; // prod: BIN DB lookup

        // 4. Parse expiry
        String[] ep = request.getCardExpiry().split("/");
        int expiryMonth = Integer.parseInt(ep[0]);
        int expiryYear  = Integer.parseInt(ep[1]) + 2000;

        String transactionId = generateTransactionId();
        log.debug("Card payment initiated | txnId={} | masked={} | network={} | type={}",
                transactionId, masked, network, cardType);

        // 5. Build transaction shell
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMode(PaymentMode.CARD)
                .status(PaymentStatus.PENDING)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .description(request.getDescription())
                .gatewayReferenceId(generateGatewayRef())
                .build();

        // 6. Build card details — no CVV field exists in this entity by design
        CardPaymentDetails details = CardPaymentDetails.builder()
                .transaction(transaction)
                .cardNumberMasked(masked)
                .cardBin(bin)
                .cardNetwork(network)
                .cardType(cardType)
                .expiryMonth((short) expiryMonth)
                .expiryYear((short) expiryYear)
                .cardHolderName(request.getCardHolderName().toUpperCase().trim())
                .build();

        transaction.setCardDetails(details);
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

        double roll = RANDOM.nextDouble();
        if (roll < 0.10) {
            log.warn("Card OTP failed | txnId={}", transaction.getTransactionId());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Wrong OTP entered. Payment declined.");
        } else if (roll < 0.25) {
            log.warn("Card insufficient balance | txnId={}", transaction.getTransactionId());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Insufficient balance in linked account.");
        } else {
            log.info("Card payment success | txnId={}", transaction.getTransactionId());
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
        }
        return transaction;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String detectNetwork(String card) {
        if (card.startsWith("4"))                                          return "VISA";
        if (card.startsWith("34") || card.startsWith("37"))               return "Amex";
        if (card.startsWith("6069") || card.startsWith("6521")
                || card.startsWith("6522") || card.startsWith("60"))      return "RuPay";
        if (card.length() >= 2) {
            int p2 = Integer.parseInt(card.substring(0, 2));
            if (p2 >= 51 && p2 <= 55)                                     return "Mastercard";
        }
        if (card.length() >= 4) {
            int p4 = Integer.parseInt(card.substring(0, 4));
            if (p4 >= 2221 && p4 <= 2720)                                 return "Mastercard";
        }
        return "Unknown";
    }

    private void simulateDelay() {
        try {
            int d = MIN_DELAY_MS + RANDOM.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
            log.debug("Card processing delay: {}ms", d);
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateTransactionId() {
        return "TXN-CARD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String generateGatewayRef() {
        return "GW-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
