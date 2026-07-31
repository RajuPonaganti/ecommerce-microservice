package com.paymentgateway.service;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.request.RefundRequest;
import com.paymentgateway.dto.request.VerifyPaymentRequest;
import com.paymentgateway.dto.response.PaymentResponse;
import com.paymentgateway.dto.response.RefundResponse;

import java.util.List;

/**
 * Core payment service contract.
 *
 * <p>Four primary operations match the payment gateway lifecycle:</p>
 * <ol>
 *   <li>{@link #initiatePayment} – start a new payment transaction</li>
 *   <li>{@link #verifyPayment}   – confirm OTP / bank-callback for PENDING transactions</li>
 *   <li>{@link #paymentStatus}   – query current state of a transaction</li>
 *   <li>{@link #refund}          – initiate a full or partial refund</li>
 * </ol>
 */
public interface PaymentService {

    /**
     * Initiates a new payment transaction.
     *
     * @param request payment request with mode-specific fields
     * @return unified {@link PaymentResponse}; status is PENDING for CARD/NET_BANKING,
     *         SUCCESS or FAILED immediately for UPI
     */
    PaymentResponse initiatePayment(InitiatePaymentRequest request);

    /**
     * Verifies a PENDING payment (OTP for card, bank callback for net banking).
     *
     * @param request verification request containing the transaction ID and OTP / token
     * @return updated {@link PaymentResponse} with final status (SUCCESS or FAILED)
     */
    PaymentResponse verifyPayment(VerifyPaymentRequest request);

    /**
     * Fetches the current status of a transaction.
     *
     * @param transactionId gateway-issued transaction ID
     * @return {@link PaymentResponse} with latest state
     */
    PaymentResponse paymentStatus(String transactionId);

    /**
     * Initiates a full or partial refund against a successful transaction.
     *
     * @param request refund request
     * @return {@link RefundResponse} with refund details
     */
    RefundResponse refund(RefundRequest request);

    /**
     * Returns all transactions in the system (for admin/testing).
     */
    List<PaymentResponse> getAllTransactions();
}
