# Payment Gateway API Documentation

**Version:** 1.0.0  
**Base URL:** `http://localhost:8080`  
**OpenAPI / Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON spec:** `http://localhost:8080/api-docs`

---

## Overview

The Payment Gateway Simulation API enables end-to-end testing of payment flows for three modes:

| Mode | Description | Verification Required |
|------|-------------|----------------------|
| `CARD` | Debit / Credit (Visa, Mastercard, RuPay) | Yes – OTP (POST `/verify`) |
| `NET_BANKING` | SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, Canara, IDBI, Yes | Yes – Bank callback (POST `/verify`) |
| `UPI` | VPA / UPI ID (e.g. `name@bank`) | No – Resolved immediately |

---

## Unified Response Format

Every endpoint returns the following envelope:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Human-readable context message",
  "data": { ... },
  "errorCode": null,
  "error": null,
  "timestamp": "2024-07-31T10:30:00"
}
```

On failure:
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Refund amount exceeds remaining refundable amount.",
  "data": null,
  "errorCode": "REFUND_AMOUNT_EXCEEDED",
  "error": "Detailed error info",
  "timestamp": "2024-07-31T10:30:00"
}
```

---

## Payment Status Values

| Status | Description |
|--------|-------------|
| `INITIATED` | Transaction created |
| `PENDING` | Awaiting OTP (CARD) or bank authentication (NET_BANKING) |
| `SUCCESS` | Payment completed |
| `FAILED` | Payment declined or errored |
| `REFUNDED` | Fully refunded |
| `PARTIALLY_REFUNDED` | Partially refunded |

---

## Endpoints

---

### 1. Initiate Payment

Starts a new payment transaction. Returns `PENDING` for CARD/NET_BANKING; returns `SUCCESS` or `FAILED` immediately for UPI.

```
POST /api/v1/payments/initiate
Content-Type: application/json
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | string | ✅ | Merchant-side unique order identifier |
| `merchantId` | string | ✅ | Registered merchant ID |
| `amount` | decimal | ✅ | Payment amount (min 1.00, max 13 digits) |
| `currency` | string | ✅ | ISO 4217 code, e.g. `INR` |
| `paymentMode` | enum | ✅ | `CARD`, `NET_BANKING`, `UPI` |
| `customerName` | string | ✅ | Full name of the customer |
| `customerEmail` | string | ✅ | Valid email address |
| `customerPhone` | string | ❌ | 10-digit Indian mobile (6–9 prefix) |
| `description` | string | ❌ | Payment description |
| `cardNumber` | string | CARD only | 15–19 digit card number |
| `cardExpiry` | string | CARD only | MM/YY format (e.g. `12/26`) |
| `cvv` | string | CARD only | 3 or 4 digits |
| `cardHolderName` | string | CARD only | Name as on card |
| `bankCode` | enum | NET_BANKING only | `SBI`, `HDFC`, `ICICI`, `AXIS`, `KOTAK`, `PNB`, `BOB`, `CANARA`, `IDBI`, `YES` |
| `upiId` | string | UPI only | VPA format `username@bankhandle` |

#### Example Requests

**CARD:**
```json
{
  "orderId": "ORD-2024-TEST-001",
  "merchantId": "MERCH-001",
  "amount": 1500.00,
  "currency": "INR",
  "paymentMode": "CARD",
  "customerName": "Rajesh Kumar",
  "customerEmail": "rajesh.kumar@email.com",
  "customerPhone": "9876543210",
  "description": "Purchase of Electronics",
  "cardNumber": "4111111111111111",
  "cardExpiry": "12/26",
  "cvv": "123",
  "cardHolderName": "RAJESH KUMAR"
}
```

**NET_BANKING:**
```json
{
  "orderId": "ORD-2024-TEST-002",
  "merchantId": "MERCH-002",
  "amount": 5000.00,
  "currency": "INR",
  "paymentMode": "NET_BANKING",
  "customerName": "Priya Sharma",
  "customerEmail": "priya.sharma@email.com",
  "customerPhone": "8765432109",
  "description": "Hotel Booking",
  "bankCode": "HDFC"
}
```

**UPI:**
```json
{
  "orderId": "ORD-2024-TEST-003",
  "merchantId": "MERCH-003",
  "amount": 499.00,
  "currency": "INR",
  "paymentMode": "UPI",
  "customerName": "Amit Verma",
  "customerEmail": "amit.verma@email.com",
  "customerPhone": "7654321098",
  "description": "OTT Subscription",
  "upiId": "amit@okaxis"
}
```

#### Responses

**201 Created – CARD (PENDING)**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "OTP has been sent to your registered mobile number. Please verify to complete the payment.",
  "data": {
    "transactionId": "TXN-CARD-A1B2C3D4E5",
    "orderId": "ORD-2024-TEST-001",
    "merchantId": "MERCH-001",
    "amount": 1500.00,
    "currency": "INR",
    "paymentMode": "CARD",
    "status": "PENDING",
    "gatewayReferenceId": "GW-REF-12345678",
    "cardLastFour": "1111",
    "cardType": "CREDIT",
    "cardNetwork": "VISA",
    "createdAt": "2024-07-31T10:30:00"
  }
}
```

