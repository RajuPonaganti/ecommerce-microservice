-- =============================================================================
-- V4: Populate composition detail tables for the 12 test transactions
--     that were seeded in V2.
--
-- After V3 removed the flat columns from `transactions`, we need rows in
-- the new detail tables to make existing test data fully queryable.
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- CARD payment details (4 transactions)
-- ─────────────────────────────────────────────────────────────────────────────

-- TXN-CARD-SUCCESS001 → VISA Credit ****1111
INSERT INTO card_payment_details
    (transaction_id, card_number_masked, card_bin, card_network, card_type, expiry_month, expiry_year, card_holder_name)
VALUES
    ('TXN-CARD-SUCCESS001', '**** **** **** 1111', '411111', 'VISA',       'CREDIT', 12, 2026, 'RAJESH KUMAR');

-- TXN-CARD-FAILED001 → Mastercard Debit ****4242
INSERT INTO card_payment_details
    (transaction_id, card_number_masked, card_bin, card_network, card_type, expiry_month, expiry_year, card_holder_name)
VALUES
    ('TXN-CARD-FAILED001',  '**** **** **** 4242', '524001', 'Mastercard', 'DEBIT',  8,  2025, 'PRIYA SHARMA');

-- TXN-CARD-PENDING01 → RuPay Credit ****6789
INSERT INTO card_payment_details
    (transaction_id, card_number_masked, card_bin, card_network, card_type, expiry_month, expiry_year, card_holder_name)
VALUES
    ('TXN-CARD-PENDING01',  '**** **** **** 6789', '607059', 'RuPay',      'CREDIT', 3,  2027, 'AMIT VERMA');

-- TXN-CARD-REFUND001 → VISA Credit ****9999
INSERT INTO card_payment_details
    (transaction_id, card_number_masked, card_bin, card_network, card_type, expiry_month, expiry_year, card_holder_name)
VALUES
    ('TXN-CARD-REFUND001',  '**** **** **** 9999', '411111', 'VISA',       'CREDIT', 6,  2026, 'NEHA SINGH');

-- ─────────────────────────────────────────────────────────────────────────────
-- NET BANKING payment details (4 transactions)
-- ─────────────────────────────────────────────────────────────────────────────

-- TXN-NB-SUCCESS001 → HDFC (SUCCESS – auth ref present)
INSERT INTO netbanking_payment_details
    (transaction_id, bank_code, bank_name, mock_auth_ref_id)
VALUES
    ('TXN-NB-SUCCESS001', 'HDFC', 'HDFC Bank',  'BANK-AUTH-SUCCESS001');

-- TXN-NB-FAILED0001 → SBI (FAILED – no auth ref)
INSERT INTO netbanking_payment_details
    (transaction_id, bank_code, bank_name, mock_auth_ref_id)
VALUES
    ('TXN-NB-FAILED0001', 'SBI',  'State Bank of India', NULL);

-- TXN-NB-PENDING001 → ICICI (PENDING – awaiting auth)
INSERT INTO netbanking_payment_details
    (transaction_id, bank_code, bank_name, mock_auth_ref_id)
VALUES
    ('TXN-NB-PENDING001', 'ICICI', 'ICICI Bank', NULL);

-- TXN-NB-PARTIAL001 → AXIS (PARTIALLY_REFUNDED – auth ref present)
INSERT INTO netbanking_payment_details
    (transaction_id, bank_code, bank_name, mock_auth_ref_id)
VALUES
    ('TXN-NB-PARTIAL001', 'AXIS', 'Axis Bank', 'BANK-AUTH-PARTIAL001');

-- ─────────────────────────────────────────────────────────────────────────────
-- UPI payment details (4 transactions)
-- ─────────────────────────────────────────────────────────────────────────────

-- TXN-UPI-SUCCESS01 → rajesh@okaxis (Axis Pay)
INSERT INTO upi_payment_details
    (transaction_id, vpa, bank_handle, upi_txn_ref_id)
VALUES
    ('TXN-UPI-SUCCESS01', 'rajesh@okaxis',    'okaxis',    'NPCI-SUCCESS01ABCD');

-- TXN-UPI-SUCCESS02 → priya@ybl (PhonePe)
INSERT INTO upi_payment_details
    (transaction_id, vpa, bank_handle, upi_txn_ref_id)
VALUES
    ('TXN-UPI-SUCCESS02', 'priya@ybl',        'ybl',       'NPCI-SUCCESS02EFGH');

-- TXN-UPI-FAILED001 → unknown@somebank (failed)
INSERT INTO upi_payment_details
    (transaction_id, vpa, bank_handle, upi_txn_ref_id)
VALUES
    ('TXN-UPI-FAILED001', 'unknown@somebank', 'somebank',  'NPCI-FAILED001IJKL');

-- TXN-UPI-REFUND001 → amit@gpay (Google Pay, refunded)
INSERT INTO upi_payment_details
    (transaction_id, vpa, bank_handle, upi_txn_ref_id)
VALUES
    ('TXN-UPI-REFUND001', 'amit@gpay',        'gpay',      'NPCI-REFUND01MNOP');

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed validation_audit rows for the 12 test transactions
-- (representative PASS records – simulates what the validators would have written)
-- ─────────────────────────────────────────────────────────────────────────────

