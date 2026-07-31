-- =============================================================================
-- V2: Insert test data – 12 transactions covering all payment modes & statuses
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- CARD – SUCCESS (VISA Credit)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, card_last_four, card_type, card_network,
    gateway_reference_id, customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-CARD-SUCCESS001', 'ORD-2024-0001', 'MERCH-001', 1500.00, 'INR',
    'CARD', 'SUCCESS', '1111', 'CREDIT', 'VISA',
    'GW-REF-V001A1',
    'rajesh.kumar@email.com', '9876543210', 'Rajesh Kumar', 'Purchase of Electronics',
    NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- CARD – FAILED (Mastercard Debit, wrong OTP)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, card_last_four, card_type, card_network,
    failure_reason, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at)
VALUES ('TXN-CARD-FAILED001', 'ORD-2024-0002', 'MERCH-001', 2999.50, 'INR',
    'CARD', 'FAILED', '4242', 'DEBIT', 'Mastercard',
    'Wrong OTP entered. Payment declined.', 'GW-REF-V002B2',
    'priya.sharma@email.com', '8765432109', 'Priya Sharma', 'Online Course Subscription',
    NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- CARD – PENDING (RuPay Credit, awaiting OTP)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, card_last_four, card_type, card_network,
    gateway_reference_id, customer_email, customer_phone, customer_name, description,
    created_at, updated_at)
VALUES ('TXN-CARD-PENDING01', 'ORD-2024-0003', 'MERCH-002', 750.00, 'INR',
    'CARD', 'PENDING', '6789', 'CREDIT', 'RuPay',
    'GW-REF-V003C3',
    'amit.verma@email.com', '7654321098', 'Amit Verma', 'Movie Tickets',
    NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour');

-- ─────────────────────────────────────────────────────────────────────────────
-- CARD – REFUNDED (VISA Credit, full refund)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, card_last_four, card_type, card_network,
    gateway_reference_id, customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-CARD-REFUND001', 'ORD-2024-0004', 'MERCH-001', 5000.00, 'INR',
    'CARD', 'REFUNDED', '9999', 'CREDIT', 'VISA',
    'GW-REF-V004D4',
    'neha.singh@email.com', '9123456789', 'Neha Singh', 'Flight Booking',
    NOW() - INTERVAL '10 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '10 days');

INSERT INTO refunds (refund_id, transaction_id, amount, status, reason, created_at, updated_at, processed_at)
VALUES ('RFD-REFUND001', 'TXN-CARD-REFUND001', 5000.00, 'SUCCESS',
    'Flight cancelled by airline',
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- NET_BANKING – SUCCESS (HDFC)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, bank_code, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-NB-SUCCESS001', 'ORD-2024-0005', 'MERCH-003', 12500.00, 'INR',
    'NET_BANKING', 'SUCCESS', 'HDFC', 'GW-REF-N001E5',
    'suresh.patel@email.com', '9988776655', 'Suresh Patel', 'Hotel Booking – Goa',
    NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- NET_BANKING – FAILED (SBI, bank timeout)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, bank_code, failure_reason, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at)
VALUES ('TXN-NB-FAILED0001', 'ORD-2024-0006', 'MERCH-003', 3200.00, 'INR',
    'NET_BANKING', 'FAILED', 'SBI',
    'Bank server timeout. Please try again.', 'GW-REF-N002F6',
    'kavita.rao@email.com', '8877665544', 'Kavita Rao', 'Insurance Premium',
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- NET_BANKING – PENDING (ICICI, awaiting bank redirect)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, bank_code, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at)
VALUES ('TXN-NB-PENDING001', 'ORD-2024-0007', 'MERCH-002', 8900.00, 'INR',
    'NET_BANKING', 'PENDING', 'ICICI', 'GW-REF-N003G7',
    'vikram.joshi@email.com', '9765432108', 'Vikram Joshi', 'Car Service Booking',
    NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes');

-- ─────────────────────────────────────────────────────────────────────────────
-- NET_BANKING – PARTIALLY_REFUNDED (Axis Bank)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, bank_code, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-NB-PARTIAL001', 'ORD-2024-0008', 'MERCH-001', 6000.00, 'INR',
    'NET_BANKING', 'PARTIALLY_REFUNDED', 'AXIS', 'GW-REF-N004H8',
    'meera.nair@email.com', '9654321087', 'Meera Nair', 'Travel Package',
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '15 days');

INSERT INTO refunds (refund_id, transaction_id, amount, status, reason, created_at, updated_at, processed_at)
VALUES ('RFD-PARTIAL001', 'TXN-NB-PARTIAL001', 2000.00, 'SUCCESS',
    'Partial cancellation of tour itinerary',
    NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- UPI – SUCCESS (Axis Pay)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, upi_id, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-UPI-SUCCESS01', 'ORD-2024-0009', 'MERCH-004', 499.00, 'INR',
    'UPI', 'SUCCESS', 'rajesh@okaxis', 'GW-REF-U001I9',
    'rajesh.kumar@email.com', '9876543210', 'Rajesh Kumar', 'OTT Subscription',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

-- ─────────────────────────────────────────────────────────────────────────────
-- UPI – SUCCESS (PhonePe)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, upi_id, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-UPI-SUCCESS02', 'ORD-2024-0010', 'MERCH-004', 2345.00, 'INR',
    'UPI', 'SUCCESS', 'priya@ybl', 'GW-REF-U002J10',
    'priya.sharma@email.com', '8765432109', 'Priya Sharma', 'Grocery Order',
    NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- ─────────────────────────────────────────────────────────────────────────────
-- UPI – FAILED (UPI ID not registered)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, upi_id, failure_reason, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at)
VALUES ('TXN-UPI-FAILED001', 'ORD-2024-0011', 'MERCH-005', 1100.00, 'INR',
    'UPI', 'FAILED', 'unknown@somebank',
    'UPI ID not registered with any bank.', 'GW-REF-U003K11',
    'test.user@email.com', '9000000001', 'Test User', 'Test Payment',
    NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours');

-- ─────────────────────────────────────────────────────────────────────────────
-- UPI – REFUNDED (Google Pay)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO transactions (transaction_id, order_id, merchant_id, amount, currency,
    payment_mode, status, upi_id, gateway_reference_id,
    customer_email, customer_phone, customer_name, description,
    created_at, updated_at, completed_at)
VALUES ('TXN-UPI-REFUND001', 'ORD-2024-0012', 'MERCH-004', 350.00, 'INR',
    'UPI', 'REFUNDED', 'amit@gpay', 'GW-REF-U004L12',
    'amit.verma@email.com', '7654321098', 'Amit Verma', 'Food Delivery',
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '8 days');

INSERT INTO refunds (refund_id, transaction_id, amount, status, reason, created_at, updated_at, processed_at)
VALUES ('RFD-UPI-REFUND01', 'TXN-UPI-REFUND001', 350.00, 'SUCCESS',
    'Order not delivered',
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');
