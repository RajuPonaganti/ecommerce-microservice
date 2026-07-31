package com.paymentgateway.model.enums;

/**
 * Supported payment modes in the gateway.
 */
public enum PaymentMode {
    /** Debit/Credit card payment (Visa, Mastercard, RuPay) */
    CARD,
    /** Net Banking via bank portal redirect */
    NET_BANKING,
    /** Unified Payments Interface (UPI) via VPA */
    UPI
}
