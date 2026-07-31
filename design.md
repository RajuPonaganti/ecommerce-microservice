# Architecture Reference Document (ARD)
# Enterprise Ecommerce Platform — Java Spring Boot Microservices

> **Version**: 1.0 | **Status**: Approved | **Audience**: Principal Engineers, Staff Engineers, Platform Architects
> **Spring Boot**: 3.2.x | **Spring Cloud**: 2023.0.x | **Java**: 17 LTS

---

## Overview

This Architecture Reference Document (ARD) is the canonical design specification for an enterprise-grade ecommerce platform at Flipkart/Amazon scale, built on Java Spring Boot 3.x microservices with the Spring Cloud 2023.x ecosystem. It covers 20+ microservices across three tiers (Core, Extended Commerce, Enterprise), the full Spring Cloud toolchain (Eureka, Gateway, Config Server, Resilience4j, OpenFeign, Sleuth/Zipkin), 15 architectural patterns with trade-off analysis and Java code examples, performance benchmarks, observability standards, security architecture, and deployment topology.

**Target Audience**: Principal Engineers (L6+), Staff Engineers, and Platform/Solution Architects with production microservices experience. Prerequisites: Java 17+, Spring Boot 3.x, Kafka, PostgreSQL, Redis, Kubernetes.

---

## Architecture

The platform follows a layered architecture: Client Tier → CDN → API Gateway (Spring Cloud Gateway) → Service Mesh (Istio/Envoy) → Microservices Layer → Data Stores + Message Broker + Cache. All services are independently deployable Spring Boot applications, registered with Eureka, configured via Spring Cloud Config Server, instrumented with Micrometer/Sleuth, and resilience-wrapped with Resilience4j.

**12 mandatory architecture principles** (P-01 through P-12): Loose Coupling, High Cohesion, API-First, Fail-Fast, Design for Failure, 12-Factor App, Single Responsibility, Async-First, Idempotency by Default, Observability-First, Security-in-Depth, Infrastructure-as-Code.

---

## Components and Interfaces

The platform comprises **21 microservices** across three tiers:

- **Core Services** (6): User, Product, Inventory, Order, Payment, Notification
- **Extended Commerce Services** (8): Seller/Vendor, Cart, Search, Reviews & Ratings, Recommendations, Shipping & Logistics, Returns & Refunds, Promotions & Coupons
- **Enterprise/Platform Services** (7): API Gateway, Auth/IAM, Fraud Detection, Reporting & Analytics, Admin Panel, Config Server, Service Registry (Eureka)

Inter-service communication: synchronous REST/gRPC for latency-critical paths (fraud scoring, stock reservation); asynchronous Kafka events for state propagation (order lifecycle, payment confirmation, notifications). All synchronous calls are guarded by Resilience4j Circuit Breaker + Retry + Bulkhead + TimeLimiter via OpenFeign declarative clients.

---

## Data Models

Each service owns its database exclusively (Database-per-Service mandate, enforced via Kubernetes NetworkPolicy). Key data stores by service:

- **PostgreSQL 15** (ACID, CP): User, Order, Payment, Inventory, Returns, Promotions, Auth/IAM, Seller, Admin
- **PostgreSQL + Elasticsearch** (CQRS): Product (write: PG, read: ES), Reviews (write: PG, read: ES)
- **Redis** (AP, sub-10ms): Cart (primary store), Recommendations (pre-computed), distributed locks (Redisson)
- **Elasticsearch** (AP, full-text): Search (primary store)

All inter-service data sharing is strictly via API calls or Kafka events. Direct database-to-database joins are prohibited. Event schemas use Avro with Confluent Schema Registry for backward-compatible evolution.

---

## Table of Contents