**201 Created – UPI (SUCCESS)**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Payment processed successfully.",
  "data": {
    "transactionId": "TXN-UPI-F6G7H8I9J0",
    "orderId": "ORD-2024-TEST-003",
    "merchantId": "MERCH-003",
    "amount": 499.00,
    "currency": "INR",
    "paymentMode": "UPI",
    "status": "SUCCESS",
    "gatewayReferenceId": "GW-REF-ABCDEF12",
    "upiId": "amit@okaxis",
    "createdAt": "2024-07-31T10:30:00",
    "completedAt": "2024-07-31T10:30:01"
  }
}
```

**400 Bad Request**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Request validation failed.",
  "errorCode": "VALIDATION_ERROR",
  "error": "Card number is required for CARD payment mode."
}
```

**409 Conflict – Duplicate Order**
```json
{
  "success": false,
  "statusCode": 409,
  "message": "A transaction already exists for orderId: ORD-2024-TEST-001",
  "errorCode": "DUPLICATE_ORDER_ID"
}
```

---

### 2. Verify Payment

Completes a `PENDING` CARD or NET_BANKING transaction. Not needed for UPI (no-op if called).

```
POST /api/v1/payments/verify
Content-Type: application/json
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `transactionId` | string | ✅ | Transaction ID returned during initiation |
| `otp` | string | CARD only | 6-digit OTP from customer's mobile |
| `bankConfirmationToken` | string | NET_BANKING only | Any non-blank token to simulate bank callback |

#### Example Requests

**CARD:**
```json
{
  "transactionId": "TXN-CARD-A1B2C3D4E5",
  "otp": "123456"
}
```

**NET_BANKING:**
```json
{
  "transactionId": "TXN-NB-K1L2M3N4O5",
  "bankConfirmationToken": "BANK-CALLBACK-TOKEN-XYZ"
}
```

#### Simulation Outcomes

| Mode | Probability | Status | Reason |
|------|-------------|--------|--------|
| CARD | 75% | SUCCESS | – |
| CARD | 10% | FAILED | Wrong OTP |
| CARD | 15% | FAILED | Insufficient balance |
| NET_BANKING | 85% | SUCCESS | – |
| NET_BANKING | 5% | FAILED | Bank server timeout |
| NET_BANKING | 10% | FAILED | Invalid credentials |

#### Responses

**200 OK – SUCCESS**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Payment verified and completed successfully.",
  "data": {
    "transactionId": "TXN-CARD-A1B2C3D4E5",
    "orderId": "ORD-2024-TEST-001",
    "amount": 1500.00,
    "currency": "INR",
    "paymentMode": "CARD",
    "status": "SUCCESS",
    "cardLastFour": "1111",
    "cardNetwork": "VISA",
    "completedAt": "2024-07-31T10:30:05"
  }
}
```

**200 OK – FAILED**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Payment verification failed: Insufficient balance in linked account.",
  "data": {
    "transactionId": "TXN-CARD-A1B2C3D4E5",
    "status": "FAILED",
    "failureReason": "Insufficient balance in linked account."
  }
}
```

**404 Not Found**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Transaction not found with ID: TXN-INVALID-ID",
  "errorCode": "TRANSACTION_NOT_FOUND"
}
```

---

### 3. Get Payment Status

Returns the current state and full details of a transaction.

```
GET /api/v1/payments/{transactionId}/status
```

#### Path Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `transactionId` | Gateway-issued transaction ID | `TXN-CARD-SUCCESS001` |

#### Example

```
GET /api/v1/payments/TXN-CARD-SUCCESS001/status
```

#### Response – 200 OK

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Transaction status retrieved successfully.",
  "data": {
    "transactionId": "TXN-CARD-SUCCESS001",
    "orderId": "ORD-2024-0001",
    "merchantId": "MERCH-001",
    "amount": 1500.00,
    "currency": "INR",
    "paymentMode": "CARD",
    "status": "SUCCESS",
    "gatewayReferenceId": "GW-REF-V001A1",
    "cardLastFour": "1111",
    "cardType": "CREDIT",
    "cardNetwork": "VISA",
    "createdAt": "2024-07-26T10:00:00",
    "completedAt": "2024-07-26T10:00:05"
  }
}
```

#### Response – 404 Not Found

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Transaction not found with ID: TXN-MISSING",
  "errorCode": "TRANSACTION_NOT_FOUND"
}
```

