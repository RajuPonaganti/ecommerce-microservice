package com.paymentgateway.service;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.RefundRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.dto.response.*;
import com.paymentgateway.exception.PaymentException;
import com.paymentgateway.exception.TransactionNotFoundException;
import com.paymentgateway.model.entity.*;
import com.paymentgateway.model.enums.PaymentMode;
import com.paymentgateway.model.enums.PaymentStatus;
import com.paymentgateway.repository.*;
import com.paymentgateway.service.provider.PaymentProvider;
import com.paymentgateway.service.provider.PaymentProvider.InitiateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link PaymentService}.
 *
 * <h3>Orchestration flow – initiate</h3>
 * <ol>
 *   <li>Duplicate order-ID guard.</li>
 *   <li>Route to the correct {@link PaymentProvider} by {@link PaymentMode}.</li>
 *   <li>Provider runs its validator (all rules → written to {@code validation_audit}
 *       under a temp key), then builds {@link Transaction} + mode-specific detail entity.</li>
 *   <li>Persist in a single transaction — cascade saves the detail entity.</li>
 *   <li>Re-key audit records from the temp key to the real transaction ID.</li>
 *   <li>Return unified {@link PaymentResponse} with nested detail object.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository     transactionRepository;
    private final RefundRepository          refundRepository;
    private final ValidationAuditRepository auditRepository;
    private final List<PaymentProvider>     paymentProviders;

    // Lazily built provider map: PaymentMode → PaymentProvider
    private volatile Map<PaymentMode, PaymentProvider> providerMap;

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        log.info("Initiating payment | orderId={} | mode={} | amount={} {}",
                request.getOrderId(), request.getPaymentMode(),
                request.getAmount(), request.getCurrency());

        // Guard: duplicate order ID
        if (transactionRepository.existsByOrderId(request.getOrderId())) {
            throw new PaymentException(
                    "A transaction already exists for orderId: " + request.getOrderId(),
                    "DUPLICATE_ORDER_ID", HttpStatus.CONFLICT);
        }

        // Provider: validate + build unsaved Transaction + detail entity + return audit temp-ref
        PaymentProvider provider = resolveProvider(request.getPaymentMode());
        InitiateResult  result   = provider.initiate(request);

        // Persist (cascade saves card/upi/netbanking details automatically)
        Transaction transaction = transactionRepository.save(result.transaction());

        // Re-key any audit rows written under the temp key to the real transaction ID
        reattachAuditRecords(result.auditTempRef(), transaction.getTransactionId());

        log.info("Payment initiated | txnId={} | status={}",
                transaction.getTransactionId(), transaction.getStatus());

        return toPaymentResponse(transaction, buildInitiationMessage(transaction));
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request) {
        log.info("Verifying payment | txnId={}", request.getTransactionId());

        Transaction transaction = findTransaction(request.getTransactionId());
        transaction = resolveProvider(transaction.getPaymentMode())
                .verify(transaction, request);
        transaction = transactionRepository.save(transaction);

        log.info("Payment verified | txnId={} | status={}",
                transaction.getTransactionId(), transaction.getStatus());

        return toPaymentResponse(transaction, buildVerificationMessage(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse paymentStatus(String transactionId) {
        log.debug("Fetching payment status | txnId={}", transactionId);
        return toPaymentResponse(findTransaction(transactionId),
                "Transaction status retrieved successfully.");
    }

    @Override
    @Transactional
    public RefundResponse refund(RefundRequest request) {
        log.info("Processing refund | txnId={} | amount={}",
                request.getTransactionId(), request.getAmount());

        Transaction transaction = findTransaction(request.getTransactionId());

        if (transaction.getStatus() != PaymentStatus.SUCCESS
                && transaction.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentException(
                    "Refunds are only allowed on successful transactions. Current status: "
                            + transaction.getStatus(),
                    "INVALID_TRANSACTION_STATE_FOR_REFUND", HttpStatus.BAD_REQUEST);
        }

        BigDecimal alreadyRefunded = refundRepository.getTotalRefundedAmount(transaction.getTransactionId());
        BigDecimal remaining       = transaction.getAmount().subtract(alreadyRefunded);

        if (request.getAmount().compareTo(remaining) > 0) {
            throw new PaymentException(
                    String.format("Refund amount %.2f exceeds remaining refundable amount of %.2f.",
                            request.getAmount(), remaining),
                    "REFUND_AMOUNT_EXCEEDED", HttpStatus.BAD_REQUEST);
        }

        Refund refund = Refund.builder()
                .refundId(generateRefundId())
                .transaction(transaction)
                .amount(request.getAmount())
                .status("SUCCESS")
                .reason(request.getReason())
                .processedAt(LocalDateTime.now())
                .build();
        refund = refundRepository.save(refund);

        BigDecimal newTotal = alreadyRefunded.add(request.getAmount());
        transaction.setStatus(newTotal.compareTo(transaction.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        transactionRepository.save(transaction);

        log.info("Refund processed | refundId={} | txnId={} | amount={}",
                refund.getRefundId(), transaction.getTransactionId(), refund.getAmount());

        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .transactionId(transaction.getTransactionId())
                .refundAmount(refund.getAmount())
                .originalAmount(transaction.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .initiatedAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .estimatedCreditDays("5-7 business days")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(t -> toPaymentResponse(t, null))
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private PaymentProvider resolveProvider(PaymentMode mode) {
        if (providerMap == null) {
            synchronized (this) {
                if (providerMap == null) {
                    providerMap = paymentProviders.stream()
                            .collect(Collectors.toMap(
                                    PaymentProvider::getSupportedMode, Function.identity()));
                }
            }
        }
        PaymentProvider p = providerMap.get(mode);
        if (p == null) {
            throw new PaymentException(
                    "No payment provider registered for mode: " + mode,
                    "UNSUPPORTED_PAYMENT_MODE", HttpStatus.BAD_REQUEST);
        }
        return p;
    }

    private Transaction findTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    /**
     * Updates validation_audit rows written under the provider's temporary key
     * to reference the real, persisted transaction ID.
     */
    private void reattachAuditRecords(String tempRef, String realTransactionId) {
        List<ValidationAudit> tempRows = auditRepository.findByTransactionIdOrderByCreatedAt(tempRef);
        if (!tempRows.isEmpty()) {
            tempRows.forEach(a -> a.setTransactionId(realTransactionId));
            auditRepository.saveAll(tempRows);
            log.debug("Re-keyed {} audit record(s) from {} → {}",
                    tempRows.size(), tempRef, realTransactionId);
        }
    }

    // ── Response mapping ──────────────────────────────────────────────────────

    PaymentResponse toPaymentResponse(Transaction t, String message) {
        PaymentResponse.PaymentResponseBuilder b = PaymentResponse.builder()
                .transactionId(t.getTransactionId())
                .orderId(t.getOrderId())
                .merchantId(t.getMerchantId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .paymentMode(t.getPaymentMode())
                .status(t.getStatus())
                .gatewayReferenceId(t.getGatewayReferenceId())
                .failureReason(t.getFailureReason())
                .customerName(t.getCustomerName())
                .customerEmail(t.getCustomerEmail())
                .customerPhone(t.getCustomerPhone())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .message(message);

        // Mode-specific detail object — exactly one is non-null
        if (t.getCardDetails() != null) {
            CardPaymentDetails c = t.getCardDetails();
            b.cardDetails(CardDetailsResponse.builder()
                    .cardNumberMasked(c.getCardNumberMasked())
                    .cardBin(c.getCardBin())
                    .cardNetwork(c.getCardNetwork())
                    .cardType(c.getCardType())
                    .expiryMonth(c.getExpiryMonth())
                    .expiryYear(c.getExpiryYear())
                    .cardHolderName(c.getCardHolderName())
                    .build());
        }

        if (t.getUpiDetails() != null) {
            UpiPaymentDetails u = t.getUpiDetails();
            b.upiDetails(UpiDetailsResponse.builder()
                    .vpa(u.getVpa())
                    .bankHandle(u.getBankHandle())
                    .upiTxnRefId(u.getUpiTxnRefId())
                    .build());
        }

        if (t.getNetBankingDetails() != null) {
            NetBankingPaymentDetails nb = t.getNetBankingDetails();
            b.netBankingDetails(NetBankingDetailsResponse.builder()
                    .bankCode(nb.getBankCode())
                    .bankName(nb.getBankName())
                    .mockAuthRefId(nb.getMockAuthRefId())
                    .build());
        }

        return b.build();
    }

    private String buildInitiationMessage(Transaction t) {
        return switch (t.getStatus()) {
            case PENDING -> switch (t.getPaymentMode()) {
                case CARD        -> "OTP has been sent to your registered mobile number. Please verify to complete the payment.";
                case NET_BANKING -> {
                    String bank = t.getNetBankingDetails() != null
                            ? t.getNetBankingDetails().getBankName() : "bank portal";
                    yield "Redirecting to " + bank + ". Please authenticate to complete the payment.";
                }
                default -> "Payment is pending.";
            };
            case SUCCESS -> "Payment processed successfully.";
            case FAILED  -> "Payment failed: " + t.getFailureReason();
            default      -> "Payment initiated.";
        };
    }

    private String buildVerificationMessage(Transaction t) {
        return switch (t.getStatus()) {
            case SUCCESS -> "Payment verified and completed successfully.";
            case FAILED  -> "Payment verification failed: " + t.getFailureReason();
            default      -> "Payment status: " + t.getStatus();
        };
    }

    private String generateRefundId() {
        return "RFD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
