package com.paymentgateway.model.enums;

/**
 * Supported banks for Net Banking payments.
 */
public enum BankCode {
    SBI("State Bank of India"),
    HDFC("HDFC Bank"),
    ICICI("ICICI Bank"),
    AXIS("Axis Bank"),
    KOTAK("Kotak Mahindra Bank"),
    PNB("Punjab National Bank"),
    BOB("Bank of Baroda"),
    CANARA("Canara Bank"),
    IDBI("IDBI Bank"),
    YES("Yes Bank");

    private final String displayName;

    BankCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