---

### 4. List All Transactions

Returns all transactions in the system. Intended for admin and testing use.

```
GET /api/v1/payments
```

#### Response – 200 OK

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Retrieved 12 transaction(s).",
  "data": [
    {
      "transactionId": "TXN-CARD-SUCCESS001",
      "status": "SUCCESS",
      "paymentMode": "CARD",
      "amount": 1500.00,
      ...
    },
    ...
  ]
}
```

---

### 5. Initiate Refund

Creates a full or partial refund against a successful transaction.

```
POST /api/v1/refunds
Content-Type: application/json
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `transactionId` | string | ✅ | Original transaction ID to refund |
| `amount` | decimal | ✅ | Refund amount (min 1.00; cannot exceed remaining balance) |
| `reason` | string | ✅ | Reason for the refund |

#### Business Rules

- Only `SUCCESS` or `PARTIALLY_REFUNDED` transactions are eligible.
- Cumulative refunds cannot exceed the original payment amount.
- Partial refund → transaction status becomes `PARTIALLY_REFUNDED`.
- Full refund → transaction status becomes `REFUNDED`.

#### Example Requests

**Full Refund:**
```json
{
  "transactionId": "TXN-CARD-SUCCESS001",
  "amount": 1500.00,
  "reason": "Customer requested cancellation"
}
```

**Partial Refund:**
```json
{
  "transactionId": "TXN-NB-SUCCESS001",
  "amount": 3000.00,
  "reason": "Partial service not delivered"
}
```

#### Response – 201 Created

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Refund initiated successfully. Amount will be credited in 5-7 business days.",
  "data": {
    "refundId": "RFD-A1B2C3D4E5",
    "transactionId": "TXN-CARD-SUCCESS001",
    "refundAmount": 1500.00,
    "originalAmount": 1500.00,
    "status": "SUCCESS",
    "reason": "Customer requested cancellation",
    "initiatedAt": "2024-07-31T10:35:00",
    "processedAt": "2024-07-31T10:35:00",
    "estimatedCreditDays": "5-7 business days"
  }
}
```

**400 Bad Request – Amount Exceeded:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Refund amount 2000.00 exceeds remaining refundable amount of 1500.00.",
  "errorCode": "REFUND_AMOUNT_EXCEEDED"
}
```

**400 Bad Request – Invalid Status:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Refunds are only allowed on successful transactions. Current status: FAILED",
  "errorCode": "INVALID_TRANSACTION_STATE_FOR_REFUND"
}
```

---

### 6. Get Refund Details

```
GET /api/v1/refunds/{refundId}
```

#### Path Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `refundId` | Unique refund identifier | `RFD-REFUND001` |

#### Response – 200 OK

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Refund details retrieved successfully.",
  "data": {
    "refundId": "RFD-REFUND001",
    "transactionId": "TXN-CARD-REFUND001",
    "refundAmount": 5000.00,
    "originalAmount": 5000.00,
    "status": "SUCCESS",
    "reason": "Flight cancelled by airline",
    "initiatedAt": "2024-07-28T10:00:00",
    "processedAt": "2024-07-28T10:00:00",
    "estimatedCreditDays": "5-7 business days"
  }
}
```

---

## Error Codes Reference

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request fields failed validation |
| `MISSING_CARD_NUMBER` | 400 | Card number not provided for CARD mode |
| `INVALID_CARD_NUMBER` | 400 | Card number format invalid |
| `INVALID_CARD_EXPIRY` | 400 | Expiry not in MM/YY format |
| `INVALID_CVV` | 400 | CVV must be 3–4 digits |
| `MISSING_CARD_HOLDER_NAME` | 400 | Card holder name required |
| `MISSING_BANK_CODE` | 400 | Bank code required for NET_BANKING mode |
| `MISSING_UPI_ID` | 400 | UPI ID required for UPI mode |
| `INVALID_UPI_ID` | 400 | UPI ID not in `username@bank` format |
| `DUPLICATE_ORDER_ID` | 409 | Order ID already processed |
| `TRANSACTION_NOT_FOUND` | 404 | Transaction ID does not exist |
| `INVALID_TRANSACTION_STATE` | 400 | Transaction not in PENDING state for verify |
| `INVALID_TRANSACTION_STATE_FOR_REFUND` | 400 | Only SUCCESS/PARTIALLY_REFUNDED eligible for refund |
| `REFUND_AMOUNT_EXCEEDED` | 400 | Refund amount > remaining balance |
| `REFUND_NOT_FOUND` | 404 | Refund ID does not exist |
| `UNSUPPORTED_PAYMENT_MODE` | 400 | No provider registered for given mode |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |

