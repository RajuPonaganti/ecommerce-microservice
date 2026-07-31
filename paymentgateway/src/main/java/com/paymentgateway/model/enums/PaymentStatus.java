package com.paymentgateway.model.enums;

/**
 * All possible lifecycle states of a payment transaction.
 */
public enum PaymentStatus {
    /** Transaction created; awaiting provider initiation response. */
    INITIATED,
    /** Payment is in progress (OTP pending, or bank redirect in progress). */
    PENDING,
    /** Payment completed successfully. */
    SUCCESS,
    /** Payment was declined or encountered an error. */
    FAILED,
    /** Full amount has been refunded. */
    REFUNDED,
    /** A portion of the amount has been refunded. */
    PARTIALLY_REFUNDED
}