1. [Architecture Overview](#section-1-architecture-overview)
2. [Core Microservices](#section-2-core-microservices)
3. [Extended Commerce Microservices](#section-3-extended-commerce-microservices)
4. [Enterprise Cross-Cutting Services](#section-4-enterprise-cross-cutting-services)
5. [Spring Cloud Ecosystem — Deep Dive](#section-5-spring-cloud-ecosystem-deep-dive)
6. [Architectural Patterns Catalogue](#section-6-architectural-patterns-catalogue)
7. [Java Spring Boot Code Snippets](#section-7-java-spring-boot-code-snippets)
8. [Performance Tuning & Scalability](#section-8-performance-tuning--scalability)
9. [Observability & SLA Governance](#section-9-observability--sla-governance)
10. [Data Architecture & Consistency](#section-10-data-architecture--consistency)
11. [Security Architecture](#section-11-security-architecture)
12. [Deployment Topology & DevOps](#section-12-deployment-topology--devops)
13. [Correctness Properties](#correctness-properties)

---

# Section 1: Architecture Overview

## 1.1 Target Audience and Prerequisites

**Target Audience**: Principal Engineers (L6+), Staff Engineers, and Platform/Solution Architects responsible for designing, building, and governing a Flipkart/Amazon-scale ecommerce platform.

**Prerequisites**:
- Deep proficiency in Java 17+ (records, sealed classes, virtual threads in Java 21) and Spring Boot 3.x
- Working knowledge of Spring Cloud Netflix OSS, Spring Security, Spring Data, and Spring Batch
- Hands-on experience with Kafka, Redis, PostgreSQL, and Elasticsearch in production
- Familiarity with Kubernetes operators, Helm charts, and GitOps (ArgoCD/Flux)
- Understanding of distributed systems fundamentals: CAP theorem, two-phase commit, consensus protocols
- Practical exposure to APM tooling (Prometheus/Grafana, Zipkin/Jaeger, ELK stack)

This document assumes you have shipped microservices to production and are solving operational problems at scale, not learning fundamentals. Explanations are at the "why and what" level, not the "how to set up Java" level.

---

## 1.2 Architecture Principles

The following numbered principles are **mandatory constraints** that all per-service designs must satisfy. Violation requires an explicit ADR (Architecture Decision Record) with sign-off from the Principal Engineer guild.

| # | Principle | Mandate |
|---|-----------|---------|
| P-01 | **Loose Coupling** | Services communicate via API contracts or events only. No shared libraries containing domain logic. No shared databases. |
| P-02 | **High Cohesion** | A service owns exactly one bounded context. Business capabilities that change together live together. |
| P-03 | **API-First** | OpenAPI 3.1 spec is the source of truth, not the implementation. Contract tests enforce compliance. |
| P-04 | **Fail-Fast** | Validate inputs at the boundary (controller layer). Reject invalid requests immediately with RFC 7807 Problem Details. |
| P-05 | **Design for Failure** | Every inter-service call is guarded by a circuit breaker and timeout. No call is made without a fallback strategy. |
| P-06 | **12-Factor App** | Config externalised, stateless processes, dev/prod parity, logs as streams. No local disk state that isn't ephemeral. |
| P-07 | **Single Responsibility** | Each service has one reason to change: a change in business domain, not a change in infrastructure. |
| P-08 | **Async-First** | Default to event-driven communication for state-changing operations. Synchronous calls limited to read queries and latency-critical paths (payment auth, fraud check). |
| P-09 | **Idempotency by Default** | All write endpoints accept an idempotency key. All Kafka consumers deduplicate. All compensating transactions are re-entrant. |
| P-10 | **Observability-First** | No service ships without metrics (Micrometer), structured logs (Logback JSON), and distributed traces (Sleuth). SLOs are defined at design time. |
| P-11 | **Security-in-Depth** | mTLS between services, JWT at API Gateway boundary, RBAC at method level, PII pseudonymisation, secrets in Vault. |
| P-12 | **Infrastructure-as-Code** | All Kubernetes manifests, Helm charts, Terraform modules are versioned in Git. No click-ops in production. |


## 1.3 Service Catalogue

| Service | Domain | Bounded Context | Primary Responsibility | Tech Stack | CAP | SLA (Avail.) | Peak RPS |
|---------|--------|-----------------|----------------------|------------|-----|--------------|----------|
| **User** | Identity | User Lifecycle & Auth | Account management, profile, address book | Spring Boot, PostgreSQL, Redis | CP | 99.99% | 20,000 |
| **Product** | Catalogue | Product Information Mgmt | Product listings, attributes, pricing | Spring Boot, PostgreSQL, Elasticsearch | AP | 99.95% | 50,000 |
| **Inventory** | Fulfilment | Stock Reservation | Stock levels, reservations, warehouse allocation | Spring Boot, PostgreSQL (row-lock) | CP | 99.99% | 15,000 |
| **Order** | Commerce | Order Lifecycle | Order creation, Saga orchestration, state tracking | Spring Boot, PostgreSQL, Kafka | CP | 99.99% | 5,000 |
| **Payment** | Finance | Payment Processing | Charge, refund, idempotency, PCI compliance | Spring Boot, PostgreSQL, Vault | CP | 99.999% | 2,000 |
| **Notification** | Engagement | Multi-Channel Messaging | Email/SMS/push dispatch, templating, DLQ | Spring Boot, Kafka, SendGrid/SNS | AP | 99.9% | 30,000 |
| **Seller/Vendor** | Marketplace | Seller Onboarding & Mgmt | Seller registration, catalogue management, payouts | Spring Boot, PostgreSQL | CP | 99.95% | 3,000 |
| **Cart** | Commerce | Shopping Session | Session cart, persistent cart, merge-on-login | Spring Boot, Redis, PostgreSQL | AP | 99.95% | 40,000 |
| **Search** | Discovery | Product Discovery | Full-text & faceted search, autocomplete | Spring Boot, Elasticsearch | AP | 99.95% | 30,000 |
| **Reviews & Ratings** | Trust | User-Generated Content | Review submission, moderation, aggregated scores | Spring Boot, PostgreSQL, Elasticsearch | AP | 99.9% | 10,000 |
| **Recommendations** | Personalisation | Product Recommendations | Personalised & trending feeds, A/B model routing | Spring Boot, Redis, ML model serving | AP | 99.9% | 25,000 |
| **Shipping & Logistics** | Fulfilment | Carrier Integration | Shipment creation, tracking, carrier webhook ingestion | Spring Boot, PostgreSQL, Kafka | AP | 99.95% | 5,000 |
| **Returns & Refunds** | Post-Purchase | Return Lifecycle | Return requests, inspection states, refund triggers | Spring Boot, PostgreSQL, Kafka | CP | 99.95% | 2,000 |
| **Promotions & Coupons** | Marketing | Discount Engine | Coupon validation, stacking rules, usage tracking | Spring Boot, Redis (Redisson), PostgreSQL | CP | 99.99% | 20,000 |
| **API Gateway** | Platform | Ingress & Routing | Rate limiting, JWT validation, routing, SSL termination | Spring Cloud Gateway | AP | 99.99% | 100,000 |
| **Auth/IAM** | Platform | Identity & Access | OAuth 2.0 / OIDC, token issuance, RBAC | Spring Auth Server, PostgreSQL | CP | 99.999% | 10,000 |
| **Fraud Detection** | Risk | Fraud Risk Scoring | Real-time ML scoring, rule engine, manual review | Spring Boot, Python model (gRPC), Kafka | AP | 99.99% | 2,000 |
| **Reporting & Analytics** | Intelligence | Business Analytics | Lambda architecture, CQRS read models, dashboards | Spring Batch, Kafka Streams, Spark, Elasticsearch | AP | 99.9% | 5,000 |
| **Admin Panel** | Operations | Platform Administration | RBAC admin operations, audit logging, config overrides | Spring Boot, PostgreSQL | CP | 99.9% | 500 |
| **Config Server** | Platform | Configuration Management | Externalised config, hot-reload, secret overlay | Spring Cloud Config, Vault | CP | 99.99% | 5,000 |
| **Service Registry** | Platform | Service Discovery | Dynamic service location, health tracking | Eureka / Spring Cloud Registry | AP | 99.99% | N/A |


## 1.4 System Context Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT TIER                                        │
│   Mobile App (iOS/Android)    Web SPA (React)    Seller Portal    Admin UI  │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ HTTPS (TLS 1.3)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CDN TIER (Cloudflare / AWS CloudFront)             │
│   Static Assets  │  Edge Cache (Product/Search)  │  DDoS Shield            │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Spring Cloud Gateway)                       │
│  JWT Validation │ Rate Limiting │ SSL Termination │ Request Routing         │
│  Canary Routing │ Correlation ID Injection │ Response Transform             │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ mTLS (internal)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SERVICE MESH (Istio / Envoy Sidecar)                     │
│  Circuit Breaking │ mTLS │ Traffic Shaping │ Observability │ Service RBAC   │
└────────┬───────────┬────────────┬───────────────┬───────────────┬───────────┘
         │           │            │               │               │
         ▼           ▼            ▼               ▼               ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        MICROSERVICES LAYER                                   │
│                                                                              │
│  ┌──────────┐ ┌─────────┐ ┌───────────┐ ┌────────┐ ┌─────────┐ ┌────────┐ │
│  │  User    │ │ Product │ │ Inventory │ │ Order  │ │ Payment │ │ Notify │ │
│  │ Service  │ │ Service │ │  Service  │ │Service │ │ Service │ │ Service│ │
│  └──────────┘ └─────────┘ └───────────┘ └────────┘ └─────────┘ └────────┘ │
│                                                                              │
│  ┌──────┐ ┌──────┐ ┌────────┐ ┌─────────┐ ┌───────┐ ┌────────┐ ┌───────┐  │
│  │ Cart │ │Search│ │Reviews │ │Recommend│ │Seller │ │Shipping│ │Return │  │
│  │      │ │      │ │       │ │  ations │ │       │ │        │ │       │  │
│  └──────┘ └──────┘ └────────┘ └─────────┘ └───────┘ └────────┘ └───────┘  │
│                                                                              │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │Promotions│ │  Fraud   │ │Auth/IAM │ │ Admin    │ │  Reporting &     │  │
│  │& Coupons │ │Detection │ │ Service │ │  Panel   │ │  Analytics       │  │
│  └──────────┘ └──────────┘ └─────────┘ └──────────┘ └──────────────────┘  │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼──────────────────────────┐
          ▼                         ▼                          ▼
┌──────────────────┐   ┌─────────────────────┐   ┌──────────────────────────┐
│   DATA STORES    │   │   MESSAGE BROKER     │   │    CACHE LAYER           │
│                  │   │                      │   │                          │
│ PostgreSQL (CP)  │   │  Apache Kafka        │   │  Redis Cluster           │
│ MongoDB (AP)     │   │  (3 brokers, 3 ZK)   │   │  (6 shards, 1 replica)   │
│ Elasticsearch    │   │  MirrorMaker 2       │   │  Session / Cart / Lock   │
│ Cassandra (AP)   │   │  (cross-region)      │   │                          │
└──────────────────┘   └─────────────────────┘   └──────────────────────────┘
```


## 1.5 Inter-Service Communication Matrix

| From \ To | User | Product | Inventory | Order | Payment | Notification | Cart | Search | Recommendations | Promotions | Shipping | Returns | Fraud | Auth/IAM |
|-----------|------|---------|-----------|-------|---------|--------------|------|--------|-----------------|------------|----------|---------|-------|---------|
| **Order** | REST(read) | REST(read) | REST(reserve) | — | REST(charge) | Kafka(event) | — | — | — | REST(validate) | REST(create) | — | REST(score) | — |
| **Payment** | — | — | — | Kafka(event) | — | Kafka(event) | — | — | — | — | — | Kafka(event) | — | — |
| **Cart** | REST(read) | REST(read) | REST(check) | REST(create) | — | — | — | — | REST(recs) | REST(validate) | — | — | — | — |
| **Inventory** | — | — | — | Kafka(event) | — | Kafka(event) | — | Kafka(reindex) | — | — | Kafka(event) | — | — | — |
| **Returns** | — | — | REST(restore) | — | REST(refund) | Kafka(event) | — | — | — | — | REST(pickup) | — | — | — |
| **API Gateway** | REST(auth) | — | — | — | — | — | — | — | — | — | — | — | — | REST(validate) |
| **Search** | — | Kafka(consume) | Kafka(consume) | — | — | — | — | — | — | — | — | — | — | — |
| **Notification** | REST(read) | — | — | — | — | — | — | — | — | — | — | — | — | — |

**Legend**: REST = synchronous HTTP/gRPC | Kafka = asynchronous event | (read) = GET only | (event) = published domain event

---

# Section 2: Core Microservices

## 2.1 User Service

**Bounded Context**: User Lifecycle & Identity — owns all aspects of consumer identity including registration, authentication (delegated to Auth/IAM), profile management, address book, and account state transitions. Does NOT own sessions (Auth/IAM) or purchase history (Order).

### Domain Model

```
User
 ├── userId (UUID, PK)
 ├── email (unique, indexed)
 ├── phoneNumber (unique, E.164 format)
 ├── status (PENDING_VERIFICATION | ACTIVE | SUSPENDED | DELETED)
 ├── credentialHash (Argon2id, stored separately in credentials table)
 ├── mfaEnabled (boolean)
 ├── createdAt, updatedAt, deletedAt (soft delete)
 └── addresses[] → Address (userId FK, label, street, city, pincode, isDefault)

AccountEvent (event store for FSM replay)
 ├── eventId (UUID)
 ├── userId (FK)
 ├── eventType (REGISTERED | VERIFIED | PASSWORD_CHANGED | SUSPENDED | REACTIVATED | DELETED)
 ├── actor (system | user | admin)
 └── occurredAt (timestamp)
```

### Account Lifecycle FSM

```
PENDING_VERIFICATION ──[email_verified]──► ACTIVE
ACTIVE ──[admin_suspend]──► SUSPENDED
ACTIVE ──[user_delete_request]──► DELETION_PENDING (GDPR 30-day grace)
SUSPENDED ──[admin_reactivate]──► ACTIVE
DELETION_PENDING ──[grace_period_expired]──► DELETED (pseudonymised)
DELETED is terminal
```

### API Contract

| Method | Path | Request | Response | Auth |
|--------|------|---------|----------|------|
| POST | `/v1/users/register` | `{email, phone, password}` | `201 UserDto` | None |
| GET | `/v1/users/{userId}` | — | `200 UserDto` | JWT (self or ADMIN) |
| PATCH | `/v1/users/{userId}` | `{displayName, phone}` | `200 UserDto` | JWT (self) |
| DELETE | `/v1/users/{userId}` | — | `202 Accepted` | JWT (self or ADMIN) |
| POST | `/v1/users/{userId}/addresses` | `AddressDto` | `201 AddressDto` | JWT (self) |
| GET | `/v1/users/{userId}/addresses` | — | `200 AddressDto[]` | JWT (self) |

### Data Schema

```sql
CREATE TABLE users (
    user_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(320) NOT NULL UNIQUE,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    mfa_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE user_credentials (
    user_id       UUID PRIMARY KEY REFERENCES users(user_id),
    credential_hash VARCHAR(255) NOT NULL, -- Argon2id, never bcrypt for new accounts
    hash_version  SMALLINT     NOT NULL DEFAULT 2,
    last_rotated  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_addresses (
    address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(user_id),
    label      VARCHAR(50),
    street     TEXT        NOT NULL,
    city       VARCHAR(100) NOT NULL,
    pincode    VARCHAR(10)  NOT NULL,
    is_default BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Events Published

| Event | Topic | Payload Summary |
|-------|-------|-----------------|
| `UserRegistered` | `user.events` | `{userId, email, status: PENDING_VERIFICATION}` |
| `UserVerified` | `user.events` | `{userId, verifiedAt}` |
| `UserDeleted` | `user.events` | `{userId, pseudonymId, deletedAt}` |
| `UserSuspended` | `user.events` | `{userId, reason, actor}` |

### Events Consumed

| Event | Source | Action |
|-------|--------|--------|
| `OrderPlaced` | Order | Update `last_order_at` in user profile cache |
| `RefundCompleted` | Returns | Update wallet balance (if applicable) |

### CAP Classification: **CP**
User accounts require strong consistency — a suspended account must be immediately unreachable, a deleted account must immediately cease to exist. Split-brain on account state is a security risk. PostgreSQL with synchronous replication + write to primary only.

### SLA Targets

| Availability | P50 Read | P95 Read | P99 Read | P99 Write | Peak RPS | Write RPS |
|-------------|----------|----------|----------|-----------|---------|-----------|
| 99.99% | 5 ms | 20 ms | 50 ms | 150 ms | 20,000 | 2,000 |

### Key Design Decisions

- **Argon2id over bcrypt**: Argon2id is the 2015 Password Hashing Competition winner. Memory-hard, resistant to GPU/ASIC cracking. bcrypt is acceptable for existing hashes; migrate on next login.
- **Separate credentials table**: Allows credential rotation without touching the main user row. Enables credential history for password reuse enforcement.
- **Soft delete + pseudonymisation**: GDPR Article 17 compliance. `deletedAt` starts the 30-day grace period. At expiry, PII fields are replaced with deterministic pseudonyms derived from a per-user HMAC key stored in the erasure vault.
- **Auth/IAM boundary**: User Service owns identity data; Auth/IAM owns token issuance, session state, and OAuth flows. This is not a grey area — User Service has no token logic.


---

## 2.2 Product Service

**Bounded Context**: Product Information Management — owns product catalogue data (attributes, images, pricing), category taxonomy, and seller-submitted product listings. Read volume far exceeds writes; uses CQRS with PostgreSQL (write) and Elasticsearch (read).

### Domain Model

```
Product
 ├── productId (UUID)
 ├── sellerId (FK to Seller Service — no join, API reference only)
 ├── sku (unique per seller)
 ├── title, description, brand
 ├── categoryPath (e.g., Electronics/Mobiles/Smartphones)
 ├── attributes (JSONB — flexible schema per category)
 ├── basePrice, mrp, currency
 ├── status (DRAFT | PENDING_REVIEW | ACTIVE | DISCONTINUED)
 └── images[] (CDN URLs)

ProductPriceHistory (append-only, audit trail)
 ├── productId, price, effectiveFrom, changedBy
```

### API Contract

| Method | Path | Request | Response | Auth |
|--------|------|---------|----------|------|
| POST | `/v1/products` | `ProductCreateDto` | `201 ProductDto` | JWT (SELLER) |
| GET | `/v1/products/{productId}` | — | `200 ProductDto` | None |
| PUT | `/v1/products/{productId}` | `ProductUpdateDto` | `200 ProductDto` | JWT (SELLER, owns product) |
| GET | `/v1/products?category=&brand=&page=` | — | `200 Page<ProductDto>` | None |
| POST | `/v1/products/{productId}/publish` | — | `200 ProductDto` | JWT (SELLER) |

### Data Schema

```sql
-- Write store (PostgreSQL)
CREATE TABLE products (
    product_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id    UUID NOT NULL,  -- API reference, no FK constraint across services
    sku          VARCHAR(100) NOT NULL,
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    category_path VARCHAR(500) NOT NULL,
    attributes   JSONB,
    base_price   NUMERIC(12,2) NOT NULL,
    mrp          NUMERIC(12,2),
    currency     CHAR(3) NOT NULL DEFAULT 'INR',
    status       VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    version      BIGINT NOT NULL DEFAULT 0,  -- Optimistic lock
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (seller_id, sku)
);
```

```json
// Read store (Elasticsearch index: products_v3)
{
  "mappings": {
    "properties": {
      "productId":      { "type": "keyword" },
      "title":          { "type": "text", "analyzer": "custom_en_hi" },
      "brand":          { "type": "keyword" },
      "categoryPath":   { "type": "keyword" },
      "basePrice":      { "type": "scaled_float", "scaling_factor": 100 },
      "attributes":     { "type": "flattened" },
      "status":         { "type": "keyword" },
      "avgRating":      { "type": "half_float" },
      "reviewCount":    { "type": "integer" },
      "stockAvailable": { "type": "boolean" },
      "updatedAt":      { "type": "date" }
    }
  }
}
```

### Events Published

| Event | Topic | Payload |
|-------|-------|---------|
| `ProductCreated` | `product.events` | `{productId, sellerId, sku, title, categoryPath, status}` |
| `ProductUpdated` | `product.events` | `{productId, changedFields, version}` |
| `ProductPublished` | `product.events` | `{productId, effectiveAt}` |
| `ProductDiscontinued` | `product.events` | `{productId, reason}` |

### Events Consumed

| Event | Source | Action |
|-------|--------|--------|
| `InventoryUpdated` | Inventory | Update `stockAvailable` in Elasticsearch projection |
| `ReviewAggregated` | Reviews | Update `avgRating`, `reviewCount` in Elasticsearch projection |

### CAP Classification: **AP**
Product catalogue reads tolerate stale data (seconds to minutes). The Elasticsearch read replica may lag behind the PostgreSQL write store. This is acceptable — a product listing showing an outdated price for 5 seconds is operationally fine. Inventory availability is sourced from Inventory Service, not Product Service.

### SLA Targets

| Availability | P50 Read | P95 Read | P99 Read | P99 Write | Peak RPS | Write RPS |
|-------------|----------|----------|----------|-----------|---------|-----------|
| 99.95% | 8 ms | 30 ms | 80 ms | 200 ms | 50,000 | 3,000 |

---

## 2.3 Inventory Service

**Bounded Context**: Stock Reservation — owns stock levels, warehouse locations, and stock reservations. The critical invariant: available stock ≥ 0 at all times. Under concurrent load (Black Friday flash sale), this is the hardest invariant to maintain.

### Domain Model

```
StockItem
 ├── stockItemId (UUID)
 ├── productId (API reference)
 ├── warehouseId
 ├── quantityOnHand (total physical stock)
 ├── quantityReserved (sum of active reservations)
 ├── quantityAvailable = quantityOnHand - quantityReserved
 └── version (optimistic lock counter — @Version in JPA)

StockReservation
 ├── reservationId (UUID)
 ├── productId, warehouseId
 ├── quantity
 ├── orderId (FK reference)
 ├── status (RESERVED | CONFIRMED | CANCELLED | EXPIRED)
 └── expiresAt (NOW() + 15 minutes TTL)
```

### Optimistic Locking Under Concurrent Checkout

```java
@Entity
@Table(name = "stock_items")
public class StockItem {
    @Version
    private Long version; // JPA increments on every UPDATE; concurrent update throws OptimisticLockException

    public void reserve(int quantity) {
        if (this.quantityAvailable() < quantity) {
            throw new InsufficientStockException(productId, quantity, quantityAvailable());
        }
        this.quantityReserved += quantity;
        // version bump on flush prevents a concurrent thread from committing a stale view
    }
}
```

At 5,000 concurrent checkouts for the same SKU, most will fail with `OptimisticLockException`. The application layer retries with exponential backoff (3 attempts, 50–200ms). For flash sales, a Redis-based decrement counter provides a pre-filter before hitting PostgreSQL:

```java
// Redis pre-check: atomically decrement stock counter
Long remaining = redisTemplate.execute(stockDecrementScript,
    Collections.singletonList("stock:" + productId),
    String.valueOf(quantity));
if (remaining < 0) {
    redisTemplate.execute(stockIncrementScript,
        Collections.singletonList("stock:" + productId),
        String.valueOf(quantity)); // compensate
    throw new StockExhaustedException(productId);
}
// If Redis check passes, proceed to PostgreSQL reservation (authoritative)
```

### Stock Reservation TTL

Reservations expire after 15 minutes if not confirmed by a completed payment. A scheduled job runs every 60 seconds to cancel expired reservations and restore stock:

```sql
UPDATE stock_reservations
SET status = 'EXPIRED'
WHERE status = 'RESERVED' AND expires_at < NOW()
RETURNING reservation_id, product_id, warehouse_id, quantity;
-- Application layer then executes UPDATE stock_items SET quantity_reserved -= :qty
```

### Events Published

| Event | Topic | Payload |
|-------|-------|---------|
| `StockReserved` | `inventory.events` | `{reservationId, productId, quantity, orderId, expiresAt}` |
| `StockConfirmed` | `inventory.events` | `{reservationId, orderId}` |
| `StockReleased` | `inventory.events` | `{reservationId, reason: EXPIRED|CANCELLED}` |
| `StockReplenished` | `inventory.events` | `{productId, warehouseId, addedQuantity, newTotal}` |

### Events Consumed

| Event | Source | Action |
|-------|--------|--------|
| `OrderCancelled` | Order | Release reservation |
| `PaymentCompleted` | Payment | Confirm reservation |
| `ReturnApproved` | Returns | Restore stock |

### CAP Classification: **CP**
Strong consistency is non-negotiable. Overselling is a P0 incident — it destroys seller trust, triggers compensating returns, and violates SLAs. PostgreSQL serialisable isolation + optimistic locking + Redis pre-filter.

### SLA Targets

| Availability | P50 Read | P95 Read | P99 Read | P99 Write | Peak RPS | Write RPS |
|-------------|----------|----------|----------|-----------|---------|-----------|
| 99.99% | 3 ms | 15 ms | 50 ms | 150 ms | 15,000 | 5,000 |


---

## 2.4 Order Service

**Bounded Context**: Order Lifecycle — owns the entire lifecycle of a customer order from creation through fulfilment or cancellation. Coordinates the multi-service Saga. The most complex service in the platform: it is the saga orchestrator AND produces the most domain events.

### Domain Model

```
Order
 ├── orderId (UUID)
 ├── userId (API reference)
 ├── items[] → OrderItem (productId, sku, quantity, unitPrice, sellerId)
 ├── shippingAddressId
 ├── status (CREATED | PAYMENT_PENDING | PAYMENT_CONFIRMED | INVENTORY_RESERVED |
 │           PROCESSING | SHIPPED | DELIVERED | CANCELLED | REFUND_INITIATED)
 ├── totalAmount, taxAmount, discountAmount, finalAmount
 ├── couponCode (optional, validated at creation)
 ├── paymentRef (idempotency key for Payment Service)
 ├── sagaState (Spring State Machine serialised state)
 └── version (optimistic lock)
```

### Saga Pattern — Choreography Variant (Kafka Events)

In the choreography variant, each service reacts to events and publishes its own. No central coordinator.

```
Order Service      Inventory Service    Payment Service    Notification Service
     │                    │                   │                    │
     │──OrderCreated──────►│                  │                    │
     │                    │──StockReserved────►│                   │
     │                    │                   │──PaymentCompleted──►│
     │                    │                   │                    │──email/SMS─►User
     │◄──StockReserved────│                   │                    │
     │◄──────────────────────PaymentCompleted──│                   │
     │──OrderConfirmed────►(all services)      │                   │
```

**Compensating flow (payment fails after stock reserved)**:
```
Payment Service publishes PaymentFailed
   → Inventory Service listens → releases reservation
   → Order Service listens → marks CANCELLED
   → Notification Service listens → sends cancellation email
```

Choreography works well for linear flows but becomes complex with >4 services. The platform uses orchestration for the primary happy path.

### Saga Pattern — Orchestration Variant (Spring State Machine)

```java
package com.platform.order.saga;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;

@Component
public class OrderSagaOrchestrator {

    // States match Order.status enum
    public enum OrderState {
        CREATED, INVENTORY_RESERVING, INVENTORY_RESERVED,
        PAYMENT_PROCESSING, PAYMENT_CONFIRMED,
        FULFILMENT_STARTED, COMPLETED,
        INVENTORY_RESERVE_FAILED, PAYMENT_FAILED, CANCELLED
    }

    public enum OrderEvent {
        RESERVE_INVENTORY, INVENTORY_RESERVED, INVENTORY_FAILED,
        PROCESS_PAYMENT, PAYMENT_CONFIRMED, PAYMENT_FAILED,
        START_FULFILMENT, FULFILMENT_COMPLETE, CANCEL
    }

    @Bean
    public StateMachine<OrderState, OrderEvent> orderStateMachine() throws Exception {
        StateMachineBuilder.Builder<OrderState, OrderEvent> builder =
            StateMachineBuilder.builder();

        builder.configureStates()
            .withStates()
            .initial(OrderState.CREATED)
            .end(OrderState.COMPLETED)
            .end(OrderState.CANCELLED)
            .states(EnumSet.allOf(OrderState.class));

        builder.configureTransitions()
            .withExternal()
                .source(OrderState.CREATED).target(OrderState.INVENTORY_RESERVING)
                .event(OrderEvent.RESERVE_INVENTORY)
                .action(inventoryReserveAction())       // calls Inventory Service
                .and()
            .withExternal()
                .source(OrderState.INVENTORY_RESERVING).target(OrderState.INVENTORY_RESERVED)
                .event(OrderEvent.INVENTORY_RESERVED)
                .and()
            .withExternal()
                .source(OrderState.INVENTORY_RESERVING).target(OrderState.INVENTORY_RESERVE_FAILED)
                .event(OrderEvent.INVENTORY_FAILED)
                .action(cancelOrderAction())            // compensating transaction
                .and()
            .withExternal()
                .source(OrderState.INVENTORY_RESERVED).target(OrderState.PAYMENT_PROCESSING)
                .event(OrderEvent.PROCESS_PAYMENT)
                .action(paymentChargeAction())          // calls Payment Service
                .and()
            .withExternal()
                .source(OrderState.PAYMENT_PROCESSING).target(OrderState.PAYMENT_CONFIRMED)
                .event(OrderEvent.PAYMENT_CONFIRMED)
                .and()
            .withExternal()
                .source(OrderState.PAYMENT_PROCESSING).target(OrderState.PAYMENT_FAILED)
                .event(OrderEvent.PAYMENT_FAILED)
                .action(releaseInventoryAction())       // compensating: release stock
                .and()
            .withExternal()
                .source(OrderState.PAYMENT_CONFIRMED).target(OrderState.COMPLETED)
                .event(OrderEvent.FULFILMENT_COMPLETE);

        return builder.build();
    }

    @Bean
    public Action<OrderState, OrderEvent> inventoryReserveAction() {
        return ctx -> {
            UUID orderId = ctx.getExtendedState().get("orderId", UUID.class);
            try {
                inventoryClient.reserve(orderId); // OpenFeign call
                ctx.getStateMachine().sendEvent(OrderEvent.INVENTORY_RESERVED);
            } catch (Exception e) {
                ctx.getStateMachine().sendEvent(OrderEvent.INVENTORY_FAILED);
            }
        };
    }
}
```

### Events Published

| Event | Topic | Payload |
|-------|-------|---------|
| `OrderCreated` | `order.events` | `{orderId, userId, items[], totalAmount, couponCode}` |
| `OrderCancelled` | `order.events` | `{orderId, reason, cancelledAt}` |
| `OrderConfirmed` | `order.events` | `{orderId, paymentRef, confirmedAt}` |
| `OrderShipped` | `order.events` | `{orderId, shipmentId, trackingNumber}` |
| `OrderDelivered` | `order.events` | `{orderId, deliveredAt}` |

### Events Consumed

| Event | Source | Action |
|-------|--------|--------|
| `StockReserved` | Inventory | Advance saga state |
| `StockReserveFailed` | Inventory | Trigger compensation |
| `PaymentCompleted` | Payment | Advance saga state |
| `PaymentFailed` | Payment | Trigger compensation |

### CAP Classification: **CP**
Order integrity requires strong consistency. Double-orders (same cart checked out twice) or orders without payment are P0. PostgreSQL with read-after-write on primary. Saga state machine persisted to DB on each transition.

### SLA Targets

| Availability | P50 Read | P95 Read | P99 Read | P99 Write | Peak RPS | Write RPS |
|-------------|----------|----------|----------|-----------|---------|-----------|
| 99.99% | 10 ms | 40 ms | 100 ms | 300 ms | 5,000 | 5,000 |


---

## 2.5 Payment Service

**Bounded Context**: Payment Processing — owns charge initiation, settlement tracking, refund processing, and PCI-DSS compliance boundary. The strictest SLA in the platform (99.999%). Every operation is idempotent, every event is delivered via the Outbox Pattern.

### Idempotency Key Design

Every write endpoint requires `Idempotency-Key: <uuid>` header. The service deduplicates using a `payment_requests` table:

```java
@PostMapping("/v1/payments/charge")
public ResponseEntity<PaymentResponseDto> charge(
        @RequestHeader("Idempotency-Key") UUID idempotencyKey,
        @RequestHeader("Authorization") String jwt,
        @Valid @RequestBody ChargeRequestDto req) {

    // Check idempotency cache (Redis L1, then DB L2)
    Optional<PaymentResponse> existing = idempotencyService.find(idempotencyKey);
    if (existing.isPresent()) {
        return ResponseEntity.ok(existing.get().toDto()); // replay cached response
    }

    // Execute payment with distributed lock to prevent concurrent same-key processing
    return redissonClient.getLock("payment-lock:" + idempotencyKey)
        .tryLock(5, 30, TimeUnit.SECONDS)
        ? executeCharge(idempotencyKey, req)
        : ResponseEntity.status(HttpStatus.CONFLICT).build();
}
```

### Outbox Pattern Implementation

```java
@Entity
@Table(name = "payment_outbox")
public class PaymentOutboxEvent {
    @Id
    private UUID eventId;
    private String aggregateType;   // "Payment"
    private UUID aggregateId;       // paymentId
    private String eventType;       // "PaymentCompleted"
    private String payload;         // JSON
    private String status;          // PENDING | PUBLISHED | FAILED
    private int retryCount;
    private Instant createdAt;
    private Instant publishedAt;
}

@Service
@Transactional // Both the payment and the outbox event commit atomically
public class PaymentService {

    public PaymentResponse charge(ChargeRequest req) {
        Payment payment = processPaymentWithProvider(req); // calls external PSP
        paymentRepository.save(payment);

        // Outbox event in same transaction — either both commit or both rollback
        PaymentOutboxEvent outboxEvent = PaymentOutboxEvent.builder()
            .eventId(UUID.randomUUID())
            .aggregateType("Payment")
            .aggregateId(payment.getPaymentId())
            .eventType("PaymentCompleted")
            .payload(objectMapper.writeValueAsString(new PaymentCompletedEvent(payment)))
            .status("PENDING")
            .createdAt(Instant.now())
            .build();
        outboxRepository.save(outboxEvent);

        return payment.toResponse();
    }
}
```

A Debezium CDC connector monitors `payment_outbox` and publishes `PENDING` rows to `payment.events` Kafka topic, then marks them `PUBLISHED`. This decouples the payment transaction from Kafka availability.

### Compensating Transactions

| Step | Forward | Compensating |
|------|---------|-------------|
| 1 | Reserve stock | Release stock |
| 2 | Charge payment | Refund payment |
| 3 | Create shipment | Cancel shipment |
| 4 | Update order status | Revert order status |

Compensation is triggered by `PaymentFailed` or `FraudRejected` events. Each compensating action is itself idempotent.

### Events Published

| Event | Topic | Payload |
|-------|-------|---------|
| `PaymentInitiated` | `payment.events` | `{paymentId, orderId, amount, currency, idempotencyKey}` |
| `PaymentCompleted` | `payment.events` | `{paymentId, orderId, amount, settledAt, transactionRef}` |
| `PaymentFailed` | `payment.events` | `{paymentId, orderId, failureCode, failureReason}` |
| `RefundInitiated` | `payment.events` | `{refundId, paymentId, amount, initiatedBy}` |
| `RefundCompleted` | `payment.events` | `{refundId, settledAt}` |

### CAP Classification: **CP**
Double-charge is catastrophic — regulatory, reputational, and financial liability. Synchronous writes to primary PostgreSQL, idempotency keys, distributed locks. No eventual consistency compromise.

### SLA Targets

| Availability | P50 Charge | P95 Charge | P99 Charge | P99 Refund | Peak RPS | Write RPS |
|-------------|-----------|-----------|-----------|-----------|---------|-----------|
| 99.999% | 50 ms | 150 ms | 300 ms | 500 ms | 2,000 | 2,000 |

---

## 2.6 Notification Service

**Bounded Context**: Multi-Channel Messaging — owns the dispatch of transactional and marketing notifications across email (SendGrid/SES), SMS (Twilio/SNS), and push (FCM/APNs). This service is a consumer, never a producer of business events. Guaranteed delivery via DLQ.

### Multi-Channel Dispatch Architecture

```
Kafka Topic: notification.commands
       │
       ▼
NotificationConsumer (Spring Kafka @KafkaListener)
       │
       ├──[channel=EMAIL]──► EmailDispatcher (SendGrid REST API)
       ├──[channel=SMS]────► SmsDispatcher (Twilio REST API)
       └──[channel=PUSH]───► PushDispatcher (FCM HTTP v1 API)

On dispatch failure (3 retries, exponential backoff):
       └──► notification.commands.DLQ (manual review queue)
```

### Guaranteed Delivery with DLQ

```java
@Configuration
public class KafkaNotificationConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationCommand>
            notificationListenerFactory(ConsumerFactory<String, NotificationCommand> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, NotificationCommand>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(10); // 10 consumer threads per instance

        // Dead-letter configuration: after 3 failures → DLQ
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLQ", record.partition())),
            new FixedBackOff(1000L, 3L) // 1s delay, 3 retries
        ));
        return factory;
    }
}
```

### Template Engine Integration

Templates stored in DB + cache (Redis 1h TTL). Template variables resolved via Mustache/Handlebars:

```
Email template: ORDER_CONFIRMED
Subject: "Your order {{orderId}} is confirmed!"
Body: "Hi {{firstName}}, your ₹{{amount}} order ships by {{expectedDelivery}}."
```

### Events Consumed

| Event | Source | Template Triggered |
|-------|--------|--------------------|
| `OrderConfirmed` | Order | `ORDER_CONFIRMED` (email + SMS) |
| `PaymentCompleted` | Payment | `PAYMENT_RECEIPT` (email) |
| `OrderShipped` | Order | `SHIPMENT_DISPATCHED` (email + push) |
| `RefundCompleted` | Returns | `REFUND_PROCESSED` (email + SMS) |
| `UserRegistered` | User | `WELCOME_EMAIL` (email) |

### SLA Targets

| Availability | P99 Dispatch (email) | P99 Dispatch (SMS) | P99 Dispatch (push) |
|-------------|---------------------|-------------------|---------------------|
| 99.9% | 2,000 ms | 3,000 ms | 1,500 ms |

---


# Section 3: Extended Commerce Microservices

## 3.1 Seller/Vendor Service

**Bounded Context**: Seller Onboarding and Management — owns seller registration, KYC verification, catalogue permission grants, commission structures, and seller payouts. Distinct from User Service (buyer identity).

### Domain Model

```
Seller
 ├── sellerId (UUID)
 ├── legalName, tradeName, gstin, pan
 ├── status (PENDING_KYC | KYC_SUBMITTED | ACTIVE | SUSPENDED | TERMINATED)
 ├── bankAccount (tokenised — vault reference, not raw IFSC/account)
 ├── commissionTier (STANDARD | PREMIUM | ENTERPRISE)
 └── cataloguePermissions[] (allowed categories)

SellerPayout
 ├── payoutId, sellerId
 ├── periodStart, periodEnd
 ├── grossSales, platformFee, taxDeducted, netAmount
 └── status (PENDING | PROCESSING | PAID | DISPUTED)
```

### Key Design Decisions

- **KYC as an async flow**: KYC document submission triggers a `SellerKycSubmitted` event. A third-party KYC provider webhook confirms approval. The service uses a state machine identical in pattern to Order's.
- **Seller-to-buyer data firewall**: Seller service never exposes bank account details to non-admin callers. All financial data is behind `SELLER_ADMIN` or `PLATFORM_ADMIN` roles.
- **Payout calculation as a batch job**: Spring Batch job runs nightly — aggregates order settlements, deducts commission, generates payout record. Idempotent — re-runnable for the same period.

### SLA Targets

| Availability | P99 Read | P99 Write | Peak RPS |
|-------------|----------|-----------|---------|
| 99.95% | 100 ms | 300 ms | 3,000 |

---

## 3.2 Cart Service

**Bounded Context**: Shopping Session — manages the pre-order shopping basket. Handles guest (session-based) and authenticated (persistent) carts, merge-on-login, and cart expiry.

### Session vs. Persistent Cart Strategy

| Aspect | Session Cart (Guest) | Persistent Cart (Authenticated) |
|--------|---------------------|--------------------------------|
| Storage | Redis only (TTL 24h) | PostgreSQL (durable) + Redis (cache) |
| Identity | `guestSessionId` cookie | `userId` JWT claim |
| Expiry | 24 hours idle | 30 days explicit expiry |
| On Login | Merge with persistent cart | — |
| Data | product IDs, quantities | product IDs, quantities, savedForLater |

### Cart Merge-on-Login Logic

```java
@Service
public class CartMergeService {

    @Transactional
    public Cart mergeOnLogin(String guestSessionId, UUID userId) {
        Cart guestCart = redisCartRepository.findById(guestSessionId)
            .orElse(Cart.empty());
        Cart persistentCart = cartRepository.findByUserId(userId)
            .orElse(Cart.empty());

        // Merge strategy: authenticated cart items take precedence on quantity conflict
        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existing = persistentCart.findBySku(guestItem.getSku());
            if (existing.isPresent()) {
                // Use max quantity of the two (don't double-add)
                existing.get().setQuantity(
                    Math.max(existing.get().getQuantity(), guestItem.getQuantity()));
            } else {
                persistentCart.addItem(guestItem);
            }
        }

        cartRepository.save(persistentCart);
        redisCartRepository.delete(guestSessionId); // evict guest cart
        return persistentCart;
    }
}
```

### Redis-Backed Cart with Spring Data Redis

```java
@RedisHash(value = "cart", timeToLive = 86400L) // 24h TTL for guest carts
public class RedisCart implements Serializable {
    @Id
    private String cartId; // guestSessionId or "user:" + userId

    private List<CartItem> items = new ArrayList<>();
    private Instant lastUpdated;

    @TimeToLive // per-instance TTL override
    private Long ttl;
}

@Repository
public interface RedisCartRepository extends CrudRepository<RedisCart, String> {
    Optional<RedisCart> findById(String cartId);
}
```

### Events Published

| Event | Topic | Payload |
|-------|-------|---------|
| `CartAbandoned` | `cart.events` | `{cartId, userId, items[], totalValue, abandonedAt}` |
| `CartCheckedOut` | `cart.events` | `{cartId, userId, orderId}` |

### CAP Classification: **AP**
Cart staleness for a few seconds (item price change, stock depletion) is acceptable. The definitive stock/price check happens at checkout (Order Service). Redis AP semantics are fine here.

### SLA Targets

| Availability | P99 Read | P99 Write | Peak RPS |
|-------------|----------|-----------|---------|
| 99.95% | 10 ms | 30 ms | 40,000 |

---

## 3.3 Search Service

**Bounded Context**: Product Discovery — provides full-text and faceted search over the product catalogue. Built entirely on Elasticsearch; does not own any data authoritatively.

### Elasticsearch Index Design

```json
PUT /products_v3
{
  "settings": {
    "number_of_shards": 12,
    "number_of_replicas": 2,
    "analysis": {
      "analyzer": {
        "custom_en_hi": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "hindi_stemmer", "english_stemmer", "synonym_filter"]
        }
      }
    },
    "index": {
      "refresh_interval": "1s",
      "max_result_window": 10000
    }
  },
  "mappings": {
    "properties": {
      "title":          { "type": "text", "analyzer": "custom_en_hi", "boost": 3.0 },
      "brand":          { "type": "keyword", "boost": 2.0 },
      "categoryPath":   { "type": "keyword" },
      "basePrice":      { "type": "scaled_float", "scaling_factor": 100 },
      "attributes":     { "type": "flattened" },
      "suggest":        { "type": "completion" },
      "stockAvailable": { "type": "boolean" },
      "avgRating":      { "type": "half_float" }
    }
  }
}
```

### Near-Real-Time Update Pipeline

```
ProductUpdated (Kafka) → Search Index Consumer → Elasticsearch Bulk API (batched, 500ms)
InventoryUpdated (Kafka) → Search Index Consumer → Partial update (stockAvailable field only)
```

The consumer uses Elasticsearch's `_update` API with `_seq_no` / `_primary_term` for optimistic concurrency on index updates.

### Relevance Scoring

Base relevance: BM25. Boosted by:
- `title` match: ×3.0
- `brand` match: ×2.0
- `avgRating > 4.0`: ×1.5 function score
- `stockAvailable = true`: filter, not score (excluded from results if false)
- Recency: linear decay on `updatedAt`, scale 30 days, offset 7 days

### SLA Targets

| Availability | P50 Search | P95 Search | P99 Search | Peak RPS |
|-------------|-----------|-----------|-----------|---------|
| 99.95% | 15 ms | 50 ms | 100 ms | 30,000 |

---

## 3.4 Reviews & Ratings Service

**Bounded Context**: User-Generated Content — owns review submission, moderation workflow, and aggregated rating computation. Feeds back to Product Service via `ReviewAggregated` event.

### Domain Model

```
Review
 ├── reviewId, productId, userId, orderId (must have purchased)
 ├── rating (1–5, SMALLINT)
 ├── title, body (text, max 2000 chars)
 ├── status (PENDING_MODERATION | APPROVED | REJECTED | FLAGGED)
 ├── helpfulVotes, notHelpfulVotes
 └── createdAt

RatingAggregate (materialised, recomputed on each approval)
 ├── productId
 ├── totalRatings, averageRating
 └── distribution (1★:N, 2★:N, 3★:N, 4★:N, 5★:N)
```

### Key Design Decisions

- **Purchase verification**: A review can only be submitted for a product linked to a delivered order by the same user. Cross-service API call to Order Service at submission time.
- **Moderation**: Automated (profanity filter, spam detection) then manual queue for borderline cases. Approved reviews go to Elasticsearch for full-text search.
- **Rating aggregation**: Materialised view updated asynchronously on `ReviewApproved` event. Slight staleness (seconds) is acceptable.

### SLA Targets

| Availability | P99 Read | P99 Write | Peak RPS |
|-------------|----------|-----------|---------|
| 99.9% | 50 ms | 300 ms | 10,000 |

---

## 3.5 Recommendations Service

**Bounded Context**: Personalisation — serves pre-computed recommendation feeds (collaborative filtering, content-based) and real-time popularity fallbacks.

### Read-Path Architecture

```
Offline ML Training (daily batch, Spark)
       │
       ▼
Recommendation Store (Redis sorted sets per userId)
  Key: "recs:user:{userId}"
  Value: sorted set of productIds with relevance scores
  TTL: 24 hours

Request Path:
GET /v1/recommendations/{userId}
       │
       ├─► Redis lookup (HIT ~95% → return in <5ms)
       └─► On MISS → Popularity Fallback (top-N trending, Redis sorted set "trending:global")
```

### Cache Warming Strategy

Before cache expiry, a background job re-runs the model inference for high-activity users (MAU top 20%) and warms the new entries before the old TTL expires. This avoids cache stampede on busy users.

### Fallback to Popularity-Based Ranking

```java
@Service
public class RecommendationService {

    public List<String> getRecommendations(UUID userId, int limit) {
        String key = "recs:user:" + userId;
        Set<String> personalised = redisTemplate.opsForZSet()
            .reverseRange(key, 0, limit - 1);

        if (personalised != null && personalised.size() >= limit) {
            return new ArrayList<>(personalised);
        }

        // Fallback: blend with trending products (popularity-based ranking)
        Set<String> trending = redisTemplate.opsForZSet()
            .reverseRange("trending:global", 0, limit - 1);
        return Stream.concat(
            personalised != null ? personalised.stream() : Stream.empty(),
            trending != null ? trending.stream() : Stream.empty()
        ).distinct().limit(limit).collect(Collectors.toList());
    }
}
```

### SLA Targets

| Availability | P50 Read | P99 Read | Peak RPS |
|-------------|----------|----------|---------|
| 99.9% | 3 ms | 20 ms | 25,000 |


---

## 3.6 Shipping & Logistics Service

**Bounded Context**: Carrier Integration — creates shipments with third-party carriers, ingests tracking webhook updates, and maintains shipment state. Abstracted via a carrier interface to support 10+ carrier integrations without service changes.

### Carrier Abstraction Interface

```java
public interface CarrierClient {
    ShipmentCreationResponse createShipment(ShipmentRequest request);
    TrackingInfo getTracking(String awbNumber);
    CancellationResponse cancelShipment(String awbNumber);
}

@Component("bluedart")
public class BlueDartCarrierClient implements CarrierClient { ... }

@Component("delhivery")
public class DelhiveryCarrierClient implements CarrierClient { ... }

@Service
public class ShippingService {
    private final Map<String, CarrierClient> carriers; // injected by Spring

    public ShipmentCreationResponse createShipment(CreateShipmentCommand cmd) {
        CarrierClient carrier = carriers.get(cmd.getPreferredCarrier());
        return carrier.createShipment(new ShipmentRequest(cmd));
    }
}
```

### Webhook Idempotency

Carrier webhooks may be delivered multiple times (at-least-once). Idempotency enforced via:

```java
@PostMapping("/webhooks/shipment-events")
@Transactional
public ResponseEntity<Void> receiveWebhook(
        @RequestBody ShipmentWebhookPayload payload,
        @RequestHeader("X-Carrier-Signature") String signature) {

    webhookSignatureValidator.verify(payload, signature); // HMAC-SHA256

    // Idempotency: check if this eventId was already processed
    if (processedWebhookRepository.existsByEventId(payload.getEventId())) {
        return ResponseEntity.ok().build(); // idempotent replay — do nothing
    }

    shipmentStateService.transition(payload.getAwbNumber(), payload.getStatus());
    processedWebhookRepository.save(new ProcessedWebhook(payload.getEventId(), Instant.now()));
    return ResponseEntity.ok().build();
}
```

### SLA Targets

| Availability | P99 Create Shipment | P99 Tracking Read | Peak RPS |
|-------------|--------------------|--------------------|---------|
| 99.95% | 500 ms | 100 ms | 5,000 |

---

## 3.7 Returns & Refunds Service

**Bounded Context**: Post-Purchase Returns — manages the full lifecycle of a return from customer request through physical pickup, inspection, and refund or rejection.

### Return State Machine

```
REQUESTED
    │
    ├─[approved by policy engine]──► APPROVED
    │
    ├─[rejected: outside return window]──► REJECTED (terminal)
    │
APPROVED
    │
    └─[carrier assigned]──► PICKUP_SCHEDULED
                                    │
                            [item collected]──► PICKED_UP
                                                    │
                                            [warehouse received]──► RECEIVED
                                                                        │
                                                                [inspection pass]──► INSPECTED_PASS
                                                                [inspection fail]──► INSPECTED_FAIL
                                                                        │
                                                          INSPECTED_PASS──[refund triggered]──► REFUNDED (terminal)
                                                          INSPECTED_FAIL──[partial refund or reject]──► REJECTED (terminal)
```

### Compensating Transactions

On `ReturnApproved`:
1. **Inventory Service** ← `RestoreStockCommand`: add quantity back to warehouse
2. **Payment Service** ← `InitiateRefundCommand`: trigger refund of `originalAmount - restockingFee`
3. **Shipping Service** ← `CreateReturnShipmentCommand`: create pickup shipment

All three compensating calls are idempotent. If the return inspection fails after stock was restored, `CancelStockRestoration` is published.

### SLA Targets

| Availability | P99 Request | Refund Settlement SLA | Peak RPS |
|-------------|------------|----------------------|---------|
| 99.95% | 300 ms | ≤ 5 business days | 2,000 |

---

## 3.8 Promotions & Coupons Service

**Bounded Context**: Discount Engine — manages coupon lifecycle, validates coupon applicability, enforces usage limits (including single-use coupons under concurrent redemption), and evaluates promotion stacking rules.

### Distributed Lock for Single-Use Coupons

Single-use coupons are the hardest concurrency problem in the platform: two simultaneous checkouts with the same coupon code must not both succeed.

```java
@Service
public class CouponValidationService {

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;

    @Transactional
    public CouponValidationResult validate(String couponCode, UUID userId, BigDecimal orderAmount) {
        // Distributed lock: only one thread can validate this coupon at a time
        RLock lock = redissonClient.getLock("coupon-lock:" + couponCode);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS); // wait 3s, hold max 10s
            if (!locked) {
                throw new CouponConcurrentValidationException(couponCode);
            }

            Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new CouponNotFoundException(couponCode));

            validateApplicability(coupon, userId, orderAmount);

            // Mark as USED atomically within the locked section
            coupon.incrementUsageCount();
            if (coupon.isMaxUsageReached()) {
                coupon.setStatus(CouponStatus.EXHAUSTED);
            }
            couponRepository.save(coupon);

            return CouponValidationResult.valid(coupon.calculateDiscount(orderAmount));

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### Promotion Stacking Rules

Promotions are evaluated by a rule engine (Drools or Spring's expression language). Stacking policy:
- `EXCLUSIVE` coupons: cannot stack with any other promotion
- `STACKABLE_PRODUCT` coupons: stack with bank offers, not other product coupons
- `BANK_OFFER` coupons: stack with product promotions, exclusive with other bank offers
- Maximum discount cap: `min(calculatedDiscount, orderAmount × 0.30)` — no more than 30% off from all stacked promotions combined

### SLA Targets

| Availability | P99 Validate | P99 Apply | Peak RPS |
|-------------|------------|-----------|---------|
| 99.99% | 50 ms | 100 ms | 20,000 |

---


# Section 4: Enterprise Cross-Cutting Services

## 4.1 API Gateway (Spring Cloud Gateway)

**Role**: Single ingress point for all external traffic. Handles SSL termination, JWT validation, rate limiting, request routing, and response transformation. Zero business logic.

### Route Configuration

```yaml
# application.yml (Spring Cloud Gateway)
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://product-service            # Eureka service discovery
          predicates:
            - Path=/api/v1/products/**
          filters:
            - RewritePath=/api/v1/products/(?<segment>.*), /v1/products/${segment}
            - AddRequestHeader=X-Gateway-Version, 2023.1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 500     # tokens/sec per client
                redis-rate-limiter.burstCapacity: 1000
                redis-rate-limiter.requestedTokens: 1
                key-resolver: "#{@jwtKeyResolver}"        # rate limit by JWT sub
            - name: CircuitBreaker
              args:
                name: product-service-cb
                fallbackUri: forward:/fallback/products

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - JwtValidationFilter                          # custom JWT filter bean
            - TenantContextFilter                          # extracts tenantId to header
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: "#{@jwtKeyResolver}"

      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOriginPatterns: "https://*.platform.com"
            allowedMethods: [GET, POST, PUT, PATCH, DELETE, OPTIONS]
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
        - AddRequestHeader=X-Correlation-ID, ${T(java.util.UUID).randomUUID()}
```

### JWT Validation Filter

```java
@Component
public class JwtValidationFilter implements GatewayFilter, Ordered {

    private final JwtDecoder jwtDecoder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Jwt jwt = jwtDecoder.decode(token); // validates signature + expiry
            ServerWebExchange mutated = exchange.mutate()
                .request(r -> r
                    .header("X-User-Id", jwt.getSubject())
                    .header("X-User-Roles", String.join(",", jwt.getClaimAsStringList("roles")))
                    .header("X-Tenant-Id", jwt.getClaimAsString("tenantId")))
                .build();
            return chain.filter(mutated);
        } catch (JwtException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override public int getOrder() { return -100; } // before routing filters
}
```

---

## 4.2 Auth/IAM Service

**Role**: OAuth 2.0 / OpenID Connect authority. Issues JWTs, manages refresh tokens, enforces RBAC. Built on Spring Authorization Server 1.x.

### OAuth 2.0 Flows

**Authorization Code + PKCE (User-facing apps)**:
```
Browser/App → GET /oauth2/authorize?response_type=code&client_id=web&code_challenge=...
Auth Server → Login page
User authenticates
Auth Server → Redirect with ?code=XXXXX
App → POST /oauth2/token { code, code_verifier, grant_type=authorization_code }
Auth Server → { access_token (JWT, 15min), refresh_token (opaque, 7 days), id_token }
```

**Client Credentials (service-to-service)**:
```
Service A → POST /oauth2/token { client_id, client_secret, grant_type=client_credentials, scope=inventory:read }
Auth Server → { access_token (JWT, 1h) }  — no refresh token (re-authenticate when expired)
```

**Refresh Token Rotation**:
```
Client → POST /oauth2/token { refresh_token=OLD_TOKEN, grant_type=refresh_token }
Auth Server → { access_token (new), refresh_token (new, single-use), id_token }
OLD_TOKEN is immediately invalidated
```

### JWT Structure

```json
{
  "header": { "alg": "RS256", "kid": "2024-01-key" },
  "claims": {
    "iss": "https://auth.platform.com",
    "sub": "user-uuid-here",
    "aud": ["platform-api"],
    "exp": 1735689600,
    "iat": 1735688700,
    "jti": "unique-token-id",           // for revocation blacklist
    "roles": ["BUYER", "SELLER"],
    "tenantId": "tenant-uuid",
    "sessionId": "session-uuid"
  }
}
```

### Spring Security Resource Server Configuration

```java
@Configuration
@EnableMethodSecurity // enables @PreAuthorize at method level
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())       // validates RS256 signature
                    .jwtAuthenticationConverter(jwtAuthConverter()) // maps roles claim to GrantedAuthority
                )
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("roles");
        gac.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(gac);
        return conv;
    }
}

// Method-level security in service layer
@PreAuthorize("hasRole('SELLER') and #sellerId == authentication.principal.subject")
public ProductDto updateProduct(UUID sellerId, UUID productId, ProductUpdateDto dto) { ... }
```

---

## 4.3 Fraud Detection Service

**Role**: Real-time transaction risk scoring (P99 ≤ 50ms) and async post-order analysis.

### Real-Time Scoring Pipeline (Synchronous, ≤50ms P99)

```
Order Service calls POST /v1/fraud/score (synchronous, timeout=45ms)

Pipeline (executed in parallel where possible):
  1. Feature Extraction (5ms)
     - Velocity: orders/hour for userId, IP, device fingerprint
     - Amount: deviation from user's historical avg
     - Location: geolocation anomaly vs billing address
     - Card/UPI: BIN check, first-use flag

  2. ML Model Call (15ms, gRPC to Python model server)
     - Input: 47 normalised features
     - Output: fraud_probability (0.0–1.0)

  3. Rule Engine (5ms, Drools)
     - Hard block: fraud_probability > 0.90
     - Hard block: velocity > 10 orders/hour
     - Soft flag: 0.50 < fraud_probability <= 0.90 → manual review queue

  4. Decision (1ms)
     - ALLOW, MANUAL_REVIEW, or BLOCK
```

### PII Pseudonymisation

Before any data leaves the Payment domain:
```java
FraudScoreRequest fraudRequest = FraudScoreRequest.builder()
    .orderId(order.getOrderId())              // not PII
    .amount(order.getFinalAmount())           // not PII
    .velocityScore(velocityService.score(userId)) // pre-computed, not raw userId
    .deviceFingerprint(hashService.pseudonymise(rawDeviceId)) // HMAC(deviceId, secret)
    .binCountry(paymentMethod.getBinCountry()) // not PII
    // NOTE: userId, email, phone, PAN are NEVER sent to Fraud Detection
    .build();
```

### Manual Review Queue

Cases where `0.50 < fraud_probability <= 0.90` go to a Redis queue consumed by the Fraud Analyst UI. Unreviewed orders are auto-rejected after 30 minutes to cap fraud exposure.

### SLA Targets

| Availability | P99 Real-time Score | Manual Review SLA | Peak RPS |
|-------------|--------------------|--------------------|---------|
| 99.99% | 50 ms | 30 minutes | 2,000 |

---

## 4.4 Reporting & Analytics Service

**Role**: Lambda Architecture for business intelligence — real-time dashboards (speed layer) and historical analytics (batch layer).

### Lambda Architecture

```
Speed Layer:
  Kafka → Kafka Streams / Flink → Real-time aggregates → Redis (dashboard cache)
  Latency: seconds to minutes

Batch Layer:
  HDFS/S3 → Apache Spark (daily/hourly jobs) → Parquet files → Analytics DB (Redshift/ClickHouse)
  Latency: 1 hour to 1 day

Serving Layer:
  REST API reads from Redis (real-time) or Analytics DB (historical)
  CQRS read models: Elasticsearch for free-text analytics
```

### CQRS Read Model Population

Events from all services flow into `analytics.events` Kafka topic. A Spring Cloud Stream processor projects these into denormalised read models:

```java
@Bean
public Function<KStream<String, DomainEvent>, KStream<String, AnalyticsRecord>> analyticsProjection() {
    return events -> events
        .filter((k, v) -> v != null)
        .mapValues(this::toAnalyticsRecord)
        .to("analytics.read-model", Produced.with(Serdes.String(), analyticsSerde));
}
```

---

## 4.5 Admin Panel Service

**Role**: Operational interface for platform administrators, seller admins, and customer support.

### RBAC Roles

| Role | Capabilities |
|------|-------------|
| `PLATFORM_ADMIN` | All operations, all tenants, delete sellers, override SLAs |
| `SELLER_ADMIN` | Manage own seller products, view own payouts, respond to reviews |
| `SUPPORT` | View-only on orders, users; initiate returns; escalate disputes |

### Audit Logging

Every mutation operation is intercepted by an `AuditInterceptor`:

```java
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(Auditable)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuditLog entry = AuditLog.builder()
            .actor(auth.getName())
            .roles(auth.getAuthorities().toString())
            .action(pjp.getSignature().getName())
            .requestPayload(serializeArgs(pjp.getArgs()))
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        Object result = pjp.proceed();
        entry.setResponseCode("SUCCESS");
        auditLogRepository.save(entry); // append-only table
        return result;
    }
}
```

---

## 4.6 Multi-Tenancy Design

### Strategy Comparison

| Approach | Data Isolation | Operational Complexity | Cost | Recommended For |
|----------|---------------|----------------------|------|----------------|
| **Schema-per-tenant** | Strong (schema boundary) | High (schema migrations × tenants) | High | High-compliance enterprise tenants |
| **Row-Level Security (RLS)** | Good (PostgreSQL RLS policies) | Medium (RLS policies in migrations) | Low | Standard marketplace sellers |
| **Separate DB per tenant** | Complete | Very High | Very High | Tier-1 enterprise contracts only |

**Recommended**: Row-Level Security for 99% of sellers, Schema-per-tenant for enterprise accounts (top 10 sellers by GMV).

### Tenant Context Propagation

```java
// Spring Security: extract tenantId from JWT
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        String tenantId = (String) req.getAttribute("X-Tenant-Id"); // set by API Gateway
        TenantContext.setTenantId(tenantId);
        MDC.put("tenantId", tenantId); // propagate to logs
        try { chain.doFilter(req, res); }
        finally { TenantContext.clear(); MDC.remove("tenantId"); }
    }
}

// OpenFeign: propagate to downstream services
@Component
public class TenantPropagationInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Tenant-Id", TenantContext.getTenantId());
    }
}

// Hibernate: apply tenant filter on all queries
@Component
public class TenantAwareHibernateInterceptor implements HibernatePropertiesCustomizer {
    // Sets hibernate.tenant_identifier_resolver to return TenantContext.getTenantId()
}
```

---

## 4.7 Multi-Region Topology

### Active-Active vs Active-Passive

| Service | Mode | Justification |
|---------|------|---------------|
| API Gateway, Auth, Search, Recommendations | **Active-Active** | Stateless or AP, any region can serve any request |
| Product, Notification, Reviews, Cart | **Active-Active** | AP services, eventual consistency acceptable |
| Order, Payment, Inventory | **Active-Passive** | CP services, single write region, follower regions serve reads |

### Cross-Region Replication

- **PostgreSQL**: Logical replication (publication/subscription). Write region publishes; follower regions subscribe. Lag budget: ≤500ms for critical data (Order, Payment, Inventory). Measured by `pg_stat_replication.write_lag`.
- **Kafka**: MirrorMaker 2 with dedicated replication cluster. Consumer group offset translation enabled. Lag budget: ≤500ms.
- **Redis**: Redis Enterprise Active-Active (CRDT-based) for Cart/Session. Redis Cluster replication (async) for Recommendations cache — staleness acceptable.

### Regional Failover Playbook

**Automated** (Route53 health check driven):
1. Health check detects primary region API Gateway returning >5% 5xx for >60s
2. Route53 fails over DNS to secondary region (TTL: 30s)
3. Secondary Eureka registry promoted to primary
4. Read traffic immediately served; write traffic queued in Kafka (order write region offline)

**Manual** (P1 incident commander):
1. Declare incident, page database team
2. PostgreSQL replica in secondary region promoted to primary (pg_promote())
3. Kafka consumer groups rebalanced to secondary region consumers
4. MirrorMaker 2 direction reversed (secondary → primary for audit)
5. Alert cleared, DR drill documented in post-incident report

**RPO**: ≤60s (replication lag budget)
**RTO**: ≤30 minutes (automated DNS failover in <5min; manual DB promotion in 15-30min)


---

# Section 5: Spring Cloud Ecosystem — Deep Dive

## 5.1 Eureka — Service Discovery

**Problem**: In a containerised environment, service instances start and stop dynamically. Clients cannot use hardcoded IPs. We need a registry that services can register with and that clients can query for current instance locations.

### Architecture

```
Eureka Server Cluster (3 nodes, peered)
  ├── Node 1 (region-1a)
  ├── Node 2 (region-1b)
  └── Node 3 (region-1c)

Each service instance:
  1. On startup → POST /eureka/apps/{appName} with IP:port, health URL
  2. Every 30s → PUT /eureka/apps/{appName}/{instanceId} (heartbeat/lease renewal)
  3. On shutdown → DELETE /eureka/apps/{appName}/{instanceId} (graceful deregistration)

Clients:
  - Fetch full registry on startup
  - Refresh from server every 30s (delta or full)
  - Local cache provides service location if server is unreachable
```

### Server Configuration

```yaml
# eureka-server application.yml
server:
  port: 8761

eureka:
  instance:
    hostname: eureka-server-1
  client:
    register-with-eureka: false       # server doesn't register itself
    fetch-registry: false
    service-url:
      defaultZone: http://eureka-server-2:8761/eureka/,http://eureka-server-3:8761/eureka/
  server:
    enable-self-preservation: true    # don't evict during network partition
    eviction-interval-timer-in-ms: 5000
    renewal-percent-threshold: 0.85   # 85% of expected heartbeats must arrive
    response-cache-update-interval-ms: 3000
```

### Client Configuration

```yaml
# service application.yml (e.g., order-service)
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server-1:8761/eureka/,http://eureka-server-2:8761/eureka/
    registry-fetch-interval-seconds: 10    # faster refresh than default 30s
    initial-instance-info-replication-interval-seconds: 10
  instance:
    prefer-ip-address: true               # Kubernetes: use pod IP, not hostname
    ip-address: ${POD_IP}                 # Kubernetes downward API
    lease-renewal-interval-in-seconds: 10 # heartbeat every 10s (default: 30s)
    lease-expiration-duration-in-seconds: 30 # evict if no heartbeat for 30s
    health-check-url-path: /actuator/health
    metadata-map:
      version: ${APP_VERSION}
      region: ${AWS_REGION}
      zone: ${AZ}
```

### Behaviour Under Partition

When Eureka server is unreachable (network partition):
- Client serves requests using its **local registry cache** — stale but available
- `self-preservation mode` activates: server stops evicting instances (they may still be alive even if heartbeats aren't arriving)
- When partition heals: delta sync restores registry state

Trade-off: self-preservation can result in Eureka routing to dead instances. Resilience4j circuit breakers on the client side handle this — a dead instance triggers CB OPEN after consecutive failures.

### Eureka Client Bean Customisation

```java
@Configuration
public class EurekaClientConfig {

    @Bean
    public EurekaClientConfigBean eurekaClientConfigBean() {
        EurekaClientConfigBean bean = new EurekaClientConfigBean();
        bean.setRegistryFetchIntervalSeconds(10);
        bean.setPreferSameZoneEureka(true);  // zone-affinity: prefer local AZ
        return bean;
    }
}
```

---

## 5.2 Spring Cloud Config Server

**Problem**: Each service needs environment-specific configuration (DB URLs, feature flags, Kafka topics) that must not be baked into the container image.

### Git-Backed Repository Structure

```
config-repo/
├── application.yml                  # Defaults for all services
├── application-dev.yml              # Dev overrides for all services
├── application-prod.yml             # Prod overrides for all services
├── order-service/
│   ├── order-service.yml            # Order-specific defaults
│   ├── order-service-dev.yml
│   └── order-service-prod.yml
└── payment-service/
    ├── payment-service.yml
    └── payment-service-prod.yml
```

Override precedence (highest wins): `{service}-{profile}.yml` > `{service}.yml` > `application-{profile}.yml` > `application.yml`

### Vault Integration for Secrets

```yaml
# config-server application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/platform/config-repo
          default-label: main
          search-paths: "{application}"
        vault:
          host: vault.platform.internal
          port: 8200
          scheme: https
          authentication: KUBERNETES   # Vault K8s auth backend
          kubernetes:
            role: config-server-role
          kv-version: 2
```

Vault secrets take precedence over Git properties. All `${...}` placeholders in service configs resolve from Vault at startup.

### Hot-Reload with @RefreshScope

```java
@Configuration
@RefreshScope  // Bean is re-created when /actuator/refresh is called or bus refresh event arrives
public class DynamicFeatureFlags {

    @Value("${feature.new-checkout-flow.enabled:false}")
    private boolean newCheckoutFlowEnabled;

    public boolean isNewCheckoutFlowEnabled() { return newCheckoutFlowEnabled; }
}
```

Spring Cloud Bus (backed by Kafka or RabbitMQ) broadcasts `/actuator/busrefresh` to all service instances simultaneously:

```yaml
spring:
  cloud:
    bus:
      enabled: true
  kafka:
    bootstrap-servers: kafka:9092
    # Bus uses topic: springCloudBus
```

---

## 5.3 Ribbon → Spring Cloud LoadBalancer Migration

**Problem**: Client-side load balancing across multiple instances of the same service.

### Why Migrate from Ribbon

Ribbon reached end-of-life in 2020. Spring Cloud LoadBalancer is the official replacement. Key differences:

| Feature | Ribbon | Spring Cloud LoadBalancer |
|---------|--------|--------------------------|
| Reactive support | Limited | Full (WebFlux) |
| Spring Boot 3.x | Not supported | Supported |
| Load balancing algorithms | Many built-in | Extensible, fewer defaults |
| Zone awareness | Built-in | Via custom ServiceInstanceListSupplier |
| Maintenance status | End-of-life | Active |

### Migration Steps

1. Remove `spring-cloud-starter-netflix-ribbon` dependency
2. Add `spring-cloud-starter-loadbalancer` if not transitively included
3. Replace `@LoadBalanced RestTemplate` — annotation still works, now backed by Spring Cloud LB
4. Replace `RibbonClient` / `@RibbonClient` with `@LoadBalancerClient`
5. Migrate `ZoneAwareLoadBalancer` config to `ZonePreferenceServiceInstanceListSupplier`

```java
// Before (Ribbon)
@RibbonClient(name = "product-service", configuration = RibbonProductConfig.class)

// After (Spring Cloud LoadBalancer)
@LoadBalancerClient(name = "product-service", configuration = LoadBalancerProductConfig.class)

@Configuration
public class LoadBalancerProductConfig {
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withZonePreference()  // prefer same zone as calling service
            .withHealthChecks()
            .build(context);
    }
}
```

---

## 5.4 Resilience4j — Full Suite

**Problem**: A single slow or failing downstream service should not cascade into platform-wide failure. Resilience4j provides Circuit Breaker, Retry, Bulkhead, RateLimiter, and TimeLimiter.

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-service:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 50             # evaluate last 50 calls
        minimumNumberOfCalls: 20          # don't trip until 20 calls recorded
        failureRateThreshold: 50          # >50% failures → OPEN
        waitDurationInOpenState: 30s      # wait 30s before trying HALF_OPEN
        permittedNumberOfCallsInHalfOpenState: 5
        slowCallDurationThreshold: 2s     # calls >2s count as slow
        slowCallRateThreshold: 80         # >80% slow calls → OPEN
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - feign.FeignException$ServiceUnavailable
        ignoreExceptions:
          - com.platform.exception.BusinessValidationException

      inventory-service:
        slidingWindowSize: 30
        failureRateThreshold: 60
        waitDurationInOpenState: 20s

  retry:
    instances:
      inventory-service:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2.0  # 500ms, 1000ms, 2000ms
        retryExceptions:
          - java.io.IOException
        ignoreExceptions:
          - com.platform.inventory.InsufficientStockException

  bulkhead:
    instances:
      fraud-service:
        maxConcurrentCalls: 50           # max 50 concurrent calls to fraud service
        maxWaitDuration: 100ms           # if all 50 slots busy, wait max 100ms then reject

  thread-pool-bulkhead:
    instances:
      notification-service:
        maxThreadPoolSize: 20
        coreThreadPoolSize: 10
        queueCapacity: 50
        keepAliveDuration: 20ms

  ratelimiter:
    instances:
      external-carrier-api:
        limitForPeriod: 100              # 100 calls per refresh period
        limitRefreshPeriod: 1s
        timeoutDuration: 250ms

  timelimiter:
    instances:
      fraud-service:
        timeoutDuration: 45ms            # fraud scoring must complete in 45ms
        cancelRunningFuture: true
```

### Annotation Usage

```java
@Service
public class OrderService {

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventory-service")
    @Bulkhead(name = "inventory-service", type = Bulkhead.Type.THREADPOOL)
    @TimeLimiter(name = "inventory-service")
    public CompletableFuture<ReservationResult> reserveInventory(ReservationRequest req) {
        return CompletableFuture.supplyAsync(() -> inventoryClient.reserve(req));
    }

    // Fallback: called when CB is OPEN or all retries exhausted
    public CompletableFuture<ReservationResult> inventoryFallback(ReservationRequest req, Exception ex) {
        log.warn("Inventory service unavailable, using fallback for order {}", req.getOrderId(), ex);
        return CompletableFuture.completedFuture(ReservationResult.pending(req.getOrderId()));
    }
}
```

### Actuator Exposure

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers,retries,bulkheads
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```

---

## 5.5 Hystrix → Resilience4j Migration Guide

### Feature Parity Table

| Hystrix | Resilience4j | Notes |
|---------|-------------|-------|
| `@HystrixCommand(fallbackMethod)` | `@CircuitBreaker(fallbackMethod)` | Direct replacement |
| `HystrixObservableCommand` | `@CircuitBreaker` on reactive methods | Use Reactor operators |
| `HystrixCommand.GroupKey` | Resilience4j instance name | One instance per downstream service |
| Thread pool isolation | `@Bulkhead(type=THREADPOOL)` | Explicit thread pool bulkhead |
| Semaphore isolation | `@Bulkhead(type=SEMAPHORE)` | Default bulkhead type |
| `hystrix.command.*.execution.timeout` | `resilience4j.timelimiter` | Separate annotation |
| `hystrix.command.*.circuitBreaker.requestVolumeThreshold` | `minimumNumberOfCalls` | Sliding window |
| Hystrix Dashboard | Actuator + Micrometer + Grafana | No separate dashboard |
| Turbine (aggregator) | Prometheus scraping | Native aggregation |

### Migration Steps

1. **Add dependencies**: `resilience4j-spring-boot3`, `resilience4j-reactor` (if reactive)
2. **Replace `@HystrixCommand`**: change to `@CircuitBreaker(name="...", fallbackMethod="...")`
3. **Move config from Java to YAML**: Hystrix config in Java annotations → Resilience4j in `application.yml` under `resilience4j.*`
4. **Replace fallback signature**: Resilience4j fallbacks accept the exception as last parameter
5. **Add `@Bulkhead` where thread isolation was used**
6. **Add `@TimeLimiter` where `execution.isolation.thread.timeoutInMilliseconds` was set**
7. **Update metrics dashboards**: replace Hystrix-specific metric names with Resilience4j/Micrometer names (`resilience4j.circuitbreaker.*`)

---

## 5.6 OpenFeign

**Problem**: Declarative HTTP client that integrates naturally with service discovery and Resilience4j.

```java
package com.platform.order.client;

@FeignClient(
    name = "inventory-service",                // Eureka service name
    fallbackFactory = InventoryClientFallbackFactory.class,
    configuration = InventoryFeignConfig.class
)
public interface InventoryClient {

    @PostMapping("/v1/inventory/reserve")
    @CircuitBreaker(name = "inventory-service")
    ReservationResponse reserve(@RequestBody ReservationRequest request);

    @GetMapping("/v1/inventory/{productId}/availability")
    AvailabilityResponse checkAvailability(@PathVariable UUID productId);
}

// Header propagation interceptor
@Component
public class PlatformFeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // Propagate correlation ID for distributed tracing
        template.header("X-Correlation-ID", MDC.get("traceId"));
        // Propagate tenant context
        template.header("X-Tenant-Id", TenantContext.getTenantId());
        // Propagate service-to-service JWT (client_credentials token)
        template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getServiceToken());
    }
}

// Error decoder: map HTTP error codes to domain exceptions
@Component
public class InventoryErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 409 -> new InsufficientStockException("Insufficient stock");
            case 404 -> new ProductNotFoundException("Product not found");
            case 503 -> new ServiceUnavailableException("Inventory service unavailable");
            default  -> new Default().decode(methodKey, response);
        };
    }
}

// Feign configuration
@Configuration
public class InventoryFeignConfig {
    @Bean public InventoryErrorDecoder errorDecoder() { return new InventoryErrorDecoder(); }
    @Bean public PlatformFeignInterceptor interceptor() { return new PlatformFeignInterceptor(); }

    @Bean
    public Retryer retryer() {
        // Feign retry (for network-level retries before Resilience4j kicks in)
        return new Retryer.Default(100L, 1000L, 3);
    }
}
```

---

## 5.7 Zipkin + Spring Cloud Sleuth — Distributed Tracing

**Problem**: A single user request traverses 5–10 services. Without tracing, debugging latency and errors is nearly impossible.

### Trace Model

```
TraceId: abc123  (same across all services for one user request)
  └── Span: API Gateway (duration: 350ms)
        └── Span: Order Service (duration: 300ms)
              ├── Span: Inventory Reserve (duration: 50ms) [remote call]
              ├── Span: Payment Charge (duration: 200ms) [remote call]
              └── Span: Kafka publish (duration: 5ms) [async]
                    └── Span: Notification Consumer (duration: 80ms) [different trace context]
```

### Propagation

**HTTP (B3 headers)**:
```
X-B3-TraceId: abc123def456
X-B3-SpanId: 789abc
X-B3-ParentSpanId: 111aaa
X-B3-Sampled: 1
```

**Kafka (Record Headers)**: Same B3 headers injected into Kafka record headers by the producer, extracted by the consumer. Sleuth 3.x handles this automatically.

### Sampling Strategy

```yaml
management:
  tracing:
    sampling:
      probability: 0.05   # 5% of requests traced (high-RPS services: Product, Search)
                          # Override for specific paths:
spring:
  sleuth:
    sampler:
      rate: 10            # max 10 traces/sec regardless of probability
    # Always sample error paths:
    web:
      additional-skip-pattern: /actuator/**  # don't trace health checks
```

For error paths: any span with `error=true` is always sampled and exported, regardless of rate.

### Custom Span with Business Metadata

```java
@Service
public class PaymentService {

    private final Tracer tracer;

    public PaymentResponse charge(ChargeRequest req) {
        Span span = tracer.nextSpan().name("payment.charge");
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            // Tag span with business context — queryable in Zipkin/Tempo
            span.tag("payment.orderId", req.getOrderId().toString());
            span.tag("payment.amount", req.getAmount().toString());
            span.tag("payment.currency", req.getCurrency());
            span.tag("payment.method", req.getPaymentMethod().name());

            PaymentResponse response = executePayment(req);
            span.tag("payment.transactionRef", response.getTransactionRef());
            return response;

        } catch (PaymentException ex) {
            span.tag("error", ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }
}
```

### Storage Backend

| Environment | Backend | Retention |
|-------------|---------|-----------|
| Development | Zipkin in-memory | Session |
| Staging | Zipkin + Elasticsearch | 7 days |
| Production | Grafana Tempo | 30 days hot, 90 days cold |

---


# Section 6: Architectural Patterns Catalogue

---

### Pattern 6.1: Saga — Choreography Variant

**Category**: Data | Integration

**Problem Solved**: Distributed transactions spanning multiple services (Order → Inventory → Payment → Notification) without two-phase commit or shared databases.

**Solution**: Each service publishes domain events after completing its local transaction. Other services listen and react. No central coordinator.

**Advantages**:
- Loose coupling — services are unaware of each other except through event contracts
- Highly scalable — no bottleneck coordinator
- Simple to add new participants (just subscribe to relevant events)

**Trade-offs**:
- Difficult to track overall transaction status without a dedicated event aggregator
- Complex compensating transaction choreography as flow grows beyond 4 services
- Debugging cross-service flows requires good distributed tracing

**Failure Scenarios**:
- `StockReserved` event published but Kafka is unavailable before Payment Service consumes it: reservation stays indefinitely; mitigated by TTL expiry job
- Payment fails after stock reserved: `PaymentFailed` event must reach Inventory, which may also be failing; Kafka retention ensures eventual delivery

**Anti-Patterns to Avoid**:
- Using choreography for flows with >6 participants — the implicit coupling becomes unmaintainable
- Not versioning event schemas — a breaking change breaks all subscribers simultaneously

**Applicable Services**: Order fulfilment, Return lifecycle, Seller KYC approval

**Java Implementation**: See Section 2.4 Order Service choreography sequence.

---

### Pattern 6.2: Saga — Orchestration Variant

**Category**: Data | Integration

**Problem Solved**: Same as choreography, but requires explicit flow visibility and simpler compensation logic.

**Solution**: A central `OrderSagaOrchestrator` uses Spring State Machine to coordinate each step, calling participant services directly (via Feign/REST) or via commands on a Kafka topic.

**Advantages**:
- Single place to understand transaction flow
- Easier to implement complex compensation logic
- Built-in state persistence and replay
- Clear visibility into which step failed

**Trade-offs**:
- Orchestrator becomes a coupling point and potential bottleneck
- Orchestrator is a stateful component requiring careful failover handling
- More infrastructure (state machine persistence, leader election for multi-instance orchestrator)

**Failure Scenarios**:
- Orchestrator crashes mid-saga: Spring State Machine state is persisted to DB; on restart, saga resumes from last committed state
- Downstream service returns timeout: TimeLimiter triggers; orchestrator sends compensation event

**Anti-Patterns to Avoid**:
- Putting business logic inside the orchestrator — it should only coordinate, not decide
- Using synchronous blocking calls without timeouts and circuit breakers

**Applicable Services**: Order fulfilment (primary flow), Refund processing

**Java Implementation**: See Section 2.4 `OrderSagaOrchestrator` code snippet.

---

### Pattern 6.3: CQRS (Command Query Responsibility Segregation)

**Category**: Data

**Problem Solved**: A single data model optimised for writes (normalised, transaction-safe) is never optimal for complex reads (joins, aggregations, faceting). Forces architectural tradeoffs.

**Solution**: Separate write model (PostgreSQL, normalised, strongly consistent) from read model (Elasticsearch or Redis, denormalised, optimised for query patterns). Events drive projection updates.

**Advantages**:
- Read model can be optimised (indexed, denormalised) without affecting write consistency
- Independent scaling of read and write replicas
- Multiple read models for different query patterns (e.g., Elasticsearch for search, Redis for cache)

**Trade-offs**:
- Eventual consistency between write and read stores (seconds to minutes)
- Additional infrastructure complexity (event pipeline, projection updater)
- Debugging read/write divergence requires tooling

**Failure Scenarios**:
- Projection updater crashes: events accumulate in Kafka; on recovery, projection replays from last committed offset — eventual consistency guaranteed but lag increases
- Elasticsearch write failure: retry with exponential backoff; Kafka offset not committed until successful

**Anti-Patterns to Avoid**:
- Querying the write store for read-heavy operations (defeats the purpose)
- Not handling projection failures — silent divergence between write and read models

**Applicable Services**: Product (PostgreSQL write → Elasticsearch read), Order (PostgreSQL write → Analytics read model)

**Java Implementation**:
```java
// Event-driven projection updater for Product → Elasticsearch
@Component
@KafkaListener(topics = "product.events", groupId = "search-projection-updater")
public class ProductSearchProjectionUpdater {

    private final ElasticsearchOperations esOps;

    @KafkaHandler
    public void on(ProductUpdated event) {
        // Partial update: only changed fields, preserves existing doc
        UpdateQuery updateQuery = UpdateQuery.builder(event.getProductId().toString())
            .withDocument(Document.from(Map.of(
                "title", event.getTitle(),
                "basePrice", event.getBasePrice(),
                "status", event.getStatus(),
                "updatedAt", Instant.now()
            )))
            .withRetryOnConflict(3)
            .build();
        esOps.update(updateQuery, IndexCoordinates.of("products_v3"));
    }
}
```

---

### Pattern 6.4: Event Sourcing

**Category**: Data

**Problem Solved**: Traditional CRUD loses history — you know current state but not how you got there. Event sourcing persists every state change as an immutable event, enabling full audit trail and temporal queries.

**Solution**: Persist domain events to an append-only event store. Current state is derived by replaying events. Snapshots every N events avoid full replay cost.

**Event Store Schema**:
```sql
CREATE TABLE domain_events (
    event_id       UUID PRIMARY KEY,
    aggregate_id   UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,    -- e.g., "Order"
    event_type     VARCHAR(200) NOT NULL,    -- e.g., "OrderCreated"
    event_version  INT NOT NULL,             -- monotonically increasing per aggregate
    payload        JSONB NOT NULL,
    metadata       JSONB,                   -- traceId, userId, timestamp
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (aggregate_id, event_version)    -- prevents concurrent write race
);
CREATE INDEX ON domain_events (aggregate_id, event_version);

-- Snapshots: every 50 events
CREATE TABLE aggregate_snapshots (
    aggregate_id   UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    snapshot_data  JSONB NOT NULL,
    snapshot_at_version INT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);
```

**State Reconstruction**:
```java
public Order reconstruct(UUID orderId) {
    // Load snapshot if available
    Optional<AggregateSnapshot> snapshot = snapshotRepo.findByAggregateId(orderId);
    int fromVersion = snapshot.map(AggregateSnapshot::getSnapshotAtVersion).orElse(0);
    Order order = snapshot.map(s -> deserialize(s.getSnapshotData(), Order.class))
                         .orElse(new Order());

    // Apply events since snapshot
    List<DomainEvent> events = eventStore.findByAggregateIdAndVersionGreaterThan(orderId, fromVersion);
    events.forEach(order::apply);
    return order;
}
```

**Advantages**: Complete audit trail, temporal queries, event-driven projections, easy debugging

**Trade-offs**: Replay cost without snapshots, event schema evolution complexity, learning curve

**Anti-Patterns to Avoid**: Using event sourcing for every entity — reserve for aggregates requiring full history (Payment, Order)

**Applicable Services**: Payment (PCI audit trail), Order (dispute resolution)

---

### Pattern 6.5: Outbox Pattern

**Category**: Data | Integration

**Problem Solved**: Atomically writing to a database AND publishing to Kafka. Without this, a commit succeeds but the Kafka publish fails, leaving the system in an inconsistent state.

**Solution**: Write the event to an `outbox` table in the same transaction as the business data. A separate process (Debezium CDC or scheduled poller) reads the outbox and publishes to Kafka.

**Debezium CDC Variant** (production preferred):
- Debezium monitors the `payment_outbox` table's WAL (Write-Ahead Log)
- On INSERT of a `PENDING` row: Debezium publishes to Kafka, then marks row `PUBLISHED`
- Zero polling overhead; sub-second latency

**Scheduled Poller Variant** (simpler, higher latency):
```java
@Scheduled(fixedDelay = 1000) // every 1 second
@Transactional
public void publishPendingEvents() {
    List<OutboxEvent> pending = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING", 100);
    for (OutboxEvent event : pending) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                         .get(5, TimeUnit.SECONDS); // synchronous for ordering guarantee
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
        } catch (Exception ex) {
            event.incrementRetryCount();
            if (event.getRetryCount() >= 5) event.setStatus("DEAD_LETTER");
        }
    }
    outboxRepository.saveAll(pending);
}
```

**Failure Scenarios**:
- Application crash after DB commit, before poller runs: events remain `PENDING` and are published on recovery
- Kafka unavailable: events accumulate in outbox; published in order when Kafka recovers

**Anti-Patterns to Avoid**:
- Publishing to Kafka directly in the transaction — if Kafka is down, the transaction hangs
- Not deduplicating on the consumer side — outbox guarantees at-least-once, consumers must handle duplicates

**Applicable Services**: Payment, Order, Returns (all high-stakes event publishers)

---

### Pattern 6.6: Circuit Breaker

**Category**: Resilience

**Problem Solved**: A slow or failing downstream service holds caller threads open, exhausting the thread pool and cascading failures across the platform.

**Solution**: Wrap calls in a circuit breaker that tracks failure rates. When the threshold is exceeded, the circuit OPENS and calls fast-fail without reaching the downstream service. After a wait duration, it enters HALF_OPEN and allows a test call.

**Advantages**: Prevents cascading failures, fast-fails instead of blocking, enables system recovery

**Trade-offs**: Requires tuning (too sensitive = false positives; too lenient = late detection), fallback logic adds complexity

**Failure Scenarios**:
- CB opens on transient network blip: `waitDurationInOpenState` prevents thundering herd when the downstream recovers
- CB never closes: monitor `circuitbreaker.state` metric; alert if CB remains OPEN >5 minutes

**Anti-Patterns to Avoid**:
- Circuit breaker without fallback — opens the circuit but still throws to the caller
- Using one circuit breaker for all downstream services — a slow Product call should not affect Payment circuit

**Applicable Services**: All inter-service calls via OpenFeign

See Section 5.4 for full Resilience4j configuration.

---

### Pattern 6.7: Bulkhead

**Category**: Resilience

**Problem Solved**: An unresponsive downstream service exhausts the shared thread pool, blocking all other operations even those targeting healthy services.

**Solution**: Isolate thread pools (ThreadPoolBulkhead) per downstream service. A call to a slow Fraud service uses the fraud-service thread pool; it cannot starve threads destined for the Payment service.

**Thread Pool Sizing Formula**:
```
maxThreadPoolSize = (expected P99 latency / average service call latency) × max concurrent users
                  + 10% headroom

Example (Fraud service): P99 = 45ms, avg call = 15ms, max concurrent = 200
  = (45/15) × 200 × 1.1 = 660 → round to 64 (power of 2, bounded by container CPU)
```

**Anti-Patterns to Avoid**:
- Single thread pool for all downstream calls — one slow service starves all others
- Thread pool larger than host CPU × 2 — context switching overhead exceeds benefit

**Applicable Services**: Order service (isolated pools for Inventory, Payment, Fraud, Notification)

---

### Pattern 6.8: Retry with Exponential Backoff + Jitter

**Category**: Resilience

**Problem Solved**: Transient failures (network blip, brief service overload) should not immediately fail the operation. But synchronised retries cause thundering herd.

**Solution**: Retry with exponential backoff. Add random jitter to desynchronise retries across clients.

```java
// application.yml
resilience4j:
  retry:
    instances:
      inventory-service:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2.0   # 500ms → 1000ms → 2000ms
        randomizedWaitFactor: 0.5           # ±50% jitter: 250–750ms, 500–1500ms, 1000–3000ms
```

**Anti-Patterns to Avoid**:
- Retrying non-idempotent operations (e.g., POST /payments without idempotency key) — causes duplicate charges
- Retrying without backoff — thundering herd on recovering services

---

### Pattern 6.9: Idempotent Consumer

**Category**: Integration

**Problem Solved**: Kafka at-least-once delivery means the same event may be delivered multiple times. Processing it multiple times causes double-charges, duplicate emails, duplicate inventory decrements.

**Solution**: Maintain a `processed_events` deduplication table. Before processing, check if eventId was already processed; skip if so.

```java
@KafkaListener(topics = "payment.events", groupId = "order-payment-consumer")
@Transactional
public void onPaymentEvent(PaymentCompletedEvent event) {
    if (processedEventRepo.existsByEventId(event.getEventId())) {
        log.debug("Duplicate event {}, skipping", event.getEventId());
        return; // idempotent: already processed
    }

    orderService.confirmPayment(event.getOrderId(), event.getTransactionRef());

    processedEventRepo.save(new ProcessedEvent(event.getEventId(), "PaymentCompleted",
        event.getOrderId().toString(), Instant.now()));
}
```

```sql
CREATE TABLE processed_events (
    event_id       UUID PRIMARY KEY,
    event_type     VARCHAR(200) NOT NULL,
    aggregate_id   VARCHAR(100),
    processed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Partition by processed_at for efficient cleanup (retain 30 days)
```

**Applicable Services**: All Kafka consumers, especially Order, Inventory, Returns

---

### Pattern 6.10: API Gateway Pattern

**Category**: Integration | Decomposition

**Problem Solved**: Clients would need to know the location and contract of every microservice. Cross-cutting concerns (auth, rate limiting, SSL) would need to be implemented in every service.

**Solution**: Single entry point (Spring Cloud Gateway) handles all external traffic. Services remain internal-only.

See Section 4.1 for complete implementation.

---

### Pattern 6.11: Strangler Fig

**Category**: Decomposition

**Problem Solved**: Migrating from a monolith to microservices without a big-bang rewrite.

**Solution**: Gradually replace monolith functionality by routing specific endpoints to new microservices. The monolith "strangles" as more routes are migrated.

**Migration Strategy**:
1. **Identify a bounded context** to extract first — typically the highest-value, clearest boundary (e.g., Notifications, which has the fewest inbound dependencies)
2. **Deploy new service** alongside monolith; route reads to new service
3. **Route writes** to new service; monolith consumes events for its dependent reads
4. **Cut over reads** from monolith; decommission monolith's notification code
5. **Repeat** for next bounded context

**API Gateway as strangler intermediary**: Route `/api/notifications/**` to new service; all other paths to monolith. This is transparent to clients.

**Anti-Patterns to Avoid**:
- Sharing the monolith's database with the new service — extends the coupling, not a true separation
- Extracting services with many inbound dependencies first — creates an integration nightmare

---

### Pattern 6.12: Sidecar / Service Mesh

**Category**: Integration

**Problem Solved**: Cross-cutting concerns like mTLS, load balancing, observability, and circuit breaking are implemented in every service, leading to duplication and inconsistency.

**Solution**: Istio injects an Envoy sidecar proxy alongside every Spring Boot pod. Envoy handles mTLS, traffic shaping, retries, and telemetry — invisible to application code.

**Spring Boot Integration**:
```yaml
# Disable Resilience4j circuit breaker if relying on Istio's retry policy
# (Avoid double retry — Istio + Resilience4j retries multiply)
# Recommendation: Use Resilience4j for application-level resilience (business logic fallbacks)
#                 Use Istio for infrastructure-level (network retries, mTLS)
```

**Applicable Services**: All services running in Kubernetes with Istio installed

---

### Pattern 6.13: Database per Service

**Category**: Decomposition

**Problem Solved**: A shared database creates hidden coupling. Schema changes by one team break another team's service. A slow query from one service degrades another's performance.

**Solution**: Each service owns its own database. No service may connect directly to another service's database. Data sharing is only via APIs or events.

**Enforcement**:
- Network policy: each DB pod has a NetworkPolicy allowing only its owning service's pods
- CI gate: any service that imports another service's JPA entity fails the build
- Review checklist: ADR required for any cross-service DB access

**Permitted Exceptions**:
- Reporting DB replica: read-only, non-transactional, one replica of select transactional DBs for analytics (no writes, no cross-service joins in OLTP path)

---

### Pattern 6.14: Shared Database (ANTI-PATTERN)

**Category**: Anti-Pattern

**Problem Solved**: *(Nothing — this is what we are avoiding)*

**Failure Narrative**: In a large-scale ecommerce platform, the Order and Inventory services shared a PostgreSQL instance. During a Black Friday sale, the Inventory service ran a full table scan to compute available stock counts for a real-time dashboard. This held locks on `stock_items`, which are required by the Order service to process checkouts. For 8 minutes, order creation was completely blocked — not because the Order service was overloaded, but because of an unrelated operation in a database it shared. The postmortem concluded that the shared schema coupling was the root cause. The fix required a 3-week emergency extraction to separate databases, executed under production freeze.

**Lesson**: Database-level coupling makes apparent isolation illusory. A schema migration, a runaway query, a deadlock in one service's tables propagates to all co-tenants.

---

### Pattern 6.15: Two-Phase Commit (2PC) (ANTI-PATTERN)

**Category**: Anti-Pattern

**Problem Solved**: *(Nothing viable in microservices — this is what we are avoiding)*

**Why It Fails**:
- **Coordinator single point of failure**: If the coordinator crashes during the commit phase, participants block indefinitely waiting for the decision
- **Blocking protocol**: All participants hold locks for the duration of the protocol — at microservice scale (100ms average network latency × 10 services), lock hold time is unacceptable
- **No partial commit**: If any participant votes NO or is unavailable, the entire transaction rolls back — at scale, always-available services (Redis, external APIs) may not support XA

**What to Use Instead**: Saga pattern (choreography or orchestration). Sagas use compensating transactions instead of rollback, are non-blocking, and do not require all participants to be available simultaneously.

---

## 6.16 CAP Theorem Classification Table

| Service | CAP | Consistency Model | Justification | Staleness Tolerance |
|---------|-----|-------------------|---------------|---------------------|
| User | CP | Strong (read-after-write) | Account suspension must be immediate; split-brain = security risk | 0 |
| Product | AP | Eventual (seconds) | Stale product listing is tolerable; availability > consistency | 5–60 seconds |
| Inventory | CP | Strong (serialisable) | Overselling is P0; concurrent stock decrement must be atomic | 0 |
| Order | CP | Strong (read-after-write) | Double-order = P0; saga state must be consistent | 0 |
| Payment | CP | Strong (linearisable) | Double-charge = regulatory violation | 0 |
| Notification | AP | Eventual (minutes) | Delayed email is annoying, not catastrophic | 1–5 minutes |
| Seller | CP | Strong | KYC approval must be immediately reflected | 0 |
| Cart | AP | Eventual (seconds) | Stale cart items caught at checkout; availability > consistency | 5–30 seconds |
| Search | AP | Eventual (seconds to minutes) | Search index lag acceptable; availability critical | 10–120 seconds |
| Reviews | AP | Eventual (seconds) | Review aggregation lag is fine | 10–60 seconds |
| Recommendations | AP | Eventual (hours) | Pre-computed; freshness is a personalisation quality concern, not correctness | 24 hours |
| Shipping | AP | Eventual (minutes) | Tracking status lag acceptable | 1–5 minutes |
| Returns | CP | Strong | Refund state must be consistent with Payment and Inventory | 0 |
| Promotions | CP | Strong | Coupon usage must be exact to prevent over-redemption | 0 |
| API Gateway | AP | N/A (stateless routing) | Routes are configuration, not transactional data | 30 seconds |
| Auth/IAM | CP | Strong | Token revocation must be immediate | 0 |
| Fraud Detection | AP | Eventual (model freshness) | Model retrained periodically; scoring rules may be seconds stale | 1–300 seconds |
| Reporting | AP | Eventual (minutes to hours) | Business reports have inherent reporting lag | 5 minutes to 1 hour |
| Admin Panel | CP | Strong | Admin actions (suspend seller, override config) must be immediately effective | 0 |
| Config Server | CP | Strong (with Vault) | Configuration must be consistent; misconfiguration = outage | 0 |

---


# Section 7: Java Spring Boot Code Snippets

All snippets target **Java 17+**, **Spring Boot 3.2.x**, **Spring Cloud 2023.0.x**.

**Maven BOM** (add to parent pom):
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>3.2.3</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2023.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## 7.1 Eureka Client Registration

```xml
<!-- pom.xml dependency -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
# bootstrap.yml — loaded before application.yml, sets service identity
spring:
  application:
    name: order-service   # This becomes the Eureka service name (app name)
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true     # Fail startup if config server unreachable

eureka:
  client:
    service-url:
      defaultZone: http://eureka-1:8761/eureka/,http://eureka-2:8761/eureka/
    registry-fetch-interval-seconds: 10
  instance:
    prefer-ip-address: true
    ip-address: ${POD_IP:127.0.0.1}
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

```java
package com.platform.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient  // Registers with Eureka; also enables lb:// URI scheme
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

## 7.2 Spring Cloud Gateway Routes (YAML + Java DSL)

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```java
package com.platform.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Order service: authenticated, rate-limited
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .filters(f -> f
                    .rewritePath("/api/v1/orders/(?<segment>.*)", "/v1/orders/${segment}")
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(jwtSubjectKeyResolver())
                    )
                    .circuitBreaker(config -> config
                        .setName("order-cb")
                        .setFallbackUri("forward:/fallback/orders")
                    )
                    .retry(config -> config.setRetries(2)
                        .setMethods(HttpMethod.GET))  // only retry GETs (safe)
                )
                .uri("lb://order-service")
            )
            // Product service: public read, no auth required
            .route("product-read", r -> r
                .path("/api/v1/products/**")
                .and().method(HttpMethod.GET)
                .filters(f -> f
                    .rewritePath("/api/v1/products/(?<segment>.*)", "/v1/products/${segment}")
                    .addResponseHeader("Cache-Control", "public, max-age=30")  // CDN-cacheable
                )
                .uri("lb://product-service")
            )
            .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // Token-bucket: 500 tokens/sec replenish, burst up to 1000
        return new RedisRateLimiter(500, 1000, 1);
    }

    @Bean
    public KeyResolver jwtSubjectKeyResolver() {
        // Rate limit by JWT subject (userId); fallback to IP for unauthenticated
        return exchange -> {
            String subject = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(subject != null ? subject :
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }
}
```

---

## 7.3 OpenFeign Client with Resilience4j

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-feign</artifactId>
</dependency>
```

```java
package com.platform.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(
    name = "inventory-service",
    configuration = InventoryFeignConfig.class,
    fallbackFactory = InventoryClientFallbackFactory.class
)
public interface InventoryClient {

    @PostMapping("/v1/inventory/reserve")
    ReservationResponse reserve(@RequestBody ReservationRequest request);

    @DeleteMapping("/v1/inventory/reservations/{reservationId}")
    void release(@PathVariable UUID reservationId); // compensating action
}

// Fallback factory: provides context-aware fallbacks including the exception
@Component
class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public ReservationResponse reserve(ReservationRequest request) {
                log.error("Inventory reserve failed, returning pending state", cause);
                return ReservationResponse.pending(request.getOrderId()); // saga handles pending
            }
            @Override
            public void release(UUID reservationId) {
                log.warn("Inventory release fallback for {}", reservationId); // will be retried via event
            }
        };
    }
}
```

---

## 7.4 Resilience4j CircuitBreaker @Bean + application.yml

```java
package com.platform.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    // Programmatic bean for complex CB configurations not expressible in YAML
    @Bean("paymentServiceCircuitBreaker")
    public io.github.resilience4j.circuitbreaker.CircuitBreaker paymentCB(
            CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(50)
            .minimumNumberOfCalls(20)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slowCallRateThreshold(80.0f)
            .recordExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(BusinessValidationException.class)
            .build();
        return registry.circuitBreaker("payment-service", config);
    }
}
```

```yaml
# application.yml — preferred for all standard configurations
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        slidingWindowSize: 30
        minimumNumberOfCalls: 15
        failureRateThreshold: 50
        waitDurationInOpenState: 20s
        permittedNumberOfCallsInHalfOpenState: 3
        registerHealthIndicator: true
  retry:
    instances:
      inventory-service:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2.0
        randomizedWaitFactor: 0.5    # jitter: ±50%
        retryExceptions: [java.io.IOException]
  bulkhead:
    instances:
      fraud-service:
        maxConcurrentCalls: 50
        maxWaitDuration: 100ms
  timelimiter:
    instances:
      fraud-service:
        timeoutDuration: 45ms
```

---

## 7.5 Kafka Producer with Outbox Pattern

```java
package com.platform.payment.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox",
    indexes = @Index(name = "idx_outbox_status", columnList = "status, created_at"))
public class PaymentOutboxEvent {

    @Id
    private UUID eventId = UUID.randomUUID();

    @Column(nullable = false)
    private String aggregateType;   // "Payment"

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;       // "PaymentCompleted"

    @Column(nullable = false)
    private String topic;           // "payment.events"

    @Lob
    @Column(nullable = false)
    private String payload;         // JSON-serialised event

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING | PUBLISHED | DEAD_LETTER

    private int retryCount = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant publishedAt;
}

@Service
@Transactional  // CRITICAL: outbox write is part of the same business transaction
public class PaymentApplicationService {

    private final PaymentRepository paymentRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public PaymentResponse charge(ChargeCommand cmd) throws Exception {
        Payment payment = Payment.charge(cmd);  // domain logic
        paymentRepo.save(payment);              // write 1: business state

        // Write 2: outbox event — atomic with write 1
        PaymentOutboxEvent outbox = new PaymentOutboxEvent();
        outbox.setAggregateType("Payment");
        outbox.setAggregateId(payment.getPaymentId());
        outbox.setEventType("PaymentCompleted");
        outbox.setTopic("payment.events");
        outbox.setPayload(objectMapper.writeValueAsString(
            new PaymentCompletedEvent(payment)));
        outboxRepo.save(outbox);

        return payment.toResponse();
    }
}
```

---

## 7.6 Kafka Idempotent Consumer

```java
package com.platform.order.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepo;
    private final OrderApplicationService orderService;

    @KafkaListener(
        topics = "payment.events",
        groupId = "order-service-payment-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional  // processedEvent write and business update are atomic
    public void onPaymentCompleted(PaymentCompletedEvent event,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.OFFSET) long offset) {

        String dedupKey = event.getPaymentId().toString();

        // Idempotency check: skip if already processed
        if (processedEventRepo.existsByDedupKey(dedupKey)) {
            log.debug("Skipping duplicate payment event {} at offset {}", dedupKey, offset);
            return;
        }

        // Business action
        orderService.confirmPayment(event.getOrderId(), event.getTransactionRef());

        // Record as processed — same transaction guarantees atomicity
        processedEventRepo.save(ProcessedEvent.builder()
            .dedupKey(dedupKey)
            .eventType("PaymentCompleted")
            .processedAt(Instant.now())
            .kafkaTopic(topic)
            .kafkaOffset(offset)
            .build());
    }
}
```

---

## 7.7 Spring Security JWT Resource Server

```java
package com.platform.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity   // activates @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())   // stateless API, no CSRF needed
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/liveness").permitAll()
                .requestMatchers("/actuator/health/readiness").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/orders/{orderId}").authenticated()
                .requestMatchers(HttpMethod.POST, "/v1/orders").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthConverter())
                )
                .authenticationEntryPoint(
                    (req, res, ex) -> res.sendError(401, "Unauthorised: " + ex.getMessage()))
            )
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // RS256: validate using public key from Auth/IAM service JWKS endpoint
        return NimbusJwtDecoder.withJwkSetUri("https://auth.platform.com/oauth2/jwks").build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("roles");   // custom claim in our JWT
        gac.setAuthorityPrefix("ROLE_");
        var conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(gac);
        return conv;
    }
}

// Method-level security example
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('BUYER') and @orderSecurityService.isOwner(authentication, #orderId)")
    public OrderDto getOrder(@PathVariable UUID orderId) { ... }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest req) { ... }
}
```

---

## 7.8 Redis Cart Session with Spring Data Redis

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.session</groupId>
  <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```java
package com.platform.cart.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import java.util.ArrayList;
import java.util.List;

@RedisHash(value = "cart", timeToLive = 86400L) // default TTL: 24 hours (guest cart)
public class RedisCart {

    @Id
    private String cartId;          // "guest:{sessionId}" or "user:{userId}"

    private List<CartItem> items = new ArrayList<>();

    private Instant lastUpdated = Instant.now();

    @TimeToLive                     // allows per-cart TTL override at runtime
    private Long ttl;               // null = use @RedisHash default; set explicitly for persistent carts

    public record CartItem(String productId, String sku, int quantity, BigDecimal unitPrice) {}

    public void addItem(CartItem item) {
        items.stream()
            .filter(i -> i.sku().equals(item.sku()))
            .findFirst()
            .ifPresentOrElse(
                existing -> items.set(items.indexOf(existing),
                    new CartItem(existing.productId(), existing.sku(),
                        existing.quantity() + item.quantity(), item.unitPrice())),
                () -> items.add(item)
            );
        this.lastUpdated = Instant.now();
    }
}
```

```yaml
spring:
  data:
    redis:
      host: redis-cluster.platform.internal
      port: 6379
      password: ${REDIS_PASSWORD}           # sourced from Vault
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 5
          max-wait: 100ms
      cluster:
        nodes: redis-1:6379,redis-2:6379,redis-3:6379
        max-redirects: 3
```

---

## 7.9 Redisson Distributed Lock for Coupon Enforcement

```xml
<dependency>
  <groupId>org.redisson</groupId>
  <artifactId>redisson-spring-boot-starter</artifactId>
  <version>3.27.2</version>
</dependency>
```

```java
package com.platform.promotions.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@Service
@Slf4j
public class CouponRedemptionService {

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;

    @Transactional
    public DiscountResult redeemCoupon(String couponCode, UUID userId, BigDecimal orderAmount) {

        // Distributed lock key: one lock per coupon code across all JVM instances
        RLock lock = redissonClient.getLock("coupon-redemption:" + couponCode);

        try {
            // waitTime=3s (how long to wait for lock), leaseTime=10s (auto-release on crash)
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new CouponLockTimeoutException(
                    "Coupon " + couponCode + " is being validated concurrently. Please retry.");
            }

            Coupon coupon = couponRepository.findByCodeForUpdate(couponCode) // SELECT FOR UPDATE
                .orElseThrow(() -> new CouponNotFoundException(couponCode));

            // Validate coupon rules
            if (coupon.isExpired()) throw new CouponExpiredException(couponCode);
            if (coupon.isExhausted()) throw new CouponExhaustedException(couponCode);
            if (coupon.isUserRestricted() && !coupon.isAllowedUser(userId))
                throw new CouponNotApplicableException(couponCode, userId);

            // Atomic increment usage and validate max usage
            coupon.incrementUsageCount();
            BigDecimal discount = coupon.calculateDiscount(orderAmount);
            couponRepository.save(coupon);

            log.info("Coupon {} redeemed by user {}, usage {}/{}", couponCode, userId,
                coupon.getUsageCount(), coupon.getMaxUsageCount());
            return new DiscountResult(discount, coupon.getDiscountType());

        } finally {
            // Always release, even on exception — prevents deadlock
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 7.10 Saga Orchestrator with Spring State Machine

See Section 2.4 for the complete `OrderSagaOrchestrator` implementation including state definitions, transitions, and action beans.

```xml
<dependency>
  <groupId>org.springframework.statemachine</groupId>
  <artifactId>spring-statemachine-core</artifactId>
  <version>3.2.1</version>
</dependency>
<dependency>
  <groupId>org.springframework.statemachine</groupId>
  <artifactId>spring-statemachine-data-jpa</artifactId>
  <version>3.2.1</version>
</dependency>
```

Saga state is persisted to `statemachine_state` table. On pod restart, in-flight sagas resume from last committed state. Each state transition is within a `@Transactional` boundary — business effect + state update are atomic.

---

## 7.11 Zipkin/Sleuth Custom Span

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
  <groupId>io.zipkin.reporter2</groupId>
  <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```java
package com.platform.payment.service;

import brave.Span;
import brave.Tracer;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {

    private final Tracer tracer;

    public PaymentResponse charge(ChargeRequest req) {
        // Create a child span for the payment processing step
        Span paymentSpan = tracer.nextSpan().name("payment.provider.charge");

        try (Tracer.SpanInScope scope = tracer.withSpan(paymentSpan.start())) {
            // Business metadata as span tags — queryable in Zipkin/Grafana Tempo
            paymentSpan.tag("payment.orderId", req.getOrderId().toString());
            paymentSpan.tag("payment.amount", req.getAmount().toPlainString());
            paymentSpan.tag("payment.currency", req.getCurrency());
            paymentSpan.tag("payment.method", req.getPaymentMethod().name());
            paymentSpan.tag("payment.idempotencyKey", req.getIdempotencyKey().toString());

            PaymentResponse response = paymentProvider.charge(req); // external PSP call

            paymentSpan.tag("payment.transactionRef", response.getTransactionRef());
            paymentSpan.tag("payment.status", "SUCCESS");

            return response;

        } catch (PaymentDeclinedException ex) {
            paymentSpan.tag("error", "true");
            paymentSpan.tag("payment.failureCode", ex.getFailureCode());
            paymentSpan.tag("payment.failureReason", ex.getMessage());
            paymentSpan.annotate("payment.declined"); // point-in-time event annotation
            throw ex;

        } finally {
            paymentSpan.finish(); // always finish spans, even on exception
        }
    }
}
```

```yaml
management:
  tracing:
    sampling:
      probability: 0.05   # 5% sampling for high-RPS services
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```

---


# Section 8: Performance Tuning & Scalability

## 8.1 Load Profile Table

| Service | Normal RPS | Black Friday RPS (4×) | Instances @ Normal | Instances @ Peak |
|---------|-----------|----------------------|-------------------|-----------------|
| API Gateway | 25,000 | 100,000 | 4 | 16 |
| Product (read) | 12,500 | 50,000 | 4 | 16 |
| Search | 7,500 | 30,000 | 4 | 12 |
| Cart | 10,000 | 40,000 | 4 | 16 |
| Recommendations | 6,250 | 25,000 | 2 | 8 |
| Inventory | 3,750 | 15,000 | 3 | 12 |
| Order | 1,250 | 5,000 | 3 | 12 |
| Payment | 500 | 2,000 | 3 | 8 |
| Notification | 7,500 | 30,000 | 3 | 12 |
| User | 5,000 | 20,000 | 2 | 8 |
| Promotions | 5,000 | 20,000 | 3 | 12 |
| Fraud Detection | 500 | 2,000 | 2 | 8 |
| Reviews | 2,500 | 10,000 | 2 | 8 |
| Shipping | 1,250 | 5,000 | 2 | 8 |
| Returns | 500 | 2,000 | 2 | 6 |
| Seller | 750 | 3,000 | 2 | 6 |

**Black Friday multiplier**: 4× baseline. Sustained for 4-hour sale window. Pre-warm instances 30 minutes before event start.

---

## 8.2 JVM Tuning

### G1GC (Throughput-Optimised: Order, Product, Cart)

```bash
# Order/Product/Cart: throughput-focused, heap 2–4 GB
JAVA_OPTS="-Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:G1HeapRegionSize=16m \
  -XX:G1NewSizePercent=20 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:G1MixedGCCountTarget=8 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+G1UseAdaptiveIHOP \
  -XX:+ExplicitGCInvokesConcurrent \
  -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m"
```

### ZGC (Latency-Sensitive: Payment, Fraud Detection, API Gateway)

```bash
# Payment/Fraud: P99 GC pause budget ≤ 1ms
JAVA_OPTS="-Xms3g -Xmx3g \
  -XX:+UseZGC \
  -XX:ZAllocationSpikeTolerance=5 \
  -XX:ZCollectionInterval=0 \
  -XX:+ZGenerational \             # Java 21+: Generational ZGC for better throughput
  -XX:+UnlockExperimentalVMOptions \
  -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m"
```

### Heap Sizing Formula

```
Per-container heap = min(container_memory_limit × 0.70, 4GB for stateless services)

For an 8 GB container (Tier 1): 8 × 0.70 = 5.6 GB → round to 4 GB heap (leave 4 GB for metaspace, off-heap, OS)
Use -Xms = -Xmx (avoid GC thrashing from heap growth)
```

---

## 8.3 HikariCP Connection Pool Tuning

```yaml
# application.yml per service tier
spring:
  datasource:
    hikari:
      # Formula: maximumPoolSize = (cpu_cores × 2) + effective_spindle_count
      # For 4-core pod against SSD PostgreSQL: (4×2) + 1 = 9 → round to 10
      maximum-pool-size: 10          # Tier 1 (Payment, Order, Inventory)
      minimum-idle: 3                # keep 3 connections warm
      connection-timeout: 3000       # fail fast: 3s to acquire a connection
      idle-timeout: 300000           # release idle connections after 5 min
      max-lifetime: 1800000          # recycle connections every 30 min (< PostgreSQL's idle timeout)
      keepalive-time: 60000          # heartbeat to prevent NAT/firewall termination
      validation-timeout: 1000       # connection validity check: 1s
      connection-test-query: SELECT 1
      pool-name: HikariPool-${spring.application.name}
      # Register metrics with Micrometer
      register-mbeans: true
      metrics-tracker-factory: com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
```

**PostgreSQL correlation**:
```
max_connections per PostgreSQL instance = 500
Pool size per service instance = 10
Max service instances in cluster = 16 (peak)
Pool headroom: 500 - (16 × 10) = 340 connections reserved for admin, replication, analytics
```

| Service Tier | max-pool-size | Justification |
|-------------|--------------|---------------|
| Tier 1 (Payment, Order, Inventory) | 10–15 | High write RPS, low latency required |
| Tier 2 (User, Product, Cart) | 8–10 | Mixed read/write |
| Tier 3 (Notification, Recommendations) | 5–8 | Mostly reads; async acceptable |

---

## 8.4 Kafka Tuning

### Producer Configuration

```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      acks: all               # wait for ISR acknowledgement (durability > throughput)
      retries: 2147483647     # effectively infinite retries (rely on delivery.timeout.ms)
      properties:
        enable.idempotence: true         # exactly-once producer semantics
        max.in.flight.requests.per.connection: 5  # with idempotence enabled, safe to use 5
        batch.size: 65536                # 64 KB batch — balance throughput vs latency
        linger.ms: 5                     # wait 5ms for batch to fill (throughput > latency)
        compression.type: lz4           # lz4: best compression/CPU tradeoff for high-throughput
        delivery.timeout.ms: 120000     # 2 min total delivery timeout
        max.block.ms: 10000             # fail fast if broker unreachable for 10s
```

### Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      auto-offset-reset: earliest      # start from beginning on new consumer group
      enable-auto-commit: false        # manual commit: only commit after successful processing
      properties:
        fetch.min.bytes: 65536         # wait for 64 KB before fetching (throughput batch)
        fetch.max.wait.ms: 500         # but don't wait more than 500ms (latency cap)
        max.poll.records: 100          # process up to 100 records per poll
        max.poll.interval.ms: 300000   # 5 min max processing time per batch
        session.timeout.ms: 45000      # coordinator considers consumer dead after 45s
        heartbeat.interval.ms: 15000   # heartbeat every 15s (< session.timeout / 3)
        isolation.level: read_committed # only read committed records (transactional producers)
```

### Throughput vs Latency Matrix

| Parameter | High Throughput Config | Low Latency Config |
|-----------|----------------------|-------------------|
| `linger.ms` | 20–50ms | 0ms |
| `batch.size` | 131072 (128 KB) | 16384 (16 KB) |
| `fetch.min.bytes` | 131072 | 1 |
| `max.poll.records` | 500 | 10 |
| Use case | Analytics, Notifications | Payment events, Fraud scoring |

---

## 8.5 Redis Cluster Sizing

### Memory Formula

```
Required memory = (avg_object_size_bytes × num_keys × replication_factor) × 1.3 (overhead)

Cart service example:
  avg cart: 2 KB × 10M daily active carts × 2 replicas × 1.3 = 52 GB
  → 6-shard cluster, 16 GB per shard = 96 GB total (comfortable headroom)

Session store:
  avg session: 512 bytes × 50M active sessions × 2 × 1.3 = 65 GB
  → 6-shard cluster, 16 GB per shard
```

### Eviction Policies

| Store | Eviction Policy | Rationale |
|-------|----------------|-----------|
| Cart (Redis) | `allkeys-lru` | Evict least-recently-used carts; data recoverable from PostgreSQL |
| Session store | `noeviction` | Sessions must not be silently lost (user forced logout) |
| Recommendations cache | `allkeys-lfu` | Evict least-frequently-used personalisation data; fallback available |
| Distributed locks | `noeviction` | Locks must never be silently evicted |

### Lettuce Connection Pool

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50      # max connections per JVM instance
          max-idle: 20        # keep 20 warm connections
          min-idle: 5
          max-wait: 100ms     # fail fast if pool exhausted
          time-between-eviction-runs: 60000ms
```

---

## 8.6 Elasticsearch Sizing

### Shard Strategy

```
Target shard size: 20–50 GB for warm data; 10 GB for hot (recent 7 days) data
products_v3 index: 50M documents × avg 1.5 KB = 75 GB compressed
→ 12 primary shards × 6.25 GB each (well within 20–50 GB target)

replica count:
  Search service (high read): 2 replicas per shard → 12 × 3 = 36 total shards
  Reviews index (moderate read): 1 replica → 8 × 2 = 16 total shards
```

### Index Lifecycle Management (ILM)

```json
{
  "policy": {
    "phases": {
      "hot":   { "min_age": "0ms",  "actions": { "rollover": { "max_size": "50gb", "max_age": "7d" } } },
      "warm":  { "min_age": "7d",   "actions": { "shrink": { "number_of_shards": 1 }, "forcemerge": { "max_num_segments": 1 } } },
      "cold":  { "min_age": "30d",  "actions": { "freeze": {} } },
      "delete":{ "min_age": "365d", "actions": { "delete": {} } }
    }
  }
}
```

### refresh_interval Tuning

```json
// During bulk indexing (e.g., initial load, reindex):
PUT /products_v3/_settings { "refresh_interval": "30s" }  // reduce I/O load

// Normal operation:
PUT /products_v3/_settings { "refresh_interval": "1s" }   // near-real-time

// For bulk API calls, set refresh=false and call POST /_refresh explicitly when done
```

---


# Section 9: Observability & SLA Governance

## 9.1 Three-Pillar Observability Stack

```
┌─────────────────────────────────────────────────────────────────┐
│                       METRICS PILLAR                            │
│  Spring Boot Actuator → Micrometer → Prometheus (scrape 15s)   │
│  → Grafana (dashboards) + Alertmanager (alerting rules)         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        LOGS PILLAR                              │
│  Logback (JSON) → Filebeat (agent) → Logstash (optional)        │
│  → Elasticsearch → Kibana (search + dashboards)                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       TRACES PILLAR                             │
│  Micrometer Tracing (Brave) → Zipkin / Grafana Tempo            │
│  → Grafana (trace exploration, linked to logs/metrics)          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9.2 Micrometer Metrics Standards

### Mandatory Metrics (every service must expose)

| Metric | Type | Labels | Alert Threshold |
|--------|------|--------|----------------|
| `http.server.requests` | Timer | service, endpoint, method, status, region | P99 > SLO |
| `jvm.memory.used` | Gauge | area (heap/nonheap), service | > 80% of max |
| `jvm.gc.pause` | Timer | cause, action, service | P99 > 20ms (Tier 1) |
| `hikaricp.connections.active` | Gauge | pool.name, service | > 90% of max |
| `hikaricp.connections.timeout` | Counter | pool.name | any increase |
| `kafka.consumer.fetch-manager.records-lag-max` | Gauge | topic, group, service | > 10,000 |
| `resilience4j.circuitbreaker.state` | Gauge | name, state | state = OPEN |
| `resilience4j.circuitbreaker.failure.rate` | Gauge | name | > 50% |

### Label Standards

```java
// All metrics MUST include these tags
MeterRegistry registry;

registry.timer("http.server.requests",
    "service", "order-service",
    "region", System.getenv("AWS_REGION"),
    "tenantId", TenantContext.getTenantId(),  // for multi-tenant filtering
    "endpoint", "/v1/orders",
    "method", "POST",
    "status", "201"
);
```

### Prometheus Scrape Configuration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'spring-boot-services'
    scrape_interval: 15s
    metrics_path: /actuator/prometheus
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: "true"
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
```

---

## 9.3 Structured Logging Standard

### JSON Log Format

```json
{
  "timestamp": "2024-01-15T14:30:45.123Z",
  "level": "INFO",
  "service": "order-service",
  "version": "2.3.1",
  "traceId": "abc123def456",
  "spanId": "789abc",
  "tenantId": "tenant-uuid",
  "userId": "user-uuid",
  "orderId": "order-uuid",
  "thread": "http-nio-8080-exec-3",
  "logger": "com.platform.order.service.OrderService",
  "message": "Order created successfully",
  "duration_ms": 45
}
```

### Logback Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

  <appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeContext>false</includeContext>
      <customFields>{"service":"${spring.application.name}","version":"${APP_VERSION}"}</customFields>
      <fieldNames>
        <timestamp>timestamp</timestamp>
        <message>message</message>
        <logger>logger</logger>
        <thread>thread</thread>
        <level>level</level>
      </fieldNames>
      <!-- Include MDC fields automatically -->
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>spanId</includeMdcKeyName>
      <includeMdcKeyName>tenantId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
      <includeMdcKeyName>orderId</includeMdcKeyName>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON_STDOUT"/>
  </root>
</configuration>
```

### Log Level Governance

| Service / Environment | Production | Staging | Development |
|----------------------|-----------|---------|-------------|
| Payment, Order, Inventory | WARN | INFO | DEBUG |
| All other services | INFO | DEBUG | DEBUG |
| Security events | WARN always | — | — |
| Audit events (Admin) | INFO always | — | — |

**Rule**: DEBUG logs must never contain PII, PAN, or raw credentials. Info logs may contain masked identifiers (last 4 digits of card, userId). Full PII only in audit logs in the compliance vault.

---

## 9.4 SLA Definitions Table

| Service | Availability SLO | P50 | P95 | P99 | Error Rate Budget | Measurement Window |
|---------|-----------------|-----|-----|-----|------------------|--------------------|
| Payment | 99.999% | 30ms | 100ms | 300ms | 0.01% | Rolling 30 days |
| Order | 99.99% | 20ms | 60ms | 150ms | 0.05% | Rolling 30 days |
| Inventory | 99.99% | 5ms | 20ms | 50ms | 0.05% | Rolling 30 days |
| User | 99.99% | 5ms | 20ms | 50ms | 0.05% | Rolling 30 days |
| Auth/IAM | 99.999% | 10ms | 40ms | 100ms | 0.01% | Rolling 30 days |
| Promotions | 99.99% | 15ms | 40ms | 100ms | 0.05% | Rolling 30 days |
| API Gateway | 99.99% | 5ms | 15ms | 40ms | 0.1% | Rolling 30 days |
| Product | 99.95% | 8ms | 30ms | 80ms | 0.1% | Rolling 30 days |
| Cart | 99.95% | 5ms | 15ms | 30ms | 0.1% | Rolling 30 days |
| Search | 99.95% | 15ms | 50ms | 100ms | 0.1% | Rolling 30 days |
| Recommendations | 99.9% | 3ms | 10ms | 20ms | 0.5% | Rolling 30 days |
| Notification | 99.9% | 500ms | 1500ms | 3000ms | 1.0% | Rolling 30 days |
| Reviews | 99.9% | 20ms | 60ms | 150ms | 0.5% | Rolling 30 days |
| Shipping | 99.95% | 50ms | 150ms | 500ms | 0.5% | Rolling 30 days |
| Returns | 99.95% | 30ms | 100ms | 300ms | 0.5% | Rolling 30 days |
| Seller | 99.95% | 20ms | 60ms | 150ms | 0.5% | Rolling 30 days |
| Fraud Detection | 99.99% | 15ms | 30ms | 50ms | 0.1% | Rolling 30 days |
| Reporting | 99.9% | 200ms | 1000ms | 3000ms | 1.0% | Rolling 30 days |
| Admin Panel | 99.9% | 50ms | 150ms | 500ms | 1.0% | Rolling 30 days |

---

## 9.5 Error Budget & Burn Rate Alerting

### Error Budget Calculation

```
Monthly error budget (minutes) = (1 - SLO) × 43,800 minutes

Payment (99.999%): 0.001% × 43,800 = 0.438 minutes (26 seconds) per month
Order (99.99%):    0.01% × 43,800 = 4.38 minutes per month
```

### Burn Rate Alerting (Google SRE model)

```yaml
# prometheus-alerts.yml
groups:
  - name: slo-burn-rate
    rules:
      # Payment service: fast burn (14× = 1 hour will exhaust 5.6% of monthly budget)
      - alert: PaymentServiceFastBurn
        expr: |
          sum(rate(http_server_requests_seconds_count{service="payment-service",status=~"5.."}[1h]))
          /
          sum(rate(http_server_requests_seconds_count{service="payment-service"}[1h]))
          > 14 * 0.0001  # 14× the error rate budget (0.01%)
        for: 5m
        labels:
          severity: page
          service: payment-service
        annotations:
          summary: "Payment service FAST BURN: error rate {{ $value | humanizePercentage }}"
          description: "At this rate, the monthly error budget will be exhausted in ~1 hour"
          runbook: "https://wiki.platform.com/runbooks/payment-fast-burn"

      # Payment service: slow burn (3× = 6 days ahead of budget exhaustion)
      - alert: PaymentServiceSlowBurn
        expr: |
          sum(rate(http_server_requests_seconds_count{service="payment-service",status=~"5.."}[6h]))
          /
          sum(rate(http_server_requests_seconds_count{service="payment-service"}[6h]))
          > 3 * 0.0001
        for: 30m
        labels:
          severity: ticket
          service: payment-service
        annotations:
          summary: "Payment service SLOW BURN: error rate {{ $value | humanizePercentage }}"
```

---

## 9.6 Health Check Standards

```java
// Custom health indicators
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            kafkaAdmin.describeTopics(List.of("payment.events")); // connectivity check
            return Health.up().withDetail("lag", consumerLagMonitor.getMaxLag()).build();
        } catch (Exception ex) {
            return Health.down().withException(ex).build();
        }
    }
}
```

```yaml
# Kubernetes probe config
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60    # give Spring Boot time to start
  periodSeconds: 30
  failureThreshold: 3
  timeoutSeconds: 5

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
  timeoutSeconds: 3
```

**Liveness**: Is the JVM alive? Checks: JVM is not deadlocked. Does NOT check dependencies.
**Readiness**: Can this pod serve traffic? Checks: DB connection pool has connections, Kafka consumer is assigned partitions, Redis is reachable, circuit breakers are not all OPEN.

---

## 9.7 Incident Escalation Playbook

**P1** (Payment down, Order creation failing, Platform-wide 5xx):
- **L1 Automated** (0–5 min): PagerDuty alert fires, Slack notification to `#incidents`, runbook link provided, automated diagnostics script runs (heap dump if OOM, thread dump if deadlock)
- **L2 On-Call Engineer** (5–15 min SLA): Acknowledge alert, follow runbook, attempt automated remediation (pod restart, circuit breaker force-open, traffic shed)
- **L3 Incident Commander** (15–30 min SLA): Declared for P1 not resolved by L2, bridge call opened, status page updated, executive notification
- **Post-Incident** (within 48h): 5-Why RCA, blameless postmortem document, corrective action items with owners and deadlines

**5-Why RCA Template**:
```
Incident: [brief description]
Impact: [customers affected, revenue impact, duration]
Timeline: [chronological events]
Why 1: Why did the outage occur? [immediate cause]
Why 2: Why did that happen? [contributing cause]
Why 3: Why was that allowed? [process failure]
Why 4: Why wasn't it caught earlier? [monitoring/alerting gap]
Why 5: Why wasn't the root condition prevented? [systemic gap]
Corrective Actions:
  - [Action] | [Owner] | [Due Date]
```

---


# Section 10: Data Architecture & Consistency

## 10.1 Database per Service Mandate

**Rationale**: Each service's database is an implementation detail, not a shared resource. Coupling services through a shared schema creates:
- Schema migration coordination overhead (all teams must agree on changes)
- Performance interference (one service's query affects another's latency)
- Security boundary violations (service A can accidentally read service B's sensitive data)
- Deployment coupling (schema changes require coordinated releases across services)

**Enforcement Mechanisms**:
- Network Policy: Each database pod has a Kubernetes NetworkPolicy `spec.ingress.from` restricted to exactly its owning service's pod selector label. No other pod can establish a TCP connection to the database port.
- Code review checklist: Any JPA entity that references another service's table fails the PR review.
- Service mesh policy: Istio AuthorizationPolicy denies database connections from non-owner services at Layer 4.

**Technology Selection by Service**:

| Service | Write Store | Read Store | Rationale |
|---------|------------|------------|-----------|
| User, Order, Payment, Inventory, Returns | PostgreSQL 15 | — (same DB, replica for reads) | ACID, strong consistency, mature tooling |
| Seller, Promotions, Admin | PostgreSQL 15 | — | ACID required |
| Product | PostgreSQL 15 | Elasticsearch | CQRS: complex search queries need ES |
| Cart | Redis (primary) + PostgreSQL (persistence) | Redis | Sub-10ms read requirement |
| Search | Elasticsearch only | — | Full-text search is the primary use case |
| Recommendations | Redis | Redis | Pre-computed, read-heavy, TTL-managed |
| Notification | PostgreSQL (templates, audit) | — | Simple reads; Kafka for dispatch |
| Reviews | PostgreSQL | Elasticsearch | Moderation in PG; search in ES |

**Permitted Exception**: A read-only analytics replica database may replicate a subset of tables from Order, Payment, and User services. This replica is:
- Non-transactional (SELECT only, no writes from analytics queries)
- Not used in any OLTP service-to-service call
- Subject to explicit data classification review before each new table inclusion
- Replicated with a minimum 1-hour lag (not near-real-time, to prevent OLTP impact)

---

## 10.2 Consistency Strategy per Service

| Service | CP/AP | Write Store | Read Store | Write Strategy | Staleness Tolerance |
|---------|-------|------------|------------|----------------|---------------------|
| Payment | CP | PostgreSQL primary | Same (replica lag = 0) | Synchronous write to primary; reads from primary for financial queries | 0 |
| Inventory | CP | PostgreSQL primary | Same | Serialisable isolation; optimistic lock; Redis pre-filter | 0 |
| Order | CP | PostgreSQL primary | Same | Synchronous; saga state persisted atomically | 0 |
| User | CP | PostgreSQL primary | Redis cache (5min TTL) | Write to primary; cache invalidated on mutation | 5 min (profile data), 0 (account status) |
| Product | AP | PostgreSQL primary | Elasticsearch | Async projection via Kafka consumer | 5–60 seconds |
| Cart | AP | Redis (TTL 24h) + PostgreSQL | Redis | Write-through to Redis; async persist to PostgreSQL | 5–30 seconds |
| Search | AP | Elasticsearch only | Same | Async Kafka consumer updates ES index | 10–120 seconds |
| Recommendations | AP | Redis only (pre-computed) | Same | Daily batch job warms cache | Up to 24 hours |
| Reviews | AP | PostgreSQL | Elasticsearch | Async projection after review approval | 10–60 seconds |

---

## 10.3 Outbox Pattern Implementation

See Section 2.5 (Payment Service) for full Outbox implementation with Debezium CDC variant.

**Debezium Connector Configuration**:
```json
{
  "name": "payment-outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "payment-db.platform.internal",
    "database.port": "5432",
    "database.user": "debezium_user",
    "database.password": "${DEBEZIUM_PASSWORD}",
    "database.dbname": "payment_db",
    "database.server.name": "payment-server",
    "table.include.list": "public.payment_outbox",
    "plugin.name": "pgoutput",
    "slot.name": "debezium_payment_outbox",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.by.field": "topic",
    "transforms.outbox.table.field.event.id": "event_id",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "payload"
  }
}
```

**Failure Recovery**: Debezium stores its WAL position in Kafka Connect offsets. On connector restart, it resumes from the last committed position. Events already published have `status = PUBLISHED`; Debezium will not re-emit them (WAL is read sequentially, not re-scanned).

---

## 10.4 Event Schema Evolution

### Schema Registry Setup

All Kafka events use Avro schemas registered with Confluent Schema Registry. Every producer validates against the registered schema before publishing; every consumer validates on deserialization.

```yaml
# Schema compatibility mode: BACKWARD (default)
# Consumers on v1 can read messages written by v2 producer
# Permitted changes:
#   ✓ Add optional field with default value
#   ✓ Remove a field (consumers ignore unknown fields if configured)
#   ✗ Change field type
#   ✗ Add required field without default
#   ✗ Rename field without alias
```

```json
// Avro schema: PaymentCompletedEvent v1
{
  "type": "record", "name": "PaymentCompletedEvent", "namespace": "com.platform.payment",
  "fields": [
    {"name": "paymentId",      "type": "string"},
    {"name": "orderId",        "type": "string"},
    {"name": "amount",         "type": {"type": "bytes", "logicalType": "decimal", "precision": 12, "scale": 2}},
    {"name": "currency",       "type": "string"},
    {"name": "transactionRef", "type": "string"},
    {"name": "settledAt",      "type": {"type": "long", "logicalType": "timestamp-millis"}}
  ]
}

// v2: add optional field (BACKWARD compatible)
{"name": "gatewayRef", "type": ["null", "string"], "default": null}
```

**Consumer Versioning Policy**:
- Consumers must ignore unknown fields (`IGNORE_UNKNOWN_FIELDS` in Jackson / Avro deserialiser option)
- Missing optional fields must be handled with documented defaults
- Schema version bump in a field rename requires a consumer release before the producer release

---

## 10.5 Data Retention & GDPR

### Retention Policy

| Data Category | Service | Retention | Legal Basis | Implementation |
|--------------|---------|-----------|-------------|----------------|
| Order records | Order | 7 years | Income Tax Act, GST compliance | PostgreSQL partitioned by year; old partitions archived to S3 + Parquet |
| Payment records | Payment | 5 years | PCI-DSS Requirement 3.4 | PostgreSQL; encrypted at rest (TDE) |
| Kafka events (hot) | All | 7 days | Operational replay | Kafka topic retention.ms = 604800000 |
| Kafka events (cold) | All | 1 year | Audit trail | S3 sink connector (Kafka Connect) |
| Application logs | All | 30 days (hot), 1 year (cold) | Operational | Elasticsearch ILM → S3 |
| User PII | User | Until deletion request + 30 days | GDPR Art. 17 | Soft delete → pseudonymisation |

### GDPR Right-to-Erasure (Pseudonymisation Strategy)

```sql
-- Erasure procedure (triggered 30 days after user deletion request)
-- Step 1: Generate pseudonym
INSERT INTO erasure_vault (user_id, pseudonym_id, erased_at)
VALUES (:userId, gen_random_uuid(), NOW());

-- Step 2: Replace PII in users table
UPDATE users SET
    email = 'erased-' || (SELECT pseudonym_id FROM erasure_vault WHERE user_id = :userId),
    phone = 'ERASED',
    display_name = 'Deleted User',
    deleted_at = NOW()
WHERE user_id = :userId;

-- Step 3: Replace PII in order shipping addresses (historical orders must be preserved for 7 years)
UPDATE order_addresses SET
    recipient_name = 'Deleted User',
    phone = 'ERASED',
    street = 'ERASED',
    -- city and pincode retained for analytics (non-PII at aggregate level)
WHERE user_id = :userId;

-- The erasure_vault is stored in a separate compliance database, not accessible to application services
```

---


# Section 11: Security Architecture

## 11.1 End-to-End Authentication & Authorisation Flow

```
1. Client authenticates with Auth/IAM (OAuth2 code flow or client_credentials)
   └─► Receives: access_token (JWT, RS256, 15min), refresh_token (7 days)

2. Client calls: POST /api/v1/orders
   Headers: Authorization: Bearer <JWT>
   └─► API Gateway receives request

3. API Gateway: JwtValidationFilter
   ├── Verify JWT signature (using Auth/IAM JWKS endpoint, cached 5min)
   ├── Verify exp, iss, aud claims
   ├── Inject X-User-Id, X-User-Roles, X-Tenant-Id headers from JWT claims
   └── Forward to Order Service (lb://order-service)

4. Order Service: Spring Security ResourceServer
   ├── OAuth2ResourceServer validates JWT again (defence in depth)
   ├── SecurityContextHolder populated with JwtAuthenticationToken
   └── @PreAuthorize("hasRole('BUYER')") at method level checks ROLE_BUYER

5. Order Service calls Inventory Service (OpenFeign, service-to-service):
   ├── PlatformFeignInterceptor injects:
   │   Authorization: Bearer <service-jwt> (client_credentials token, 1h TTL)
   │   X-Correlation-ID: <traceId>
   │   X-Tenant-Id: <tenantId from SecurityContext>
   └── Inventory Service validates the service JWT
```

---

## 11.2 mTLS Service-to-Service

### Certificate Lifecycle (cert-manager)

```yaml
# cert-manager Certificate resource (per service namespace)
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: order-service-mtls
  namespace: commerce
spec:
  secretName: order-service-tls
  issuerRef:
    name: platform-internal-ca
    kind: ClusterIssuer
  commonName: order-service.commerce.svc.cluster.local
  dnsNames:
    - order-service.commerce.svc.cluster.local
  duration: 720h     # 30 days
  renewBefore: 168h  # renew 7 days before expiry
```

### Istio mTLS Policy

```yaml
# Strict mTLS for all services in commerce namespace
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: commerce
spec:
  mtls:
    mode: STRICT  # reject all non-mTLS traffic
```

Spring Boot SSL configuration is handled by Istio Envoy sidecar — no application code changes required for mTLS in a service mesh environment.

---

## 11.3 Input Validation Standards

```java
// Controller-layer validation (Bean Validation JSR-380)
@PostMapping("/v1/orders")
public ResponseEntity<OrderDto> createOrder(
        @Valid @RequestBody CreateOrderRequest request) { ... }

// Request DTO with validation annotations
public record CreateOrderRequest(

    @NotNull(message = "Cart ID is required")
    @Size(min = 1, max = 1)
    List<@Valid OrderItemRequest> items,

    @NotNull UUID shippingAddressId,

    @Size(max = 50)
    String couponCode,

    @Valid PaymentMethodRequest paymentMethod
) {}

// Custom domain validator
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidIFSCValidator.class)
public @interface ValidIFSC {
    String message() default "Invalid IFSC code format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class ValidIFSCValidator implements ConstraintValidator<ValidIFSC, String> {
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return value != null && IFSC_PATTERN.matcher(value).matches();
    }
}
```

### RFC 7807 Problem Details Error Response

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex,
                                                    HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setType(URI.create("https://api.platform.com/errors/validation-failed"));
        pd.setTitle("Validation Failed");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("violations", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
            .toList());
        return pd;
    }
}
```

---

## 11.4 Secrets Management

**Mandate**: No secrets in:
- Source code (git-scanned by `truffleHog` in CI pipeline)
- Environment variables in plain text
- Docker images
- ConfigMaps (Kubernetes)

**Solution**: Spring Cloud Vault + Kubernetes auth backend.

```yaml
# bootstrap.yml
spring:
  cloud:
    vault:
      uri: https://vault.platform.internal:8200
      authentication: KUBERNETES
      kubernetes:
        role: order-service
        kubernetes-path: auth/kubernetes
      kv:
        enabled: true
        backend: secret
        profile-separator: /
        default-context: order-service
```

```java
// Access vault secrets as @Value
@Value("${database.password}")  // resolved from Vault at startup
private String dbPassword;

// Or inject directly
@VaultPropertySource("secret/order-service")
@Configuration
public class VaultConfig {}
```

**Secret Rotation**: Vault dynamic secrets for PostgreSQL credentials (Vault generates a new username/password, grants necessary privileges, and revokes after TTL). Spring Cloud Vault auto-renews leases before expiry.

---

## 11.5 PCI-DSS Controls for Payment Service

### PAN Tokenisation

The platform never stores raw PANs. On first use:
1. PAN transmitted over TLS from client
2. Payment Service sends to payment processor (Stripe/Razorpay) via HTTPS
3. Payment processor returns a `payment_method_token` (processor-side tokenisation)
4. Platform stores only `payment_method_token` + last 4 digits + card type

```java
// What is stored in payment_methods table:
// payment_method_id, processor_token, last_four, card_type, expiry_month/year, billing_address_id
// What is NEVER stored: PAN, CVV, PIN
```

### Audit Log Requirements

Every payment event is persisted immutably:
```sql
CREATE TABLE payment_audit_log (
    log_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id   UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    actor        VARCHAR(200) NOT NULL,   -- service name or user ID
    ip_address   INET,
    user_agent   TEXT,
    request_hash VARCHAR(64),             -- SHA-256 of request (not body, just hash)
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (occurred_at);
-- Append-only: no UPDATE/DELETE DML is granted to application user
-- Application DB user has only INSERT + SELECT on this table
```

### Network Segmentation

```yaml
# Kubernetes NetworkPolicy: Payment service namespace isolation
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-service-isolation
  namespace: payment
spec:
  podSelector:
    matchLabels:
      app: payment-service
  policyTypes: [Ingress, Egress]
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: api-gateway   # only API Gateway can reach payment service
        - namespaceSelector:
            matchLabels:
              name: order         # order service can call payment service
  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              name: payment-db    # payment DB only
    - ports:
        - port: 443               # external PSP HTTPS calls only
```

---

## 11.6 PII Minimisation for Fraud Detection

See Section 4.3 for the full PII pseudonymisation code. Key principle:

**Data Minimisation Checklist** (every field sent to Fraud Detection must pass this gate):
1. Is this field necessary for fraud scoring? (If no → remove it)
2. Can a non-PII derivative serve the same purpose? (e.g., velocity score instead of raw userId)
3. If raw value needed: is it pseudonymised with HMAC(field, secret) before leaving the payment domain?
4. Is the transmission over mTLS?
5. Is the Fraud Detection service in a separate namespace with NetworkPolicy?

Fields **permitted** in fraud scoring request: `order_id`, `amount`, `currency`, `payment_method_type`, `device_fingerprint_hash`, `ip_velocity_score`, `user_velocity_score`, `bin_country`, `is_first_transaction`

Fields **prohibited**: `email`, `phone`, `name`, `PAN`, `userId` (only `user_pseudonym_id` derived via HMAC)

---


# Section 12: Deployment Topology & DevOps

## 12.1 Kubernetes Resource Standards

### Resource Tiers

| Tier | Services | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|----------|------------|-----------|---------------|-------------|
| **Tier 1** | Payment, Order, Inventory | 2 CPU | 4 CPU | 4Gi | 8Gi |
| **Tier 2** | User, Product, Cart, Search, Promotions, Auth/IAM | 1 CPU | 2 CPU | 2Gi | 4Gi |
| **Tier 3** | Notification, Recommendations, Reviews, Shipping, Returns, Fraud Detection | 0.5 CPU | 1 CPU | 1Gi | 2Gi |
| **Tier 4** | Admin Panel, Reporting, Seller | 0.25 CPU | 0.5 CPU | 512Mi | 1Gi |

### HorizontalPodAutoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: commerce
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3    # Tier 1: always at least 3 for HA (spans 3 AZs)
  maxReplicas: 12   # Black Friday peak
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      pods:
        metric:
          name: http_server_requests_seconds_count  # custom Kafka/HTTP RPS metric
        target:
          type: AverageValue
          averageValue: "1000"   # scale when avg RPS per pod exceeds 1000
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2          # add at most 2 pods per 60s
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300  # wait 5min before scaling down
```

### PodDisruptionBudget (Tier 1)

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: payment-service-pdb
  namespace: payment
spec:
  maxUnavailable: 1   # at most 1 pod can be unavailable during cluster maintenance
  selector:
    matchLabels:
      app: payment-service
```

---

## 12.2 Blue-Green & Canary Release

### Blue-Green via Gateway Weight Routing

```yaml
# Spring Cloud Gateway: traffic split between blue (stable) and green (new version)
spring:
  cloud:
    gateway:
      routes:
        - id: product-green
          uri: lb://product-service-green
          predicates:
            - Path=/v1/products/**
            - Weight=product-group, 5          # 5% traffic to green

        - id: product-blue
          uri: lb://product-service-blue
          predicates:
            - Path=/v1/products/**
            - Weight=product-group, 95         # 95% traffic to blue (stable)
```

**Switchover sequence**: 0% → 5% → 25% → 50% → 100% green. Each step requires automated smoke test gate to pass.

### Canary Rollback Triggers

Automated rollback is triggered (by Argo Rollouts or Flagger) when, within a 5-minute observation window:
- Error rate in canary pods > 1% (vs. < 0.1% in stable)
- P99 latency in canary > 500ms (vs. < 100ms in stable)
- Any circuit breaker in the canary instance is OPEN

### Smoke Test Gate (per canary step)

```bash
#!/bin/bash
# Smoke test: run against canary endpoint, assert success rate
CANARY_ENDPOINT="https://api.platform.com/v1/products/test-product"
SUCCESS=0; TOTAL=20
for i in $(seq 1 $TOTAL); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" $CANARY_ENDPOINT)
  [ "$STATUS" = "200" ] && SUCCESS=$((SUCCESS+1))
done
RATE=$(echo "scale=2; $SUCCESS/$TOTAL*100" | bc)
echo "Smoke test pass rate: $RATE%"
[ $(echo "$RATE >= 95" | bc) = 1 ] && exit 0 || exit 1
```

---

## 12.3 CI/CD Pipeline (8 Stages)

```
Stage 1: Build & Unit Test
├── Tool: Maven 3.9 / Gradle 8.x
├── JUnit 5 + Mockito, AssertJ
├── Quality gate: test coverage ≥ 80% (JaCoCo)
└── Artefact: JAR / test reports

Stage 2: Code Quality
├── Tool: SonarQube 10.x
├── Quality gate: coverage ≥ 80%, 0 Critical issues, 0 Blocker issues
├── Technical debt ratio < 5%
└── OWASP security hotspots reviewed

Stage 3: Integration Test
├── Tool: Testcontainers (PostgreSQL, Redis, Kafka, Elasticsearch spun up in Docker)
├── Spring Boot @SpringBootTest with embedded services
└── Contract-based scenario tests

Stage 4: SAST / DAST
├── SAST: OWASP Dependency Check (CVE scan), Checkmarx / Semgrep
├── DAST: OWASP ZAP passive scan against staging deploy
└── Quality gate: 0 High/Critical CVEs in direct dependencies

Stage 5: Container Image Build & Scan
├── Docker BuildKit (multi-stage build, distroless base image)
├── Image scan: Trivy (CVE + secret detection) or Snyk
└── Quality gate: 0 Critical CVEs in final image layer

Stage 6: Staging Deploy + Smoke Test
├── Helm chart deploy to staging Kubernetes cluster
├── Automated smoke tests (health check, critical path)
└── Integration test suite against staging environment

Stage 7: Contract Test
├── Tool: Spring Cloud Contract (provider side)
├── Auto-generated consumer stubs validated against provider
├── Breaking change detection: any incompatible contract → pipeline fails
└── Contract version bumped in schema registry

Stage 8: Canary Deploy → Full Rollout
├── Argo Rollouts canary: 5% → 25% → 50% → 100%
├── Automated metrics gate at each step (see 12.2)
└── Slack notification on success / auto-rollback on failure
```

---

## 12.4 Contract Testing

**Provider (Spring Cloud Contract)**:
```groovy
// src/test/resources/contracts/order-service/shouldReturnOrderById.groovy
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return order by ID"
    request {
        method GET()
        url "/v1/orders/order-uuid-123"
        headers { header("Authorization": matching("Bearer .+")) }
    }
    response {
        status OK()
        body([orderId: "order-uuid-123", status: "CONFIRMED", totalAmount: 1999.00])
        headers { contentType(applicationJson()) }
    }
}
```

**Consumer (generated stub)**:
```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.platform:order-service:+:stubs:8080",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class CartServiceOrderContractTest {
    @Test
    void orderClientShouldParseOrderResponse() {
        OrderDto order = orderClient.getOrder(UUID.fromString("order-uuid-123"));
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
    }
}
```

Breaking changes (e.g., renaming `totalAmount` to `finalAmount`) cause the contract test to fail in the CI pipeline before the change reaches staging.

---

## 12.5 Multi-Region Topology

### Service Placement by Region

| Service | Region Mode | Write Region | Read Regions | Failover |
|---------|------------|-------------|-------------|---------|
| API Gateway | Active-Active | All | All | Automatic (DNS health check) |
| Auth/IAM | Active-Active (reads) / Active-Passive (writes) | 1 primary | All (cached tokens) | Manual write failover |
| Product | Active-Active | 1 primary | All | Automatic (AP, reads from any) |
| Inventory | Active-Passive | 1 primary | Primary only (CP) | Manual with DR drill |
| Order | Active-Passive | 1 primary | Primary only (CP) | Manual with DR drill |
| Payment | Active-Passive | 1 primary | Primary only (CP) | Manual with DR drill |
| Cart, Search, Recommendations | Active-Active | Any (AP) | Any | Automatic |

### Global Load Balancer Configuration

```
Route 53 / Cloudflare:
  - Latency-based routing: route to nearest healthy region
  - Health check: /actuator/health/readiness of API Gateway in each region
  - Failover TTL: 30 seconds (Region A down → all traffic to Region B within 30s)
```

### Cross-Region Kafka (MirrorMaker 2)

```yaml
# mm2 configuration (source: region-a, target: region-b)
clusters: region-a, region-b
region-a.bootstrap.servers: kafka-a-1:9092,kafka-a-2:9092
region-b.bootstrap.servers: kafka-b-1:9092,kafka-b-2:9092
region-a->region-b.enabled: true
region-a->region-b.topics: payment.events, order.events, inventory.events
region-a->region-b.groups: region-b-consumers
replication.factor: 3
refresh.topics.interval.seconds: 60
sync.group.offsets.enabled: true   # translate consumer group offsets for cross-region resume
```

**Lag Budget**: ≤ 500ms for critical event topics (payment, order, inventory). Monitored via `kafka.consumer.fetch-manager.records-lag-max` metric for MirrorMaker consumer groups.

---

## 12.6 Disaster Recovery

### RTO / RPO Targets

| Tier | Services | RTO | RPO | Mechanism |
|------|----------|-----|-----|-----------|
| Tier 1 | Payment, Order, Inventory | 30 min | 60 sec | PostgreSQL streaming replication + automated failover script + Kafka MirrorMaker 2 |
| Tier 2 | User, Product, Cart, Auth | 30 min | 5 min | Similar; slightly relaxed RPO for AP services |
| Tier 3 | Notification, Recommendations | 60 min | 30 min | Redis replication; Kafka consumer group rebalance on recovery |

### DR Mechanisms

**Automated**:
- PostgreSQL streaming replication: synchronous for Tier 1 (ack when replica confirms write) → RPO ≈ 0 but slight write latency increase
- Redis Enterprise Active-Active: CRDT replication, automatic conflict resolution
- Kafka MirrorMaker 2: continuous cross-region replication with offset sync

**Manual** (run by DR incident commander):
```bash
# 1. Promote PostgreSQL replica in secondary region
psql -c "SELECT pg_promote();" -h payment-db-secondary.region-b

# 2. Update application config to point to promoted DB (via Config Server)
# (hot-reload via Spring Cloud Bus takes ~30s)

# 3. Rebalance Kafka consumer groups
kafka-consumer-groups.sh --bootstrap-server kafka-b:9092 \
  --group order-service-payment-consumer --reset-offsets --to-latest --execute

# 4. Reverse MirrorMaker 2 direction (region-b → region-a becomes primary replication)
# 5. Update Route 53 primary region record to region-b
# 6. Clear alert
```

**Monthly DR Drill**: Chaos Engineering injection using Chaos Monkey for Spring Boot or Litmus Chaos. Tests:
- Pod kill of all Payment Service pods in region A
- Network partition between Order Service and its DB
- Kafka broker failure (1 of 3 brokers killed)
- Redis primary node failure

Results documented; drill pass criteria: RTO met, RPO met, data integrity verified.

---


---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

This feature is a documentation-generation system (the ARD itself). Property-based testing applies to the structural and numeric properties of the generated document — verifiable assertions that can be checked programmatically against the document's content.

---

### Property 1: Service Catalogue Completeness

*For any* service in the defined service catalogue (all 20+ services listed in Section 1.3), the service catalogue table SHALL contain an entry with all required fields: Service name, Domain, Bounded Context, Primary Responsibility, Tech Stack, CAP Classification, SLA (Availability), and Peak RPS. No service in the defined list may be absent from the table.

**Validates: Requirements 1.3**

---

### Property 2: Per-Service Section Completeness

*For any* service in the union of Core Services, Extended Services, and Enterprise Services as defined in the Glossary, the design document SHALL contain a dedicated section that includes all of the following sub-sections: Bounded Context, Domain Model or Architecture description, API Contract, Data Schema or configuration, Events Published/Consumed (where applicable), CAP Classification, and SLA Targets. A service section that is missing any of these sub-sections is non-compliant.

**Validates: Requirements 2.1, 3.1, 4.1**

---

### Property 3: SLA Thresholds are Within Bounds

*For any* Core Service SLA table entry in the design document, the numeric values SHALL satisfy:
- Availability SLO ≥ 99.95%
- P99 read latency ≤ 100ms
- P99 write latency ≤ 300ms

For Payment and Auth/IAM specifically: Availability ≥ 99.999%, P99 charge ≤ 300ms. Any SLA entry that violates these bounds is a documentation defect.

**Validates: Requirements 2.7, 9.4**

---

### Property 4: Fraud Detection Latency Bound

*For any* configuration of the Fraud Detection Service real-time scoring pipeline documented in the design, the stated P99 synchronous latency target SHALL be ≤ 50ms. Any documentation of a fraud scoring path that claims a P99 higher than 50ms is non-compliant with the architectural standard.

**Validates: Requirements 4.4**

---

### Property 5: Cross-Region Replication Lag Budget

*For any* critical data path (Order, Payment, Inventory) in the multi-region topology, the documented replication lag budget SHALL be ≤ 500ms. This applies to both PostgreSQL streaming replication and Kafka MirrorMaker 2 replication. Documenting a higher lag tolerance for critical data is a violation of the architecture principle.

**Validates: Requirements 4.7, 12.5**

---

### Property 6: Architectural Pattern Catalogue Completeness

*For any* pattern in the defined pattern catalogue (Saga-Choreography, Saga-Orchestration, CQRS, Event Sourcing, Outbox, Circuit Breaker, Bulkhead, Retry+Backoff, Idempotent Consumer, API Gateway, Strangler Fig, Sidecar/Service Mesh, Database per Service, Shared Database anti-pattern, 2PC anti-pattern), the design document SHALL contain a section with all 8 required template fields: Pattern Name, Problem Solved, Solution, Advantages, Trade-offs, Failure Scenarios, Anti-Patterns to Avoid, and Applicable Services. A pattern section with any missing field is non-compliant.

**Validates: Requirements 6.1**

---

### Property 7: CAP Classification Coverage

*For any* service in the 20+ service catalogue, the CAP Theorem classification table SHALL contain an entry with: CP or AP classification, the consistency model (strong, eventual, causal), a justification statement, and a staleness tolerance window. Services without a CAP classification entry are undocumented regarding consistency guarantees.

**Validates: Requirements 6.6**

---

### Property 8: Code Snippet Coverage

*For any* item in the required code snippet list (Eureka client registration, Gateway routes, OpenFeign+Resilience4j, CircuitBreaker @Bean, Kafka+Outbox, Kafka idempotent consumer, JWT resource server, Redis Cart, Redisson distributed lock, Saga orchestrator, Zipkin custom span), the design document SHALL contain a corresponding Java code block. A code block is identified by the presence of a fenced code block with language tag `java`. Any required item not covered by at least one Java code block is a documentation gap.

**Validates: Requirements 7.1**

---

### Property 9: Security-Sensitive Configuration Uses Placeholders

*For any* Java or YAML code snippet in the document that contains security-sensitive configuration (identified by presence of keywords: `password`, `secret`, `jwt`, `key`, `token`, `credential`), the value SHALL be a placeholder in the format `${VARIABLE_NAME}` or `${env.VARIABLE_NAME}` — never a literal string value. A snippet containing a literal secret value (e.g., `password: myPassword123`) is a documentation defect and a security risk.

**Validates: Requirements 7.4**

---

### Property 10: Load Profile Table Coverage

*For any* service listed in the Core Services or Extended Services sets, the load profile table in Section 8.1 SHALL contain an entry with Normal RPS, Black Friday RPS (with 4× multiplier), Instances at Normal, and Instances at Peak. A service missing from the load profile table has undefined scaling characteristics.

**Validates: Requirements 8.7**

---

### Property 11: DR Targets Meet Tier Requirements

*For any* Tier-1 service (Payment, Order, Inventory) in the disaster recovery documentation, the stated RTO SHALL be ≤ 30 minutes and the stated RPO SHALL be ≤ 60 seconds. Any DR documentation for a Tier-1 service that claims a higher RTO or RPO is non-compliant with the enterprise SLA requirement.

**Validates: Requirements 12.6**

---

## Error Handling

### Validation Error Handling

All API endpoints return RFC 7807 Problem Details on error:

```json
{
  "type": "https://api.platform.com/errors/insufficient-stock",
  "title": "Insufficient Stock",
  "status": 409,
  "detail": "Only 2 units of SKU ABC123 available; requested 5",
  "instance": "/v1/orders/checkout",
  "orderId": "uuid-here",
  "productId": "uuid-here",
  "availableQuantity": 2
}
```

### Service-Level Error Categories

| HTTP Status | Error Category | Retry? | Example |
|-------------|---------------|--------|---------|
| 400 | Client validation error | No | Invalid request body |
| 401 | Authentication error | No (re-authenticate) | Expired JWT |
| 403 | Authorisation error | No | Insufficient role |
| 404 | Resource not found | No | Unknown orderId |
| 409 | Business conflict | No | Insufficient stock, duplicate idempotency key |
| 422 | Business validation | No | Invalid coupon code |
| 429 | Rate limited | Yes (after Retry-After) | API rate limit exceeded |
| 500 | Internal server error | Yes (with backoff) | Unexpected exception |
| 502/503 | Downstream unavailable | Yes (circuit breaker) | Inventory service down |

### Kafka Consumer Error Handling

```java
@Bean
public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
    // 3 retries with 1s fixed backoff, then DLQ
    var recovery = new DeadLetterPublishingRecoverer(template,
        (rec, ex) -> new TopicPartition(rec.topic() + ".dlq", rec.partition()));
    var backoff = new FixedBackOff(1000L, 3L);
    return new DefaultErrorHandler(recovery, backoff);
}
```

---

## Testing Strategy

### Dual Testing Approach

This architecture document mandates a dual testing approach for all implementing services:

**Unit Tests** (example-based):
- Controller layer: MockMvc tests for each endpoint, verifying status codes, response structure, validation errors
- Service layer: Mockito-based tests for business logic, edge cases, compensation flows
- Domain model tests: state machine transitions, business rule enforcement
- Security: tests for authorised vs unauthorised access on each endpoint

**Property-Based Tests** (applicable where universal properties hold):

The property-based testing framework to use is **jqwik** (Java) for Spring Boot 3.x services.

```xml
<dependency>
  <groupId>net.jqwik</groupId>
  <artifactId>jqwik-spring</artifactId>
  <version>0.14.0</version>
  <scope>test</scope>
</dependency>
```

**PBT Configuration**: minimum 1000 iterations per property test (increased from default 100 due to the complexity of ecommerce domain edge cases).

**Tag format**: Each property test must be tagged with:
```java
// Feature: ecommerce-microservices-architecture, Property N: <property text>
@Property(tries = 1000)
@Tag("property-based")
void propertyN_description(@ForAll("validOrders") Order order) { ... }
```

**What to test with PBT** (based on prework analysis):
- Idempotency: `∀ request with idempotency key K, processing it twice produces the same outcome`
- Inventory invariant: `∀ sequence of reserve/release operations, quantityAvailable ≥ 0`
- Outbox atomicity: `∀ payment operations, either both DB record and outbox event exist, or neither does`
- Coupon exclusivity: `∀ exclusive coupon codes, at most 1 successful redemption exists in the DB after N concurrent attempts`
- Cart merge commutativity: `∀ (guestCart, persistentCart), merge(guest, persistent) produces the same item set as merge(persistent, guest)` (no item duplication)
- Kafka deduplication: `∀ event with eventId E, processing it N times produces the same final state as processing it once`
- JWT placeholder compliance: `∀ code snippet containing security keywords, no literal credential values present`

**Integration Tests** (example-based, with Testcontainers):
- End-to-end saga flows: Create Order → Reserve Inventory → Process Payment → Confirm
- Failure paths: Payment failure → inventory release → order cancellation
- Cross-service API contract compliance
- Database schema migration tests (Flyway migration forward + rollback)

**Contract Tests**: Spring Cloud Contract on all inter-service API calls (see Section 12.4).

**Performance Tests**: Gatling load tests for critical paths (Order creation, Checkout, Search) run nightly against staging environment. Pass criterion: P99 within SLO at 1.5× normal RPS.

---

*This ARD is a living document. All changes must be reviewed by the Principal Engineer guild and version-tagged. The architecture principles in Section 1.2 are binding — deviations require an Architecture Decision Record (ADR) with explicit trade-off justification.*

*Document Owner: Platform Architecture Guild*
*Review Cadence: Quarterly or upon major infrastructure change*
*Next Review: Q2 2025*