---

## Pre-loaded Test Data

The following transactions are available immediately after startup (seeded by Flyway V2 migration).

### Card Transactions

| Transaction ID | Order ID | Amount | Status | Card | Scenario |
|---------------|----------|--------|--------|------|----------|
| `TXN-CARD-SUCCESS001` | ORD-2024-0001 | ₹1,500 | SUCCESS | VISA ****1111 | Normal success |
| `TXN-CARD-FAILED001` | ORD-2024-0002 | ₹2,999.50 | FAILED | MC ****4242 | Wrong OTP |
| `TXN-CARD-PENDING01` | ORD-2024-0003 | ₹750 | PENDING | RuPay ****6789 | Awaiting OTP |
| `TXN-CARD-REFUND001` | ORD-2024-0004 | ₹5,000 | REFUNDED | VISA ****9999 | Full refund issued |

### Net Banking Transactions

| Transaction ID | Order ID | Amount | Status | Bank | Scenario |
|---------------|----------|--------|--------|------|----------|
| `TXN-NB-SUCCESS001` | ORD-2024-0005 | ₹12,500 | SUCCESS | HDFC | Normal success |
| `TXN-NB-FAILED0001` | ORD-2024-0006 | ₹3,200 | FAILED | SBI | Bank timeout |
| `TXN-NB-PENDING001` | ORD-2024-0007 | ₹8,900 | PENDING | ICICI | Awaiting redirect |
| `TXN-NB-PARTIAL001` | ORD-2024-0008 | ₹6,000 | PARTIALLY_REFUNDED | Axis | ₹2,000 refunded |

### UPI Transactions

| Transaction ID | Order ID | Amount | Status | UPI ID | Scenario |
|---------------|----------|--------|--------|--------|----------|
| `TXN-UPI-SUCCESS01` | ORD-2024-0009 | ₹499 | SUCCESS | rajesh@okaxis | Axis Pay |
| `TXN-UPI-SUCCESS02` | ORD-2024-0010 | ₹2,345 | SUCCESS | priya@ybl | PhonePe |
| `TXN-UPI-FAILED001` | ORD-2024-0011 | ₹1,100 | FAILED | unknown@somebank | ID not registered |
| `TXN-UPI-REFUND001` | ORD-2024-0012 | ₹350 | REFUNDED | amit@gpay | Google Pay, full refund |

---

## Quick Test Scenarios

### Scenario 1 – Successful Card Payment

```bash
# Step 1: Initiate
curl -X POST http://localhost:8080/api/v1/payments/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "TEST-ORD-001",
    "merchantId": "MERCH-001",
    "amount": 1500.00,
    "currency": "INR",
    "paymentMode": "CARD",
    "customerName": "Test User",
    "customerEmail": "test@example.com",
    "customerPhone": "9876543210",
    "cardNumber": "4111111111111111",
    "cardExpiry": "12/26",
    "cvv": "123",
    "cardHolderName": "TEST USER"
  }'

# Step 2: Verify with OTP (use transactionId from Step 1)
curl -X POST http://localhost:8080/api/v1/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "<txnId-from-step-1>",
    "otp": "123456"
  }'
```

### Scenario 2 – UPI Payment (one step)

```bash
curl -X POST http://localhost:8080/api/v1/payments/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "TEST-ORD-002",
    "merchantId": "MERCH-001",
    "amount": 299.00,
    "currency": "INR",
    "paymentMode": "UPI",
    "customerName": "Test User",
    "customerEmail": "test@example.com",
    "upiId": "testuser@okicici"
  }'
```

### Scenario 3 – Partial Refund

```bash
curl -X POST http://localhost:8080/api/v1/refunds \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-NB-SUCCESS001",
    "amount": 5000.00,
    "reason": "Partial service not availed"
  }'
```

### Scenario 4 – Check Transaction Status

```bash
curl http://localhost:8080/api/v1/payments/TXN-UPI-SUCCESS01/status
```

---

## Running the Application

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL 14+

### Database Setup

```sql
CREATE DATABASE payment_gateway_db;
-- Default credentials: postgres / postgres
-- Update src/main/resources/application.yml if different
```

### Start the Application

```bash
cd payment-gateway
mvn spring-boot:run
```

Flyway will automatically run migrations on startup:
- `V1__create_tables.sql` – creates `transactions` and `refunds` tables
- `V2__insert_test_data.sql` – seeds 12 test transactions

### Access Swagger UI

Open `http://localhost:8080/swagger-ui.html` in your browser for an interactive API explorer.

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.3.2 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 14+ |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Documentation | SpringDoc OpenAPI 2.6 (Swagger UI) |
| Build | Maven 3.9 |
| Utilities | Lombok |