-- CARD – SUCCESS001
INSERT INTO validation_audit (transaction_id, rule_name, result, message, payment_mode) VALUES
    ('TXN-CARD-SUCCESS001', 'CARD_NUMBER_PRESENT',     'PASS', 'Card number field is present.',                          'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_NUMBER_FORMAT',      'PASS', 'Card number format is valid.',                           'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_LUHN_CHECK',         'PASS', 'Card number passed Luhn algorithm check.',               'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_EXPIRY_FORMAT',      'PASS', 'Expiry format MM/YY is valid.',                          'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_EXPIRY_NOT_PAST',    'PASS', 'Card expiry 12/26 is valid.',                            'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_CVV_PRESENT',        'PASS', 'CVV field is present.',                                  'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_CVV_FORMAT',         'PASS', 'CVV format is valid. CVV will not be stored.',           'CARD'),
    ('TXN-CARD-SUCCESS001', 'CARD_HOLDER_NAME_PRESENT','PASS', 'Card holder name is present.',                           'CARD'),
    ('TXN-CARD-SUCCESS001', 'AMOUNT_POSITIVE',         'PASS', 'Amount 1500.00 is positive.',                            'CARD');

-- CARD – FAILED001 (wrong OTP — all pre-checks still passed)
INSERT INTO validation_audit (transaction_id, rule_name, result, message, payment_mode) VALUES
    ('TXN-CARD-FAILED001',  'CARD_NUMBER_PRESENT',     'PASS', 'Card number field is present.',                          'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_NUMBER_FORMAT',      'PASS', 'Card number format is valid.',                           'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_LUHN_CHECK',         'PASS', 'Card number passed Luhn algorithm check.',               'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_EXPIRY_FORMAT',      'PASS', 'Expiry format MM/YY is valid.',                          'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_EXPIRY_NOT_PAST',    'PASS', 'Card expiry 08/25 is valid.',                            'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_CVV_PRESENT',        'PASS', 'CVV field is present.',                                  'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_CVV_FORMAT',         'PASS', 'CVV format is valid. CVV will not be stored.',           'CARD'),
    ('TXN-CARD-FAILED001',  'CARD_HOLDER_NAME_PRESENT','PASS', 'Card holder name is present.',                           'CARD'),
    ('TXN-CARD-FAILED001',  'AMOUNT_POSITIVE',         'PASS', 'Amount 2999.50 is positive.',                            'CARD');

-- NET_BANKING – SUCCESS001 (HDFC)
INSERT INTO validation_audit (transaction_id, rule_name, result, message, payment_mode) VALUES
    ('TXN-NB-SUCCESS001',   'BANK_CODE_PRESENT',          'PASS', 'Bank code field is present: HDFC.',                   'NET_BANKING'),
    ('TXN-NB-SUCCESS001',   'BANK_CODE_EXISTS',           'PASS', 'Bank code HDFC exists in master data.',               'NET_BANKING'),
    ('TXN-NB-SUCCESS001',   'BANK_ACTIVE',                'PASS', 'Bank HDFC Bank is active.',                           'NET_BANKING'),
    ('TXN-NB-SUCCESS001',   'BANK_SUPPORTS_NETBANKING',   'PASS', 'Bank HDFC Bank supports net banking.',                'NET_BANKING'),
    ('TXN-NB-SUCCESS001',   'AMOUNT_POSITIVE',            'PASS', 'Amount 12500.00 is positive.',                        'NET_BANKING');

-- NET_BANKING – FAILED0001 (SBI – all validation passed, bank timed out at runtime)
INSERT INTO validation_audit (transaction_id, rule_name, result, message, payment_mode) VALUES
    ('TXN-NB-FAILED0001',   'BANK_CODE_PRESENT',          'PASS', 'Bank code field is present: SBI.',                    'NET_BANKING'),
    ('TXN-NB-FAILED0001',   'BANK_CODE_EXISTS',           'PASS', 'Bank code SBI exists in master data.',                'NET_BANKING'),
    ('TXN-NB-FAILED0001',   'BANK_ACTIVE',                'PASS', 'Bank State Bank of India is active.',                 'NET_BANKING'),
    ('TXN-NB-FAILED0001',   'BANK_SUPPORTS_NETBANKING',   'PASS', 'Bank State Bank of India supports net banking.',      'NET_BANKING'),
    ('TXN-NB-FAILED0001',   'AMOUNT_POSITIVE',            'PASS', 'Amount 3200.00 is positive.',                         'NET_BANKING');

-- UPI – SUCCESS01 (rajesh@okaxis)
INSERT INTO validation_audit (transaction_id, rule_name, result, message, payment_mode) VALUES
    ('TXN-UPI-SUCCESS01',   'UPI_ID_PRESENT',             'PASS', 'UPI ID field is present.',                            'UPI'),
    ('TXN-UPI-SUCCESS01',   'UPI_FORMAT_CHECK',           'PASS', 'UPI ID format is valid: rajesh@okaxis.',              'UPI'),
    ('TXN-UPI-SUCCESS01',   'UPI_BANK_HANDLE_EXISTS',     'PASS', 'Bank handle @okaxis is registered and active.',       'UPI'),
    ('TXN-UPI-SUCCESS01',   'AMOUNT_POSITIVE',            'PASS', 'Amount 499.00 is positive.',                          'UPI');

-- UPI – FAILED001 (unknown@somebank – UPI ID format passed but bank handle unknown)
INSERT INTO validation_audit (transaction_id, rule_name, result, message, payment_mode) VALUES
    ('TXN-UPI-FAILED001',   'UPI_ID_PRESENT',             'PASS', 'UPI ID field is present.',                            'UPI'),
    ('TXN-UPI-FAILED001',   'UPI_FORMAT_CHECK',           'PASS', 'UPI ID format is valid: unknown@somebank.',           'UPI'),
    ('TXN-UPI-FAILED001',   'UPI_BANK_HANDLE_EXISTS',     'PASS', 'Bank handle @somebank not in master list; proceeding (simulation).', 'UPI'),
    ('TXN-UPI-FAILED001',   'AMOUNT_POSITIVE',            'PASS', 'Amount 1100.00 is positive.',                         'UPI');
