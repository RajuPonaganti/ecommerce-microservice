-- =============================================================================
-- V3: Composition tables for payment details + bank master data
--
-- Changes from V1 flat schema:
--   • Remove mode-specific columns from `transactions`
--   • Create `banks` (master data)
--   • Create `card_payment_details`
--   • Create `upi_payment_details`
--   • Create `netbanking_payment_details`
--   • Create `validation_audit`
-- =============================================================================

-- ── 1. Drop mode-specific columns from transactions ───────────────────────────
ALTER TABLE transactions
    DROP COLUMN IF EXISTS card_last_four,
    DROP COLUMN IF EXISTS card_type,
    DROP COLUMN IF EXISTS card_network,
    DROP COLUMN IF EXISTS bank_code,
    DROP COLUMN IF EXISTS upi_id;

-- ── 2. Bank master data ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS banks (
    id                   BIGSERIAL    PRIMARY KEY,
    bank_code            VARCHAR(20)  NOT NULL UNIQUE,
    bank_name            VARCHAR(100) NOT NULL,
    ifsc_prefix          VARCHAR(10),
    upi_handles          VARCHAR(255),
    supports_net_banking BOOLEAN      NOT NULL DEFAULT TRUE,
    supports_upi         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_banks_bank_code ON banks (bank_code);
CREATE INDEX idx_banks_is_active ON banks (is_active);

-- Seed bank master data
INSERT INTO banks (bank_code, bank_name, ifsc_prefix, upi_handles, supports_net_banking, supports_upi, is_active)
VALUES
    ('SBI',    'State Bank of India',    'SBIN', 'oksbi,sbi',                    TRUE, TRUE,  TRUE),
    ('HDFC',   'HDFC Bank',              'HDFC', 'okhdfc,hdfcbank',              TRUE, TRUE,  TRUE),
    ('ICICI',  'ICICI Bank',             'ICIC', 'okicici,icici',                TRUE, TRUE,  TRUE),
    ('AXIS',   'Axis Bank',              'UTIB', 'okaxis,axisbank',              TRUE, TRUE,  TRUE),
    ('KOTAK',  'Kotak Mahindra Bank',    'KKBK', 'kotak,okkotak',               TRUE, TRUE,  TRUE),
    ('PNB',    'Punjab National Bank',   'PUNB', 'okpnb,pnb',                   TRUE, TRUE,  TRUE),
    ('BOB',    'Bank of Baroda',         'BARB', 'okbob,barodampay',             TRUE, TRUE,  TRUE),
    ('CANARA', 'Canara Bank',            'CNRB', 'okcanara,cnrb',               TRUE, TRUE,  TRUE),
    ('IDBI',   'IDBI Bank',              'IBKL', 'okidbi,idbi',                 TRUE, FALSE, TRUE),
    ('YES',    'Yes Bank',               'YESB', 'okyesbank,yesbank',           TRUE, TRUE,  TRUE),
    ('PAYTM',  'Paytm Payments Bank',    'PYTM', 'paytm',                       FALSE, TRUE, TRUE),
    ('AIRTEL', 'Airtel Payments Bank',   'AIRP', 'airtel',                      FALSE, TRUE, TRUE);

-- ── 3. Card payment details ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS card_payment_details (
    id                 BIGSERIAL    PRIMARY KEY,
    transaction_id     VARCHAR(64)  NOT NULL UNIQUE
                           REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    card_number_masked VARCHAR(20)  NOT NULL,  -- e.g. "**** **** **** 1234"
    card_bin           VARCHAR(8)   NOT NULL,  -- first 6 digits (BIN)
    card_network       VARCHAR(20)  NOT NULL,  -- VISA / Mastercard / RuPay / Amex
    card_type          VARCHAR(10)  NOT NULL,  -- CREDIT / DEBIT
    expiry_month       SMALLINT     NOT NULL,  -- 1-12
    expiry_year        SMALLINT     NOT NULL,  -- e.g. 2026
    card_holder_name   VARCHAR(255) NOT NULL,
    -- CVV is intentionally NOT stored here
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_card_details_txn_id ON card_payment_details (transaction_id);

-- ── 4. UPI payment details ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS upi_payment_details (
    id             BIGSERIAL    PRIMARY KEY,
    transaction_id VARCHAR(64)  NOT NULL UNIQUE
                       REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    vpa            VARCHAR(128) NOT NULL,   -- e.g. rajesh@okaxis
    bank_handle    VARCHAR(64)  NOT NULL,   -- extracted part after '@'
    upi_txn_ref_id VARCHAR(64)  NOT NULL,   -- simulated NPCI reference
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_upi_details_txn_id ON upi_payment_details (transaction_id);
CREATE INDEX idx_upi_details_vpa    ON upi_payment_details (vpa);

-- ── 5. Net Banking payment details ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS netbanking_payment_details (
    id               BIGSERIAL    PRIMARY KEY,
    transaction_id   VARCHAR(64)  NOT NULL UNIQUE
                         REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    bank_code        VARCHAR(20)  NOT NULL,
    bank_name        VARCHAR(100) NOT NULL,
    mock_auth_ref_id VARCHAR(64),  -- filled when verify() is called
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_nb_details_txn_id    ON netbanking_payment_details (transaction_id);
CREATE INDEX idx_nb_details_bank_code ON netbanking_payment_details (bank_code);

-- ── 6. Validation audit ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS validation_audit (
    id             BIGSERIAL    PRIMARY KEY,
    transaction_id VARCHAR(64),           -- nullable until txn is persisted
    rule_name      VARCHAR(64)  NOT NULL,
    result         VARCHAR(10)  NOT NULL, -- PASS or FAIL
    message        TEXT,
    payment_mode   VARCHAR(20),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_val_audit_txn_id  ON validation_audit (transaction_id);
CREATE INDEX idx_val_audit_rule    ON validation_audit (rule_name);
CREATE INDEX idx_val_audit_result  ON validation_audit (result);
CREATE INDEX idx_val_audit_created ON validation_audit (created_at DESC);
