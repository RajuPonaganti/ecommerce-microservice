-- =============================================================================
-- V1: Create transactions and refunds tables
-- =============================================================================

CREATE TABLE IF NOT EXISTS transactions (
    id                   BIGSERIAL       PRIMARY KEY,
    transaction_id       VARCHAR(64)     NOT NULL UNIQUE,
    order_id             VARCHAR(128)    NOT NULL,
    merchant_id          VARCHAR(64)     NOT NULL,
    amount               NUMERIC(15, 2)  NOT NULL,
    currency             CHAR(3)         NOT NULL DEFAULT 'INR',
    payment_mode         VARCHAR(20)     NOT NULL,
    status               VARCHAR(30)     NOT NULL,

    -- Card-specific fields
    card_last_four       CHAR(4),
    card_type            VARCHAR(10),
    card_network         VARCHAR(20),

    -- Net Banking field
    bank_code            VARCHAR(20),

    -- UPI field
    upi_id               VARCHAR(128),

    -- Shared fields
    failure_reason       TEXT,
    gateway_reference_id VARCHAR(64),
    customer_email       VARCHAR(255),
    customer_phone       VARCHAR(15),
    customer_name        VARCHAR(255),
    description          TEXT,

    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMP
);

CREATE INDEX idx_transactions_order_id       ON transactions (order_id);
CREATE INDEX idx_transactions_merchant_id    ON transactions (merchant_id);
CREATE INDEX idx_transactions_status         ON transactions (status);
CREATE INDEX idx_transactions_payment_mode   ON transactions (payment_mode);
CREATE INDEX idx_transactions_customer_email ON transactions (customer_email);
CREATE INDEX idx_transactions_created_at     ON transactions (created_at DESC);

-- =============================================================================

CREATE TABLE IF NOT EXISTS refunds (
    id             BIGSERIAL       PRIMARY KEY,
    refund_id      VARCHAR(64)     NOT NULL UNIQUE,
    transaction_id VARCHAR(64)     NOT NULL REFERENCES transactions (transaction_id) ON DELETE RESTRICT,
    amount         NUMERIC(15, 2)  NOT NULL,
    status         VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS',
    reason         TEXT,
    created_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP
);

CREATE INDEX idx_refunds_transaction_id ON refunds (transaction_id);
CREATE INDEX idx_refunds_status         ON refunds (status);
