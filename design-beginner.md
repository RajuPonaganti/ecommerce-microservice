- **Core Services** (6): User, Product, Inventory, Order, Payment, Notification
# Ecommerce Microservices — Beginner's Architecture Guide
# Learning Edition: Java Spring Boot

> **Who this is for**: Developers who are learning microservices from scratch.
> Every concept is explained step by step, starting from "what is a microservice?"
> Think of this as a textbook combined with a blueprint for building Amazon/Flipkart.
>
> **Tech Stack**: Java 17, Spring Boot 3.2, Spring Cloud 2023
> **Level**: Beginner → Intermediate (no prior microservices experience needed)

---

## Table of Contents

1. [What is a Microservice? (Start Here)](#1-what-is-a-microservice)
2. [The Big Picture — What We Are Building](#2-the-big-picture)
3. [Setting Up Your Java Development Environment](#3-setting-up-java)
4. [Creating Your First Spring Boot Microservice](#4-first-spring-boot-service)
5. [Service Discovery — How Services Find Each Other (Eureka)](#5-eureka-service-discovery)
6. [API Gateway — The Front Door of Your Platform](#6-api-gateway)
7. [Configuration Management — One Place for All Settings](#7-config-server)
8. [Core Services — Deep Dive with Examples](#8-core-services)
9. [How Services Talk to Each Other (OpenFeign)](#9-openfeign)
10. [Messaging — Kafka for Async Communication](#10-kafka)
11. [Resilience Patterns — What to Do When Things Break](#11-resilience)
12. [Security — Protecting Your Platform](#12-security)
13. [Data Management — Each Service Owns Its Data](#13-data)
14. [Design Patterns Explained Simply](#14-design-patterns)
15. [Observability — Knowing What's Happening](#15-observability)
16. [Deployment — Running on Kubernetes](#16-deployment)
17. [Learning Roadmap](#17-roadmap)

---


## 1. What is a Microservice?

### The Problem With "One Big Application" (Monolith)

Imagine you are building Amazon. You start with one Java application that handles:
- User login
- Product listings
- Shopping cart
- Orders
- Payments
- Notifications

This is called a **Monolith** — everything is in one codebase, one deployment.

**Problems that appear as you grow:**

| Problem | Real Example |
|---------|-------------|
| One bug in Payments crashes the whole site | A NullPointerException in payment code takes down the product page |
| You cannot scale just the part that is busy | On sale day, only the Order system is overloaded, but you must scale everything |
| 50 developers edit the same codebase | Team A's change breaks Team B's feature |
| Deployment is scary | Releasing a tiny bug fix means redeploying the entire 500,000-line application |

### The Microservices Solution

Instead of one big application, you split it into **small, independent services**. Each service:
- Does **one thing only** (e.g., the Payment Service only handles payments)
- Has its **own database** (Payment Service's data is private to it)
- **Runs independently** (you can restart the Notification Service without touching the Order Service)
- **Communicates over the network** (via HTTP APIs or messages)

```
MONOLITH                          MICROSERVICES
┌─────────────────────┐           ┌──────────┐  ┌──────────┐
│  User + Product +   │    →      │  User    │  │ Product  │
│  Order + Payment +  │           │ Service  │  │ Service  │
│  Notification       │           └──────────┘  └──────────┘
└─────────────────────┘           ┌──────────┐  ┌──────────┐
                                  │  Order   │  │ Payment  │
                                  │ Service  │  │ Service  │
                                  └──────────┘  └──────────┘
```

### Analogy: A Restaurant

Think of a restaurant:
- The **Chef** only cooks (Kitchen Service)
- The **Waiter** only takes orders and serves (Order Service)
- The **Cashier** only handles billing (Payment Service)
- Each person does their job independently

If the cashier is sick, the kitchen keeps cooking. That is **fault isolation** — the core idea of microservices.

---


## 2. The Big Picture — What We Are Building

We are building an ecommerce platform like Flipkart or Amazon. Here are all the pieces:

### Service Map (All 20+ Services)

```
┌─────────────────────────────────────────────────────────────────┐
│                    CUSTOMER USES                                │
│         Mobile App / Website / Seller Portal                   │
└──────────────────────────────┬──────────────────────────────────┘
                               │ (HTTPS)
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│              API GATEWAY  ← The single front door               │
│   Checks login tokens, routes requests, limits traffic          │
└──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────────────┘
       │      │      │      │      │      │      │
       ▼      ▼      ▼      ▼      ▼      ▼      ▼
  ┌────────┐ ┌──────┐ ┌─────┐ ┌──────┐ ┌───────┐ ┌──────┐
  │  User  │ │Prod- │ │Cart │ │Order │ │Payment│ │Search│
  │Service │ │uct   │ │     │ │      │ │       │ │      │
  └────────┘ └──────┘ └─────┘ └──────┘ └───────┘ └──────┘
  ┌────────┐ ┌──────┐ ┌─────┐ ┌──────┐ ┌───────┐ ┌──────┐
  │Invent- │ │Notif-│ │Ship-│ │Retur-│ │Promos │ │Fraud │
  │ory     │ │ication│ │ping │ │ns    │ │       │ │Det.  │
  └────────┘ └──────┘ └─────┘ └──────┘ └───────┘ └──────┘
```

### What Each Service Does (Plain English)

| Service | Job | Example |
|---------|-----|---------|
| **User Service** | Manages accounts | Register, login, update profile |
| **Product Service** | Manages product listings | Add a new phone, update price |
| **Inventory Service** | Tracks stock | "50 units of iPhone in Delhi warehouse" |
| **Order Service** | Manages orders | "Raju ordered 2 shirts" |
| **Payment Service** | Processes payments | Charge credit card, process refund |
| **Notification Service** | Sends emails/SMS/push | "Your order is shipped!" |
| **Cart Service** | Shopping basket | Add/remove items before buying |
| **Search Service** | Product search | "Show me red Nike shoes under ₹2000" |
| **Seller Service** | Manages sellers | Onboard a new seller, calculate payouts |
| **Shipping Service** | Delivery tracking | "Package picked up by BlueDart" |
| **Returns Service** | Return/refund requests | "I want to return this damaged phone" |
| **Promotions Service** | Discount coupons | Validate "SAVE20" coupon code |
| **Recommendations** | Suggested products | "Customers who bought this also bought..." |
| **Reviews Service** | Product reviews | Submit, moderate, display reviews |
| **Fraud Detection** | Spot fake orders | Block suspicious transactions |
| **Auth/IAM Service** | Login tokens (OAuth2) | Issue JWT tokens |
| **API Gateway** | Traffic routing | Route requests to correct service |
| **Config Server** | Central settings | Store database URLs, feature flags |
| **Eureka Server** | Service directory | "Order Service runs on IP 10.0.0.5:8080" |
| **Admin Panel** | Internal tools | Platform admin manages sellers |

---


## 3. Setting Up Your Java Development Environment

Before writing any code, you need these tools installed on your computer.

### Step 1: Install Java 17

Java 17 is the Long-Term Support (LTS) version we use. Think of LTS like a car model that gets
safety updates for 8 years — it is stable and well-supported.

**On Windows:**
1. Go to https://adoptium.net
2. Download "Eclipse Temurin 17 (LTS)"
3. Run the installer — it adds Java to your system PATH automatically
4. Verify: open Command Prompt, type `java -version`
   ```
   java version "17.0.10" 2024-01-16 LTS
   ```

**On Mac:**
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

### Step 2: Install Maven (Build Tool)

Maven is like a factory for your Java project. You describe what you want to build in a file
called `pom.xml`, and Maven downloads all dependencies and compiles your code.

**Download from:** https://maven.apache.org/download.cgi (Binary zip archive)

```bash
# Verify Maven is installed
mvn -version
# Apache Maven 3.9.6
```

### Step 3: Install an IDE (IntelliJ IDEA recommended)

IntelliJ IDEA is the industry standard for Java development. The Community Edition is free.
- Download: https://www.jetbrains.com/idea/download/

### Step 4: Install Docker Desktop

Docker lets you run databases (PostgreSQL, Redis) and other tools on your laptop without
installing them directly. Think of Docker containers like lightweight virtual machines.

- Download: https://www.docker.com/products/docker-desktop/

### Step 5: Quick Sanity Check

Create a file `Hello.java`:
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Java is working!");
    }
}
```

```bash
javac Hello.java    # compile
java Hello          # run
# Java is working!
```

---


## 4. Creating Your First Spring Boot Microservice

### What is Spring Boot?

Spring Boot is a framework (a ready-made toolkit) that makes building Java web applications fast.
Without Spring Boot, you would need to write hundreds of lines of configuration. Spring Boot
gives you **sensible defaults** so you can focus on your business logic.

**Analogy**: Building a house. Spring Boot is like a prefabricated house — walls, plumbing, and
wiring are already in place. You just add your furniture (business logic).

### Step 1: Generate Project from Spring Initializr

Go to https://start.spring.io and fill in:

```
Project:  Maven
Language: Java
Spring Boot: 3.2.3
Group:    com.ecommerce
Artifact: product-service
Java:     17

Dependencies to add:
  ✓ Spring Web          (to build REST APIs)
  ✓ Spring Data JPA     (to talk to database)
  ✓ PostgreSQL Driver   (database connector)
  ✓ Lombok              (reduces boilerplate code)
  ✓ Spring Boot Actuator (health checks)
```

Click **Generate** → downloads a ZIP → unzip → open in IntelliJ.

### Step 2: Understand the Project Structure

```
product-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/productservice/
│   │   │   ├── ProductServiceApplication.java  ← Main class (entry point)
│   │   │   ├── controller/                     ← Handle HTTP requests
│   │   │   ├── service/                        ← Business logic
│   │   │   ├── repository/                     ← Database access
│   │   │   └── model/                          ← Data objects (Product, etc.)
│   │   └── resources/
│   │       └── application.yml                 ← Configuration
├── pom.xml                                     ← Dependencies list
└── ...
```

### Step 3: The Main Application Class

```java
package com.ecommerce.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication is magic — it turns on everything Spring Boot offers
// It is shorthand for 3 annotations:
//   @SpringBootConfiguration  (this is a configuration class)
//   @EnableAutoConfiguration  (auto-configure based on dependencies)
//   @ComponentScan            (scan this package for components)
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        // This is the standard Java entry point
        // SpringApplication.run() starts the embedded web server (Tomcat)
        SpringApplication.run(ProductServiceApplication.class, args);
        // After this line, your service is running on http://localhost:8080
    }
}
```

### Step 4: Create a Product Model (What Data We Store)

```java
package com.ecommerce.productservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

// @Entity tells JPA (Java Persistence API) that this class maps to a database table
// JPA is an interface; Hibernate is the most popular implementation
@Entity
@Table(name = "products")  // maps to the "products" table in PostgreSQL
public class Product {

    // @Id = this is the primary key
    // @GeneratedValue = auto-generate the ID (we use UUID, not auto-increment numbers)
    // Why UUID? Because UUIDs are globally unique across all services and databases
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID productId;

    // @Column specifies the database column properties
    @Column(nullable = false, length = 500)  // nullable=false means NOT NULL in SQL
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)  // e.g., 999999999.99
    private BigDecimal price;

    @Column(nullable = false)
    private String category;

    // @Enumerated stores enum as a string in the DB (not a number)
    // Using STRING is safer — if you reorder enum values, DB is not affected
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.DRAFT;

    // Getters and setters — Lombok @Data annotation generates these automatically
    // Without Lombok you would write 20+ lines of boilerplate
    public UUID getProductId() { return productId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
}
```

```java
// Enum for product states
public enum ProductStatus {
    DRAFT,           // seller just created it, not visible yet
    PENDING_REVIEW,  // submitted for approval
    ACTIVE,          // visible to customers
    DISCONTINUED     // no longer for sale
}
```

### Step 5: Create a Repository (Database Access)

```java
package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

// JpaRepository gives you free CRUD methods:
//   save(product)         → INSERT or UPDATE
//   findById(id)          → SELECT WHERE id = ?
//   findAll()             → SELECT *
//   deleteById(id)        → DELETE WHERE id = ?
// Spring generates the SQL automatically — you write zero SQL for basic operations!

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Spring Data JPA reads the method name and generates SQL automatically!
    // findByCategory → SELECT * FROM products WHERE category = ?
    List<Product> findByCategory(String category);

    // findByStatusAndCategory → SELECT * FROM products WHERE status = ? AND category = ?
    List<Product> findByStatusAndCategory(ProductStatus status, String category);
}
```

### Step 6: Create a Service (Business Logic)

```java
package com.ecommerce.productservice.service;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

// @Service tells Spring this class contains business logic
// Spring will create one instance of this class and reuse it everywhere (Singleton)
@Service
public class ProductService {

    // Spring automatically injects ProductRepository here
    // This is called "Dependency Injection" — Spring manages object creation for you
    // Why? So you never write "new ProductRepository()" — Spring wires it all up
    private final ProductRepository productRepository;

    // Constructor injection — recommended way in Spring
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        product.setStatus(ProductStatus.DRAFT); // always start as draft
        return productRepository.save(product);
        // .save() runs: INSERT INTO products (product_id, title, price, ...) VALUES (...)
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByStatusAndCategory(ProductStatus.ACTIVE, category);
    }

    public Product getProduct(UUID productId) {
        // findById returns Optional<Product> — it might be empty if ID does not exist
        return productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        // orElseThrow — if Optional is empty, throw an exception
    }
}
```


### Step 7: Create a Controller (Handle HTTP Requests)

```java
package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

// @RestController = @Controller + @ResponseBody
// It means: handle HTTP requests AND automatically convert Java objects to JSON
@RestController
@RequestMapping("/v1/products")  // All endpoints in this class start with /v1/products
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // @PostMapping handles HTTP POST requests to /v1/products
    // @RequestBody reads the JSON from the request body and converts it to a Product object
    // ResponseEntity lets us control the HTTP status code (201 Created, not just 200 OK)
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        // Returns: HTTP 201, body = {"productId": "...", "title": "...", ...}
    }

    // @GetMapping handles HTTP GET requests to /v1/products/{productId}
    // @PathVariable extracts the {productId} from the URL
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
        // Returns: HTTP 200, body = the product as JSON
    }

    // @RequestParam reads query parameters from the URL
    // Example URL: /v1/products?category=Electronics
    @GetMapping
    public ResponseEntity<List<Product>> getProductsByCategory(
            @RequestParam String category) {
        List<Product> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
}
```

### Step 8: Configure application.yml

```yaml
# src/main/resources/application.yml
# YAML uses indentation (spaces, not tabs) to show hierarchy

spring:
  application:
    name: product-service    # This is the service's name — used everywhere

  datasource:
    url: jdbc:postgresql://localhost:5432/productdb    # PostgreSQL connection URL
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update    # 'update' = auto-create/modify tables based on your @Entity classes
                          # USE 'validate' in production — never let Hibernate modify prod DB!
    show-sql: true        # print SQL statements to console — helpful for learning

server:
  port: 8081              # Run on port 8081 (not 8080, so multiple services don't clash)

management:
  endpoints:
    web:
      exposure:
        include: health,info    # /actuator/health shows if service is running
```

### Step 9: Start PostgreSQL with Docker

Instead of installing PostgreSQL directly, use Docker:

```bash
# Start PostgreSQL in a Docker container
# -e sets environment variables (database name, user, password)
# -p 5432:5432 maps container port to your laptop port
# -d runs it in background (detached)
docker run --name productdb \
  -e POSTGRES_DB=productdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15

# Verify it is running
docker ps
```

### Step 10: Run Your Service

```bash
# In IntelliJ: click the green Run button on ProductServiceApplication.java
# OR from terminal:
mvn spring-boot:run

# Test with curl (command line) or Postman (GUI tool)
# Create a product:
curl -X POST http://localhost:8081/v1/products \
  -H "Content-Type: application/json" \
  -d '{"title": "iPhone 15", "price": 79999, "category": "Electronics"}'

# Get a product (replace the UUID with what was returned above):
curl http://localhost:8081/v1/products/abc-123-uuid-here
```

**Congratulations! You have your first microservice running.** 🎉

---


## 5. Eureka — Service Discovery (How Services Find Each Other)

### The Problem

When you have 20 microservices, each running on different servers, how does the Order Service
know where the Inventory Service is? You cannot hardcode IP addresses because:
- Services restart and get new IPs (especially in Kubernetes)
- You run multiple copies of the same service for load balancing
- IPs change when you deploy updates

### The Solution: Service Registry (Eureka)

Eureka is a phone book for your services:
1. When a service starts, it **registers** itself: "Hi, I am Inventory Service, I am at 10.0.0.5:8082"
2. When Order Service needs Inventory Service, it **asks Eureka**: "Where is Inventory Service?"
3. Eureka returns the current IP/port
4. Order Service calls Inventory Service directly

```
Inventory Service  →  "Register me!" →  Eureka Server
Order Service      →  "Where is Inventory?" → Eureka Server → "10.0.0.5:8082"
Order Service      →  HTTP call →  10.0.0.5:8082 (Inventory Service)
```

**Analogy**: Eureka is like a receptionist at a company. You ask "I need to speak with the Payments
team" and the receptionist tells you their room number.

### Setting Up Eureka Server

**Step 1: Create a new Spring Boot project** at start.spring.io with:
- Artifact: `eureka-server`
- Dependency: `Eureka Server`

**Step 2: Main class:**

```java
package com.ecommerce.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer  // This one annotation turns the app into a Eureka registry server
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**Step 3: application.yml for Eureka Server:**

```yaml
server:
  port: 8761    # Eureka's default port — all clients will look here

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false    # The server itself doesn't register with itself
    fetch-registry: false          # The server doesn't need to fetch a registry

  server:
    enable-self-preservation: true    # Don't remove services during network glitches
    # Self-preservation: if too many services stop sending heartbeats at the same time,
    # Eureka assumes there is a network problem (not that services died) and keeps them
    # in the registry. This prevents false removal during network partitions.
```

**Step 4: Run it.** Visit http://localhost:8761 — you see a dashboard showing registered services.

### Making Product Service Register with Eureka

**Step 1: Add Eureka Client dependency to product-service pom.xml:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    <!-- Version is managed by Spring Cloud BOM — no need to specify version here -->
</dependency>
```

**Step 2: Add Spring Cloud BOM to pom.xml (manages all Spring Cloud versions):**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.1</version>
            <type>pom</type>
            <scope>import</scope>
            <!-- BOM = Bill of Materials: a list of compatible versions for all Spring Cloud libraries.
                 By importing this, all spring-cloud-* dependencies get the right version automatically.
                 Never manually set versions for Spring Cloud dependencies — let the BOM handle it. -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Step 3: Add to application.yml:**

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/    # Where to find Eureka server
    registry-fetch-interval-seconds: 10             # Refresh registry every 10s
  instance:
    prefer-ip-address: true         # Register with IP address, not hostname
    # Why? Hostnames can be inconsistent in Docker/Kubernetes environments
    lease-renewal-interval-in-seconds: 10    # Heartbeat every 10s
    # Heartbeat: a signal the service sends to Eureka saying "I am still alive"
    # If Eureka doesn't hear from a service for 30s (lease-expiration), it removes it
    lease-expiration-duration-in-seconds: 30
```

**Step 4:** Add `@EnableDiscoveryClient` to your main class (or Spring Boot 3.x auto-detects it).

Restart product-service. Check http://localhost:8761 — you should now see "PRODUCT-SERVICE" listed!

---


## 6. API Gateway — The Front Door of Your Platform

### The Problem Without an API Gateway

Without a gateway, your mobile app must know where every service lives:
```
Mobile App → http://10.0.0.5:8081/v1/products   (Product Service)
Mobile App → http://10.0.0.6:8082/v1/orders     (Order Service)
Mobile App → http://10.0.0.7:8083/v1/users      (User Service)
```

Problems:
- The app needs to know 20 different addresses
- No central place to check if the user is logged in
- No central place to block bots (rate limiting)
- Changing a service's port breaks all clients

### The Solution: API Gateway

The Gateway is a single entry point. The app only knows **one address**.
The gateway receives all requests and routes them to the right service.

```
Mobile App → https://api.myecommerce.com/v1/products → Gateway → Product Service
Mobile App → https://api.myecommerce.com/v1/orders   → Gateway → Order Service
```

The gateway also:
- Checks login tokens (authentication)
- Limits requests per user (rate limiting)
- Logs all requests
- Handles CORS (browser security policy)

**Analogy**: The Gateway is the **security desk at a corporate office building**. Everyone signs in
at the front desk. The desk checks your ID badge, gives you a visitor pass, and tells you which
floor to go to. Nobody gets past the desk without being authenticated.

### Setting Up Spring Cloud Gateway

**Create a new project** at start.spring.io with:
- Artifact: `api-gateway`
- Dependencies: `Gateway`, `Eureka Discovery Client`

**Main class:**
```java
@SpringBootApplication
@EnableDiscoveryClient  // Gateway also registers with Eureka
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**application.yml — Route Configuration (YAML approach — easiest to understand):**

```yaml
server:
  port: 8080    # Gateway is the one service accessible from outside — standard port

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      routes:
        # Route 1: anything starting with /api/v1/products goes to product-service
        - id: product-service-route      # just a name for this route (can be anything)
          uri: lb://product-service      # lb:// means "load balance" across Eureka instances
                                         # Spring Cloud Gateway looks up "product-service"
                                         # in Eureka and picks an available instance
          predicates:
            - Path=/api/v1/products/**   # match URLs starting with this path
            # ** means "anything after" — so /api/v1/products/123 also matches
          filters:
            - RewritePath=/api/v1/products/(?<rest>.*), /v1/products/${rest}
            # RewritePath: strip the /api prefix before sending to Product Service
            # URL the client sends:    /api/v1/products/abc
            # URL Product Service gets:     /v1/products/abc

        # Route 2: orders go to order-service
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - RewritePath=/api/v1/orders/(?<rest>.*), /v1/orders/${rest}

      # Global filters apply to ALL routes
      default-filters:
        # Inject a unique ID into every request — useful for tracing through logs
        - AddRequestHeader=X-Correlation-ID, ${T(java.util.UUID).randomUUID()}
```

### How the Gateway Handles Authentication

When a user logs in, they get a **JWT token** (JSON Web Token). This token:
- Proves "I am Raju, user ID abc123, I have BUYER role"
- Is signed with a secret key (cannot be faked)
- Expires after 15 minutes (security measure)

The gateway checks this token on every request **before** forwarding to services:

```java
// src/main/java/.../filter/JwtValidationFilter.java
@Component
public class JwtValidationFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 1. Read the Authorization header: "Bearer eyJhbGciOiJSUzI1NiJ9...."
        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst("Authorization");

        // 2. If no token, reject immediately
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
            // Returns HTTP 401 — client must log in first
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            // 3. Verify the token's signature and decode its contents
            // jwtDecoder checks: is the signature valid? is it expired?
            Jwt jwt = jwtDecoder.decode(token);

            // 4. Pass useful information to downstream services via headers
            // Now Order Service knows who is making the request without checking the token again
            ServerWebExchange mutated = exchange.mutate()
                .request(r -> r
                    .header("X-User-Id", jwt.getSubject())           // "user-abc123"
                    .header("X-User-Roles", "BUYER")                 // from jwt claims
                    .header("X-Tenant-Id", jwt.getClaim("tenantId")) // for multi-seller
                )
                .build();

            return chain.filter(mutated); // Pass the modified request to the next filter/route

        } catch (JwtException ex) {
            // Token is invalid (wrong signature, expired, malformed)
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
```

### Rate Limiting — Preventing Abuse

Rate limiting means: "One user can make at most 500 requests per second".
This prevents bots from crashing your service.

```yaml
# In application.yml, add to a route's filters:
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 100    # Refill 100 tokens per second
      redis-rate-limiter.burstCapacity: 200    # Allow burst of 200 tokens
      # How it works (Token Bucket algorithm):
      # Imagine a bucket that holds tokens. Each request uses 1 token.
      # Tokens refill at 100/second. If bucket is empty, request is rejected (429 Too Many Requests)
      key-resolver: "#{@jwtKeyResolver}"       # Rate limit PER USER (identified by JWT)
```

This requires Redis running locally:
```bash
docker run --name redis -p 6379:6379 -d redis:7
```

---


## 7. Configuration Management — One Place for All Settings

### The Problem

Each service has an `application.yml`. If you have 20 services and need to change the database
URL, you must update 20 files and redeploy 20 services. This is error-prone and slow.

### The Solution: Spring Cloud Config Server

Config Server stores all configuration in one place (a Git repository). Services fetch their
configuration from Config Server at startup.

```
Git Repository (config files)
        ↓
Config Server (reads from Git)
        ↓
All 20 Services (fetch their config from Config Server at startup)
```

**Benefits:**
- Change one file in Git → all services pick up the change
- Different config per environment (dev, staging, production) in one place
- Secret values stored in Vault (not in code)

### Setting Up Config Server

**Create a project** with dependency `Config Server`:

```java
@SpringBootApplication
@EnableConfigServer    // One annotation turns this into a Config Server
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```yaml
# application.yml for Config Server
server:
  port: 8888    # Standard port for Config Server

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/youraccount/ecommerce-config    # Your config Git repo
          default-label: main    # Git branch to read from
```

### Config Repository Structure (Your Git Repo)

```
ecommerce-config/              (Git repository)
├── application.yml            ← Default settings for ALL services
├── application-dev.yml        ← Dev overrides for ALL services
├── application-prod.yml       ← Prod overrides for ALL services
├── product-service.yml        ← Settings specific to Product Service
├── product-service-dev.yml    ← Product Service dev overrides
├── order-service.yml          ← Settings specific to Order Service
└── payment-service-prod.yml   ← Payment Service PRODUCTION settings
```

**Priority Order (highest wins):**
`product-service-prod.yml` > `product-service.yml` > `application-prod.yml` > `application.yml`

### Pointing Services to Config Server

In each service, rename `application.yml` to `bootstrap.yml`
(bootstrap files load BEFORE application.yml, so Config Server is contacted first):

```yaml
# bootstrap.yml in product-service
spring:
  application:
    name: product-service    # Config Server uses this to find "product-service.yml"
  cloud:
    config:
      uri: http://localhost:8888    # Where to find Config Server
      fail-fast: true               # If Config Server is down, don't start the service
      # fail-fast: prevents a service starting with wrong/missing config
```

Add dependency:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

### Hot Reload — Change Config Without Restarting

With `@RefreshScope`, a bean re-creates itself when config changes (no restart needed):

```java
@Configuration
@RefreshScope    // This bean will reload when /actuator/refresh is called
public class FeatureFlags {

    // This value comes from Config Server
    // When you change it in Git and trigger a refresh, the new value loads automatically
    @Value("${feature.new-checkout-enabled:false}")
    private boolean newCheckoutEnabled;

    public boolean isNewCheckoutEnabled() {
        return newCheckoutEnabled;
    }
}
```

**How to trigger refresh without restarting:**
```bash
# Call this on any service instance to reload its config from Config Server
curl -X POST http://localhost:8081/actuator/refresh
```

With **Spring Cloud Bus + Kafka**, you can broadcast refresh to ALL instances at once:
```bash
curl -X POST http://localhost:8888/actuator/busrefresh
# This tells all services simultaneously: "Reload your config!"
```

---


## 8. Core Services — Deep Dive with Examples

### 8.1 Order Service — The Coordinator

The Order Service is the most complex service. When a customer places an order:
1. Check if items are in stock (calls Inventory Service)
2. Charge the customer (calls Payment Service)
3. Send confirmation email (calls Notification Service)
4. Create shipment (calls Shipping Service)

If step 2 fails (payment fails), step 1 must be undone (release the reserved stock).
This is called a **distributed transaction** — the hardest problem in microservices.

**Order domain model:**

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    private UUID userId;    // Who placed the order (reference to User Service)

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // OneToMany: one Order has many OrderItems
    // cascade ALL: when you save an Order, all its items are also saved
    // LAZY: don't load items from DB until they are actually accessed (performance)
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;

    private Instant createdAt = Instant.now();

    // The getters and setters...
}
```

```java
public enum OrderStatus {
    CREATED,              // Order received but not processed yet
    PAYMENT_PENDING,      // Waiting for payment confirmation
    PAYMENT_CONFIRMED,    // Payment succeeded
    INVENTORY_RESERVED,   // Stock is held for this order
    PROCESSING,           // Being prepared in warehouse
    SHIPPED,              // Handed to courier
    DELIVERED,            // Customer received it
    CANCELLED,            // Order cancelled
    REFUND_INITIATED      // Return in progress
}
```

### 8.2 Inventory Service — Preventing Overselling

**The critical problem**: Two customers buy the last iPhone at the same time.
Without proper locking, both orders succeed but there is only one phone. This is **overselling** —
a P0 (most critical) bug.

**Solution: Optimistic Locking with @Version**

```java
@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    private UUID stockItemId;

    private UUID productId;
    private int quantityOnHand;      // Total physical stock: 50 units
    private int quantityReserved;    // Reserved for pending orders: 10 units

    // @Version is the magic annotation for optimistic locking
    // Each time this row is updated, the version number increments
    // If two threads try to update the same row simultaneously, only ONE succeeds
    // The other gets an OptimisticLockException and must retry
    @Version
    private Long version;

    // Calculated: available stock = total - reserved
    public int getQuantityAvailable() {
        return quantityOnHand - quantityReserved;
    }

    public void reserve(int quantity) {
        if (getQuantityAvailable() < quantity) {
            throw new RuntimeException("Not enough stock!");
        }
        this.quantityReserved += quantity;
        // When JPA saves this: UPDATE stock_items SET quantity_reserved=?, version=version+1
        //                      WHERE stock_item_id=? AND version=?  ← version check!
        // If another thread already updated this row, the version changed,
        // so this UPDATE affects 0 rows → JPA throws OptimisticLockException
    }
}
```

**How to handle the OptimisticLockException:**

```java
@Service
public class InventoryService {

    // @Retryable: automatically retry up to 3 times if OptimisticLockException is thrown
    // This is from spring-retry library — add it to your pom.xml
    @Retryable(
        value = OptimisticLockException.class,    // only retry for this exception type
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
        // Retry delays: 100ms, 200ms, 400ms
        // Exponential backoff: each retry waits longer to reduce contention
    )
    @Transactional
    public StockReservation reserveStock(UUID productId, int quantity) {
        StockItem item = stockRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not in inventory"));

        item.reserve(quantity);
        stockRepository.save(item);    // This might throw OptimisticLockException

        return new StockReservation(item, quantity);
    }
}
```

### 8.3 Payment Service — Idempotency (Safe to Retry)

**The critical problem**: Network timeout. You sent "charge ₹999" to the Payment Service.
You got a timeout — did it charge or not? You do not know.
If you retry and it DID charge, the customer gets charged twice. **Catastrophic.**

**Solution: Idempotency Keys**

An idempotency key is a unique ID the client sends with each request.
The server records "I already processed this key" and returns the same result for duplicates.

```java
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    @PostMapping("/charge")
    public ResponseEntity<PaymentResponse> charge(
            // Client must provide this header on every charge request
            // UUID ensures it is unique: if the client generates it before calling,
            // they use the SAME UUID when retrying — server detects the duplicate
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody ChargeRequest request) {

        // Check if we already processed this request
        Optional<PaymentResponse> existingResponse =
            idempotencyService.findProcessedResponse(idempotencyKey);

        if (existingResponse.isPresent()) {
            // Already processed! Return the SAME response as before
            // Client gets their  answer, no double charge occurs
            return ResponseEntity.ok(existingResponse.get());
        }

        // Not seen before — process it
        PaymentResponse response = paymentProcessor.charge(request);

        // Save the response so future duplicates get the same answer
        idempotencyService.saveResponse(idempotencyKey, response);

        return ResponseEntity.ok(response);
    }
}
```

```java
// The idempotency records table (in PostgreSQL)
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private UUID idempotencyKey;

    private String responseJson;     // Cached response
    private Instant processedAt;

    // Records are kept for 24 hours, then cleaned up
    // Clients are expected to use fresh keys for genuinely new requests
}
```

---


## 9. How Services Talk to Each Other — OpenFeign

### The Problem

Order Service needs to call Inventory Service. You could use Java's `HttpURLConnection`
or Spring's `RestTemplate`, but you would write a lot of boilerplate code:
- Build the URL
- Set headers
- Serialize request to JSON
- Make the HTTP call
- Deserialize response from JSON
- Handle errors

For 50 different API calls, that is 50 sets of boilerplate code.

### The Solution: OpenFeign — Declarative HTTP Client

With OpenFeign, you define an **interface** that looks exactly like a regular Java interface.
OpenFeign generates the actual HTTP client code for you at runtime.

**Analogy**: OpenFeign is like a smart secretary. You say "call John, ask him if stock is
available". The secretary makes the actual phone call and brings back the answer. You just
define what you want, not how to do it.

**Step 1: Add dependency to pom.xml**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**Step 2: Enable Feign in your main class**

```java
@SpringBootApplication
@EnableFeignClients    // Scan for @FeignClient interfaces and generate implementations
public class OrderServiceApplication { ... }
```

**Step 3: Define the Feign client interface**

```java
package com.ecommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// @FeignClient(name = "inventory-service"):
//   - name = the Eureka service name (must match spring.application.name in Inventory Service)
//   - Feign looks up "inventory-service" in Eureka to get the actual URL
//   - No hardcoded URLs! Eureka provides the address dynamically
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    // This looks EXACTLY like a Spring MVC controller method
    // Feign uses these annotations to build the HTTP request
    @PostMapping("/v1/inventory/reserve")
    ReservationResponse reserve(@RequestBody ReservationRequest request);
    // Feign sends: POST http://inventory-service:8082/v1/inventory/reserve
    //              Body: {"productId": "...", "quantity": 2}

    @DeleteMapping("/v1/inventory/reservations/{reservationId}")
    void releaseReservation(@PathVariable UUID reservationId);
    // Feign sends: DELETE http://inventory-service:8082/v1/inventory/reservations/abc123
}
```

**Step 4: Use the Feign client in your service — it feels like a local method call**

```java
@Service
public class OrderService {

    private final InventoryClient inventoryClient;    // Feign-generated implementation
    private final PaymentClient paymentClient;

    // Spring injects the Feign-generated implementations
    public OrderService(InventoryClient inventoryClient, PaymentClient paymentClient) {
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    public Order createOrder(CreateOrderRequest request) {
        // This LOOKS like a local method call but it is actually an HTTP call to Inventory Service!
        // Feign handles URL building, JSON serialization, HTTP communication automatically
        ReservationResponse reservation = inventoryClient.reserve(
            new ReservationRequest(request.getProductId(), request.getQuantity()));
        // Behind the scenes: POST http://10.0.0.6:8082/v1/inventory/reserve
        //                    Body: {"productId": "abc", "quantity": 2}

        // Process payment...
        PaymentResponse payment = paymentClient.charge(
            new ChargeRequest(request.getUserId(), request.getTotalAmount()));

        // Create the order in our DB...
        Order order = new Order(request, reservation, payment);
        return orderRepository.save(order);
    }
}
```

### Propagating Headers Across Services

When Order Service calls Inventory Service, you need to pass:
- The user's JWT token (so Inventory knows who is calling)
- The Correlation ID (for tracing the request across services in logs)
- The Tenant ID (for multi-seller support)

Use a **RequestInterceptor** — it runs before every Feign HTTP call:

```java
@Component
public class PlatformFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // MDC = Mapped Diagnostic Context — a thread-local map for contextual logging data
        // When the request came in to Order Service, we stored traceId in MDC
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            // Add this header so Inventory Service can log the same traceId
            // This connects the log entries across services
            template.header("X-Correlation-ID", traceId);
        }

        // Pass the tenant ID so Inventory filters data by the correct seller
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            template.header("X-Tenant-Id", tenantId);
        }

        // Service-to-service authentication: pass a JWT token
        // This token was obtained via OAuth2 client_credentials flow
        // (explained in Security section)
        String serviceToken = tokenProvider.getServiceToken();
        template.header("Authorization", "Bearer " + serviceToken);
    }
}
```

### Handling Feign Errors — ErrorDecoder

When Inventory returns HTTP 409 (stock insufficient), Feign by default throws a generic
`FeignException`. You want a meaningful domain exception instead:

```java
@Component
public class InventoryErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        // Map HTTP status codes to domain-specific exceptions
        switch (response.status()) {
            case 409:
                return new InsufficientStockException(
                    "Not enough stock available for the requested product");
            case 404:
                return new ProductNotFoundException(
                    "Product does not exist in inventory");
            case 503:
                return new InventoryServiceUnavailableException(
                    "Inventory Service is temporarily unavailable");
            default:
                return new Default().decode(methodKey, response);
        }
    }
}
```

Register it with the Feign client configuration:

```java
@Configuration
public class InventoryFeignConfig {
    @Bean
    public ErrorDecoder inventoryErrorDecoder() {
        return new InventoryErrorDecoder();
    }
}

// Apply to the FeignClient:
@FeignClient(name = "inventory-service", configuration = InventoryFeignConfig.class)
public interface InventoryClient { ... }
```

---


## 10. Kafka — Messaging for Async Communication

### Why Not Just Use HTTP for Everything?

HTTP (synchronous) means: Order Service WAITS for Notification Service to respond.
If Notification Service is slow or down, Order Service is also stuck. The customer waits.

**The problem with synchronous chains:**
```
Order Service → calls → Payment Service (200ms wait)
             → calls → Inventory Service (100ms wait)
             → calls → Notification Service (500ms wait — email server slow!)
             → calls → Shipping Service (200ms wait)
Total wait time: 1 second!
```

If Notification Service crashes, the whole order creation fails.

### The Solution: Kafka (Asynchronous Messaging)

Kafka is a **message broker** — a middleman that holds messages.

Instead of calling Notification Service directly:
1. Order Service **publishes** an event: "Order #123 was placed"
2. Notification Service **subscribes** and processes the event in its own time
3. Order Service does NOT wait — it continues immediately

```
Order Service
  │
  │── publishes "OrderCreated" event ──► Kafka ──► Notification Service processes it later
  │                                              ► Search Service updates index later
  │                                              ► Analytics records the order later
  │
  └── continues immediately (total response time: much faster!)
```

**Analogy**: Instead of calling someone on the phone (synchronous, you wait), you put a note
in their mailbox (async, they read it when ready). You continue with your day.

### Key Kafka Concepts

| Concept | What it is | Analogy |
|---------|-----------|---------|
| **Topic** | Named channel for messages | A WhatsApp group chat |
| **Producer** | Service that sends messages | Person sending a message |
| **Consumer** | Service that reads messages | Person reading the message |
| **Partition** | Topic split for parallelism | Multiple post boxes in one building |
| **Offset** | Position in a partition | Page number in a book |
| **Consumer Group** | Multiple consumers sharing work | Office team sharing email inbox |

### Start Kafka with Docker Compose

Create a file `docker-compose.yml`:

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    # Zookeeper manages Kafka cluster metadata — like a manager tracking which brokers exist
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"    # Auto-create topics (good for dev)
```

```bash
docker-compose up -d    # Start Kafka in background
```

### Publishing Events — Order Service Producing

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092    # Where Kafka is running
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      # key-serializer: how to convert the message KEY to bytes
      # (Keys are used for partitioning — all messages with the same key go to same partition)
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      # value-serializer: converts Java object to JSON bytes
```

```java
@Service
public class OrderEventPublisher {

    // KafkaTemplate is Spring's helper class for sending messages to Kafka
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getOrderId(),
            order.getUserId(),
            order.getItems(),
            order.getTotalAmount(),
            Instant.now()
        );

        // Send to "order.events" topic
        // Key = orderId (ensures all events for the same order go to the same partition
        //       which guarantees ordering for that order)
        kafkaTemplate.send("order.events", order.getOrderId().toString(), event);
        // This is FIRE AND FORGET — Order Service does not wait for delivery confirmation
        // In production, you add error handling (see Outbox Pattern below)
    }
}
```

### Consuming Events — Notification Service Listening

```yaml
# application.yml in notification-service
spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:9092
      group-id: notification-service    # Consumer group ID — unique per service
      # All instances of notification-service share this group ID
      # Kafka distributes partitions among group members for parallel processing
      auto-offset-reset: earliest
      # earliest: when this consumer group first starts, read from the very beginning
      # latest: only read new messages published after this consumer starts
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.ecommerce.*"    # Security: only deserialize these packages
```

```java
@Component
public class OrderEventConsumer {

    private final EmailService emailService;

    // @KafkaListener: Spring automatically calls this method when a new message arrives
    // on the "order.events" topic for our consumer group "notification-service"
    @KafkaListener(
        topics = "order.events",
        groupId = "notification-service"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        // This runs in a background thread — not blocking Order Service at all!
        System.out.println("Received order event: " + event.getOrderId());

        emailService.sendOrderConfirmation(event);
        // If this fails, the Kafka offset is NOT committed
        // Kafka will re-deliver the message — retry is built in!
    }
}
```

### The Outbox Pattern — Guaranteed Event Publishing

**The problem**: Order Service saves an order AND publishes an event. What if Kafka is down
when we try to publish? The order is saved but the event is never published.
Notification never fires. Inventory never knows about the order. **Silent data loss.**

**The Outbox Pattern solution:**
1. Save the order AND an "outbox event" in the **same database transaction**
2. A separate process reads the outbox table and publishes to Kafka
3. If Kafka is down, the outbox accumulates events and publishes them when Kafka recovers

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID eventId = UUID.randomUUID();

    private String topic;       // "order.events"
    private String eventType;   // "OrderCreated"
    private String payload;     // JSON of the event data
    private String status = "PENDING";    // PENDING → PUBLISHED
    private Instant createdAt = Instant.now();
    private Instant publishedAt;
}

@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;

    // @Transactional: everything inside this method is ONE database transaction
    // Either BOTH the order AND the outbox event are saved, or NEITHER is
    // This atomicity guarantee is the whole point of the Outbox Pattern
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Save order
        Order order = new Order(request);
        orderRepository.save(order);

        // Save outbox event IN THE SAME TRANSACTION
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setTopic("order.events");
        outboxEvent.setEventType("OrderCreated");
        outboxEvent.setPayload(toJson(new OrderCreatedEvent(order)));
        outboxRepository.save(outboxEvent);

        // Transaction commits here — BOTH records are saved together
        // Even if Kafka is down, the outbox event is safely in the database
        return order;
    }
}

// Separate publisher — reads outbox and publishes to Kafka
@Component
public class OutboxEventPublisher {

    @Scheduled(fixedDelay = 1000)    // Run every 1 second
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
            .findByStatusOrderByCreatedAt("PENDING");

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPayload()).get();
                // .get() makes it synchronous — wait for Kafka to confirm receipt

                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                // Kafka unavailable — leave event as PENDING, retry next second
                event.incrementRetryCount();
            }
        }
    }
}
```

---


## 11. Resilience Patterns — What to Do When Things Break

In microservices, **things will fail**. Services crash, networks time out, databases slow down.
Your platform must stay running even when individual parts fail.

### 11.1 Circuit Breaker Pattern

**The problem**: Inventory Service is very slow (taking 30 seconds per call).
Every Order Service thread is waiting for Inventory. After 100 concurrent orders, all 100
threads are stuck waiting. Order Service is now down too — because of Inventory's problem.
This is called **cascading failure**.

**Analogy**: A circuit breaker in your home. When too much electricity flows (short circuit),
the breaker OPENS and stops the flow. Your TV doesn't blow up. You fix the problem and
CLOSE the breaker.

**In microservices — the Circuit Breaker has 3 states:**

```
CLOSED (normal)    →  [too many failures]  →  OPEN (fast fail)
    ↑                                              ↓
    └──── [test call succeeds] ← HALF_OPEN ←──────┘
                                (trying recovery)
```

- **CLOSED**: All calls pass through normally (circuit is closed = current flows)
- **OPEN**: Fast-fail ALL calls immediately without trying Inventory — returns error to caller
- **HALF_OPEN**: After waiting, allow a few test calls. If they succeed → back to CLOSED

**Setup with Resilience4j:**

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:    # Name matches the @CircuitBreaker annotation
        sliding-window-size: 20         # Look at last 20 calls
        minimum-number-of-calls: 10     # Need at least 10 calls before deciding
        failure-rate-threshold: 50      # If 50%+ calls fail → OPEN the circuit
        wait-duration-in-open-state: 30s # Stay OPEN for 30 seconds before trying again
        permitted-calls-in-half-open-state: 3  # Allow 3 test calls in HALF_OPEN
        # In English: "If more than 50% of the last 20 calls to inventory-service fail,
        #              open the circuit for 30 seconds, then test with 3 calls"
```

```java
@Service
public class OrderService {

    private final InventoryClient inventoryClient;

    // @CircuitBreaker: wraps this method with a circuit breaker
    // name = "inventory-service" matches the config above
    // fallbackMethod = method to call when circuit is OPEN
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "inventoryFallback")
    public ReservationResponse reserveStock(UUID productId, int quantity) {
        return inventoryClient.reserve(new ReservationRequest(productId, quantity));
        // If circuit is OPEN, this line is SKIPPED
        // inventoryFallback is called instead — immediately, no waiting
    }

    // Fallback method — same parameters PLUS the exception
    // Called when: circuit is OPEN, or the call fails with an exception
    public ReservationResponse inventoryFallback(UUID productId, int quantity, Exception ex) {
        // What to do when Inventory is down?
        // Option 1: Reject the order with a friendly message
        throw new ServiceUnavailableException("Inventory is temporarily unavailable. Please try again.");
        // Option 2: Queue the order and process later
        // Option 3: Return a "provisional" reservation and confirm asynchronously
    }
}
```

### 11.2 Retry Pattern

Some failures are **transient** (temporary). A quick network blip, a brief overload.
For these, retry the call instead of failing immediately.

**Important**: Only retry **idempotent** operations! (Operations safe to run multiple times)
- GET (read data) → safe to retry
- POST /orders → NOT safe (might create duplicate orders) — use idempotency keys first
- DELETE → safe to retry (deleting something twice has the same effect)

```yaml
resilience4j:
  retry:
    instances:
      inventory-service:
        max-attempts: 3           # Try at most 3 times total
        wait-duration: 500ms      # Wait 500ms before first retry
        enable-exponential-backoff: true     # Each retry waits longer
        exponential-backoff-multiplier: 2.0  # 500ms → 1000ms → 2000ms
        randomized-wait-factor: 0.5          # Add random jitter: ±50%
        # Jitter prevents "thundering herd": if 1000 services all retry at exactly 500ms,
        # the recovering service gets slammed. Jitter spreads retries over time.
        retry-exceptions:
          - java.io.IOException              # Network error — safe to retry
          - java.net.SocketTimeoutException  # Timeout — safe to retry
        ignore-exceptions:
          - com.ecommerce.InsufficientStockException  # Business logic — don't retry!
          # No point retrying — stock won't appear just because you asked again
```

```java
// Stack multiple annotations — they wrap in order: TimeLimiter → CircuitBreaker → Retry → Bulkhead
@CircuitBreaker(name = "inventory-service", fallbackMethod = "inventoryFallback")
@Retry(name = "inventory-service")
public ReservationResponse reserveStock(UUID productId, int quantity) {
    return inventoryClient.reserve(new ReservationRequest(productId, quantity));
    // Retry fires first (3 attempts with backoff)
    // If all retries fail, Circuit Breaker records the failure
    // If enough failures, CB opens and future calls fast-fail
}
```

### 11.3 Bulkhead Pattern

**The problem**: If Fraud Detection is slow, it hogs all the thread pool threads in Order Service.
Now no threads are available for Inventory or Payment calls — they also fail.
One slow service causes ALL service calls to fail.

**Analogy**: A ship's hull is divided into watertight compartments (bulkheads).
If one compartment floods, the others stay dry. The ship stays afloat.

**Solution**: Give each downstream service its own dedicated thread pool:

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      fraud-service:
        max-thread-pool-size: 20    # Only 20 threads can call Fraud Service at once
        core-thread-pool-size: 10   # Keep 10 threads ready
        queue-capacity: 50          # Queue up to 50 waiting requests
        # If > 50 requests are waiting AND 20 threads are busy → BulkheadFullException
        # This protects other service calls (Inventory, Payment) from being starved

      inventory-service:
        max-thread-pool-size: 30    # Inventory is critical — give it more threads
        core-thread-pool-size: 15
        queue-capacity: 100
```

```java
// Use THREADPOOL type for separate thread pools
@Bulkhead(name = "fraud-service", type = Bulkhead.Type.THREADPOOL)
@CircuitBreaker(name = "fraud-service", fallbackMethod = "fraudFallback")
public CompletableFuture<FraudScore> checkFraud(OrderRequest order) {
    return CompletableFuture.supplyAsync(() -> fraudClient.score(order));
    // This runs in the fraud-service thread pool (not the main request thread)
    // Even if all 20 fraud-service threads are busy, Inventory Service threads are unaffected
}
```

### 11.4 Timeout Pattern

Always set a maximum wait time. Never let a call hang forever.

```yaml
resilience4j:
  timelimiter:
    instances:
      fraud-service:
        timeout-duration: 50ms    # Fraud check MUST complete in 50ms
        cancel-running-future: true    # Cancel the async thread if it takes too long
```

```java
@TimeLimiter(name = "fraud-service")
@Bulkhead(name = "fraud-service", type = Bulkhead.Type.THREADPOOL)
public CompletableFuture<FraudScore> checkFraud(OrderRequest order) {
    return CompletableFuture.supplyAsync(() -> fraudClient.score(order));
    // If fraudClient.score() takes > 50ms, TimeLimiter throws TimeoutException
    // Circuit Breaker records this as a failure
    // Retry may attempt again (if configured)
    // Eventually fallback is called
}
```

---


## 12. Security — Protecting Your Platform

### 12.1 How Authentication Works (JWT Tokens)

**The old way (Session-based)**:
1. User logs in → server creates a session in memory → gives user a session cookie
2. Every request: server looks up session in memory → validates user
3. **Problem**: With 20 microservices, which one stores the session? All of them?

**The modern way (JWT — JSON Web Token)**:
1. User logs in → Auth Service creates a JWT token → sends to user
2. User sends JWT with every request
3. Every service can **validate the JWT itself** — no session lookup needed
4. JWT contains the user's information (ID, roles) — services trust it

**What a JWT looks like:**
```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEyMyIsInJvbGVzIjpbIkJVWUVSIl19.signature
     HEADER (base64)        PAYLOAD (base64)                    SIGNATURE
```

Decoded payload:
```json
{
  "sub": "user-abc123",          // Subject = user ID
  "roles": ["BUYER"],            // User's roles
  "tenantId": "seller-xyz",      // Which seller context
  "exp": 1735689600,             // Expiry timestamp (15 minutes from now)
  "iat": 1735688700              // Issued at timestamp
}
```

The **signature** is created using the Auth Service's private key. Any service with the
**public key** can verify the signature — proving the token is genuine and was not tampered with.

### 12.2 Setting Up Spring Security in a Service

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```java
@Configuration
@EnableMethodSecurity    // Activates @PreAuthorize annotations on methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF — not needed for stateless REST APIs (no browser sessions)
            .csrf(csrf -> csrf.disable())
            // Stateless: no HttpSession — each request is self-contained with JWT
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Health check is always public — Kubernetes needs it without auth
                .requestMatchers("/actuator/health/**").permitAll()
                // Product listing is public — anyone can browse products
                .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            // Configure JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // jwtDecoder validates JWT signature using Auth Service's public key
                    // JWKS = JSON Web Key Set — URL where public keys are published
                    .decoder(NimbusJwtDecoder
                        .withJwkSetUri("http://auth-service:8090/oauth2/jwks")
                        .build())
                    // Map JWT "roles" claim to Spring Security GrantedAuthority objects
                    .jwtAuthenticationConverter(jwtAuthConverter())
                )
            )
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");    // Read authorities from "roles" claim
        converter.setAuthorityPrefix("ROLE_");          // Spring needs "ROLE_" prefix
        // So JWT "roles": ["BUYER"] becomes Spring authority "ROLE_BUYER"
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

```java
// Using security in controllers
@RestController
public class OrderController {

    // @PreAuthorize: check before the method runs
    // "hasRole('BUYER')": user must have ROLE_BUYER authority
    @PostMapping("/v1/orders")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest req,
            // @AuthenticationPrincipal gives you the decoded JWT directly
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());    // Extract user ID from token
        return ResponseEntity.ok(orderService.createOrder(userId, req));
    }

    // Only the user who placed the order, or an admin, can view it
    @GetMapping("/v1/orders/{orderId}")
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public ResponseEntity<Order> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
}
```

### 12.3 Secrets Management — Never Store Passwords in Code

**BAD** (never do this):
```yaml
spring:
  datasource:
    password: mySecretPassword123    # ← This gets committed to Git!
```

**GOOD** (use Spring Cloud Vault or environment variables):
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}    # ← ${} means: read from environment variable or Vault
```

**Vault** is a tool that stores secrets securely. Spring Cloud Vault fetches secrets at startup:

```yaml
spring:
  cloud:
    vault:
      uri: http://vault:8200
      authentication: TOKEN
      token: ${VAULT_TOKEN}    # Vault access token (from Kubernetes secret)
      kv:
        enabled: true
        backend: secret
        default-context: order-service    # Read secrets from "secret/order-service" path in Vault
```

In Vault, you store:
```
secret/order-service/database.password = "ultra-secret-db-pass"
secret/order-service/kafka.password    = "kafka-secret"
```

These are injected into your application as if they were in application.yml — but they are never in Git.

---


## 13. Data Management — Each Service Owns Its Data

### The Most Important Rule: Database per Service

**Each service has its own database.** No service can directly read another service's database.

```
Order Service     → connects only to → order_db
Payment Service   → connects only to → payment_db
Product Service   → connects only to → product_db
Inventory Service → connects only to → inventory_db
```

**Why?**

Imagine if Order Service and Inventory Service shared a database. A developer on the Inventory
team runs a slow SQL query that locks some rows. Now Order Service's queries are stuck waiting
for those locks. Order processing stops — because of an Inventory query. This is the classic
shared database problem.

With separate databases:
- Each service's data is private
- One service's slow queries never affect another
- Each team owns and manages their own schema
- Services can use different database technologies (SQL, NoSQL, Redis, Elasticsearch)

### What Database to Use for Each Service

| Service | Database | Why |
|---------|---------|-----|
| User, Order, Payment, Inventory | PostgreSQL | Need transactions (ACID) |
| Product catalogue (reads) | Elasticsearch | Fast full-text search |
| Cart | Redis | Sub-10ms reads, TTL expiry |
| Recommendations | Redis | Pre-computed, fast lookup |
| Search | Elasticsearch | Full-text + faceted search |
| Analytics | ClickHouse / Redshift | Columnar, fast aggregations |

### How to Share Data Between Services

Services cannot join tables, but they need related data. Solutions:

**Option 1: API Calls (for real-time data)**
```
Cart Service needs product price → calls Product Service GET /v1/products/{id}
Order Service needs user address → calls User Service GET /v1/users/{id}/addresses
```

**Option 2: Event-Driven Replication (for frequently needed data)**
When Product price changes:
1. Product Service publishes `ProductPriceUpdated` event
2. Cart Service consumes the event and stores the price locally
3. Next time Cart needs the price, it reads its own local copy — no network call needed

**Option 3: CQRS Read Model**

Product Service stores products in PostgreSQL (write store).
For search, it also maintains an Elasticsearch index (read store).
The Elasticsearch index is populated by consuming `ProductCreated/Updated` Kafka events.

```
Customer searches: GET /v1/search?q=red+shoes
                                     ↓
                               Search Service
                            reads Elasticsearch
                                   (fast!)

Admin updates price: PATCH /v1/products/123
                                  ↓
                           Product Service
                          writes to PostgreSQL
                                   ↓
                          publishes Kafka event
                                   ↓
                        Search Service consumes event
                           updates Elasticsearch index
                              (async, seconds later)
```

### Flyway — Database Migration Management

Every time your schema changes (add a column, create a table), you need a **migration script**.
Flyway runs these scripts automatically in order.

```
db/migration/
├── V1__create_orders_table.sql
├── V2__add_coupon_column.sql
├── V3__create_order_items_table.sql
└── V4__add_index_on_user_id.sql
```

Flyway runs these in order, and tracks which have been applied in a `flyway_schema_history` table.

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```sql
-- V1__create_orders_table.sql
-- This script runs once — Flyway never re-runs it
CREATE TABLE orders (
    order_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- V2__add_coupon_column.sql
-- Safely add a new column — old rows will have NULL for coupon_code
ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50);
```

Spring Boot + Flyway runs migrations automatically on startup. No manual SQL scripts to run.

---


## 14. Design Patterns Explained Simply

Design patterns are proven solutions to common problems. Here are the most important ones for
our ecommerce platform, explained with analogies.

### 14.1 Saga Pattern — Distributed Transactions

**Problem**: An order involves 4 services (Inventory, Payment, Shipping, Notification).
Either ALL steps succeed, or ALL must be undone. But there is no single "commit" across services.

**Analogy**: Booking a holiday package — you book flight, hotel, car separately.
If the hotel is full, you must cancel the flight and car too (compensating transactions).

**Choreography Saga** — Services react to each other's events (no coordinator):

```
Order Service publishes: "OrderCreated"
     ↓
Inventory Service listens → reserves stock → publishes "StockReserved"
     ↓
Payment Service listens → charges card → publishes "PaymentCompleted"
     ↓
Notification Service listens → sends email → done!

If Payment FAILS:
Payment Service publishes "PaymentFailed"
     ↓
Inventory Service listens → RELEASES the reserved stock (compensating action)
     ↓
Order Service listens → marks order CANCELLED → publishes "OrderCancelled"
     ↓
Notification Service listens → sends "Sorry, order cancelled" email
```

```java
// Order Service — starts the saga by publishing an event
@Service
public class OrderService {

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);

        // Publish event to start the saga
        orderEventPublisher.publishOrderCreated(order);
        // The saga is now running! Order Service does not wait for the outcome.
        // It trusts the other services to do their part.

        return order;
    }
}

// Inventory Service — listens and reacts
@Component
public class OrderSagaInventoryHandler {

    @KafkaListener(topics = "order.events", groupId = "inventory-saga")
    public void onOrderCreated(OrderCreatedEvent event) {
        // Reserve stock for each item in the order
        for (OrderItem item : event.getItems()) {
            try {
                inventoryService.reserve(item.getProductId(), item.getQuantity());
            } catch (InsufficientStockException e) {
                // Cannot reserve! Publish failure event to undo the order
                kafkaTemplate.send("inventory.events",
                    new StockReservationFailedEvent(event.getOrderId(), e.getMessage()));
                return;
            }
        }
        // All items reserved successfully — advance the saga
        kafkaTemplate.send("inventory.events",
            new StockReservedEvent(event.getOrderId()));
    }

    @KafkaListener(topics = "payment.events", groupId = "inventory-saga")
    public void onPaymentFailed(PaymentFailedEvent event) {
        // Payment failed — compensate by releasing our reservation
        inventoryService.releaseReservation(event.getOrderId());
        // The customer does not lose stock they cannot pay for
    }
}
```

**Orchestration Saga** — One coordinator service (Saga Orchestrator) tells each service what to do:

```java
// The Orchestrator knows the entire flow and controls each step
@Service
public class OrderSagaOrchestrator {

    public void startSaga(UUID orderId) {
        // Step 1: Ask Inventory to reserve stock
        ReservationResult result = inventoryClient.reserve(orderId);

        if (!result.isSuccess()) {
            // Step 1 failed — cancel order, no further steps needed
            orderService.cancel(orderId, "Stock unavailable");
            return;
        }

        // Step 2: Ask Payment to charge
        PaymentResult payment = paymentClient.charge(orderId);

        if (!payment.isSuccess()) {
            // Step 2 failed — compensate Step 1
            inventoryClient.releaseReservation(orderId);    // Undo step 1
            orderService.cancel(orderId, "Payment failed");
            return;
        }

        // Step 3: Confirm the order
        orderService.confirm(orderId, payment.getTransactionId());
        notificationClient.sendOrderConfirmation(orderId);
    }
}
```

### 14.2 CQRS — Separate Read and Write

**Problem**: Writing orders needs strong consistency (ACID, PostgreSQL).
Reading orders for reporting needs fast aggregations over millions of rows.
These requirements conflict — one database cannot be optimal for both.

**CQRS** = Command Query Responsibility Segregation

```
WRITE side (Commands):
  POST /v1/products  →  Product Service  →  PostgreSQL (normalised, ACID, for writes)

READ side (Queries):
  GET /v1/search?q=shoes  →  Search Service  →  Elasticsearch (denormalised, fast, for reads)

Sync between them:
  ProductUpdated event  →  Kafka  →  Search projection updater  →  Elasticsearch
```

### 14.3 Circuit Breaker — Already covered in Section 11.1

### 14.4 Outbox Pattern — Already covered in Section 10

### 14.5 Idempotent Consumer — Processing Kafka Events Exactly Once

Kafka guarantees **at-least-once** delivery. The same event might arrive twice (network retry).
If you process "OrderCreated" twice, you create 2 orders. Problem!

**Solution**: Track which events you have already processed:

```java
@Component
public class InventoryKafkaConsumer {

    private final ProcessedEventRepository processedEvents;
    private final InventoryService inventoryService;

    @KafkaListener(topics = "order.events")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String eventKey = event.getEventId().toString();

        // Check if we already processed this exact event
        if (processedEvents.existsByEventId(eventKey)) {
            System.out.println("Duplicate event " + eventKey + " — skipping");
            return;    // Already processed, safely ignore
        }

        // Not seen before — process it
        inventoryService.reserve(event.getOrderId(), event.getItems());

        // Record that we processed this event
        // This and the inventory update happen in the same transaction
        processedEvents.save(new ProcessedEvent(eventKey, Instant.now()));
    }
}
```

```java
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;       // UUID of the Kafka event
    private Instant processedAt;
    // Clean up old records after 7 days — events won't replay that far back
}
```

### 14.6 Strangler Fig — Migrating from Monolith to Microservices

If you have an existing monolith and want to migrate to microservices, you do not rewrite
everything at once (that is called a "big bang" migration and it usually fails).

**Strangler Fig** approach — migrate one piece at a time:

```
Step 1: Add API Gateway in front of the monolith
         Customer → API Gateway → Monolith (all traffic still goes to monolith)

Step 2: Extract Notification Service
         Customer → API Gateway
                        ├── /api/notifications/**  → New Notification Service (new code!)
                        └── everything else         → Old Monolith

Step 3: Extract Cart Service
         Customer → API Gateway
                        ├── /api/notifications/**  → New Notification Service
                        ├── /api/carts/**           → New Cart Service
                        └── everything else         → Old Monolith

Step 4: Keep extracting until monolith is gone
```

The monolith is "strangled" gradually, safely, without a risky big-bang rewrite.

**Rule**: Start with services that have **few inbound dependencies** (Notification, Recommendations)
so extraction does not cascade into other teams' work.

---


## 15. Observability — Knowing What's Happening

When something breaks in production, you need to answer: **What happened? Where? When? Why?**
This requires three types of data — the "three pillars of observability".

### 15.1 Logs — What Happened

Logs are text records of events. In microservices, you use **structured JSON logs** because
machines can parse them to find patterns across millions of log lines.

```java
// Add Logback configuration: src/main/resources/logback-spring.xml
// This formats every log line as JSON
```

```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <!-- Include the thread-local MDC values in every log line -->
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
      <includeMdcKeyName>orderId</includeMdcKeyName>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON" />
  </root>
</configuration>
```

```java
@Service
public class OrderService {

    // SLF4J Logger — standard Java logging interface
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public Order createOrder(CreateOrderRequest request) {
        // Put business context into MDC (Mapped Diagnostic Context)
        // MDC is thread-local — every log line in this request automatically includes these
        MDC.put("orderId", request.getOrderId().toString());
        MDC.put("userId", request.getUserId().toString());

        log.info("Creating order for user {}", request.getUserId());
        // This prints as JSON:
        // {"timestamp":"2024-01-15T10:00:00Z","level":"INFO","traceId":"abc123",
        //  "orderId":"xyz789","userId":"user-456","message":"Creating order for user user-456"}

        // ... business logic ...

        log.info("Order {} created successfully, amount {}", orderId, totalAmount);

        MDC.clear();    // Clean up MDC after request completes
        return order;
    }
}
```

### 15.2 Metrics — How Much / How Fast

Metrics are numbers measured over time. Examples:
- How many orders per second?
- What is the P99 response time for the payment endpoint?
- How many errors in the last 5 minutes?

Spring Boot Actuator + Micrometer + Prometheus collects these automatically:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, info    # Expose these endpoints

  # Now visit http://localhost:8081/actuator/prometheus
  # You see hundreds of metrics including:
  #   http_server_requests_seconds_count{uri="/v1/orders",status="200"}  = 1524
  #   http_server_requests_seconds_max{uri="/v1/orders"}                  = 0.456
  #   jvm_memory_used_bytes{area="heap"}                                 = 256000000
```

```java
// Custom business metrics — things Spring does not automatically track
@Service
public class OrderService {

    // MeterRegistry is Spring's interface to the metrics system
    private final MeterRegistry registry;
    private final Counter ordersCreatedCounter;

    public OrderService(MeterRegistry registry) {
        this.registry = registry;
        // Create a counter that tracks how many orders were created
        this.ordersCreatedCounter = Counter.builder("orders.created.total")
            .description("Total number of orders created")
            .tag("region", System.getenv("AWS_REGION"))    // Tag for filtering in dashboards
            .register(registry);
    }

    public Order createOrder(CreateOrderRequest request) {
        Order order = /* ... */;
        ordersCreatedCounter.increment();    // +1 every time an order is created
        return order;
    }
}
```

### 15.3 Distributed Tracing — Following a Request Across Services

A user places an order. The request touches:
API Gateway → Order Service → Inventory Service → Payment Service → Notification Service

If there is a problem, which service caused it? Distributed tracing lets you see the
entire request journey across all 5 services on one timeline.

**Concept**: Every request gets a unique **Trace ID** generated at the API Gateway.
All services pass this Trace ID in their logs and to each other in HTTP headers.
Zipkin collects all the spans and shows them on one timeline.

```
TraceId: abc123
  ├── API Gateway        (0ms – 5ms)
  ├── Order Service      (5ms – 305ms)
  │     ├── DB query     (5ms – 15ms)   → "SELECT orders WHERE..."
  │     ├── Inventory call (20ms – 70ms) → normal
  │     └── Payment call   (75ms – 295ms) ← THIS IS SLOW! 220ms!
  └── Notification Service (306ms – 350ms)
```

From this trace, you immediately see Payment Service is the bottleneck.

**Setup:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0    # Trace 100% of requests (use 0.1 = 10% in high-traffic production)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans    # Where to send traces

logging:
  pattern:
    # Add traceId and spanId to every log line — connects logs to traces
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```

Spring Boot auto-configures trace propagation. Every Feign call automatically adds
`X-B3-TraceId` headers so the next service knows the trace context.

**Start Zipkin:**
```bash
docker run -d -p 9411:9411 openzipkin/zipkin
# Visit http://localhost:9411 to explore traces
```

### 15.4 Health Checks — Is the Service Alive?

Spring Boot Actuator provides `/actuator/health`:

```bash
curl http://localhost:8081/actuator/health
```
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },         // PostgreSQL is reachable
    "diskSpace": { "status": "UP" },  // Enough disk space
    "kafka": { "status": "UP" }       // Kafka is reachable
  }
}
```

If `"status": "DOWN"`, something is wrong. Kubernetes uses this to restart unhealthy pods.

---


## 16. Deployment — Running on Kubernetes

### What is Kubernetes?

Kubernetes (K8s) is a tool that manages running your Docker containers across many servers.
It handles:
- **Starting** your services
- **Restarting** crashed services
- **Scaling** up when traffic increases
- **Health checking** and replacing unhealthy instances
- **Load balancing** traffic across instances

**Analogy**: If Docker is a shipping container, Kubernetes is the shipping company that
decides which ship carries which containers, replaces lost containers, and handles customs.

### From Code to Running Service — Step by Step

**Step 1: Package as a Docker image**

```dockerfile
# Dockerfile in order-service/
# Stage 1: Build (has JDK + Maven)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests    # Compile and create JAR file

# Stage 2: Run (only JRE — smaller image, no build tools in production)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy only the compiled JAR from Stage 1
COPY --from=build /app/target/order-service-1.0.0.jar app.jar
EXPOSE 8082
# JVM flags: use ZGC (low latency GC), set heap size
ENTRYPOINT ["java", "-XX:+UseZGC", "-Xmx512m", "-jar", "app.jar"]
```

```bash
# Build the Docker image
docker build -t order-service:1.0.0 .

# Test it locally
docker run -p 8082:8082 order-service:1.0.0
```

**Step 2: Write a Kubernetes Deployment YAML**

```yaml
# order-service-deployment.yaml
# A Deployment tells Kubernetes: "Run 3 copies of order-service and keep them running"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: ecommerce    # Group related services in a namespace
spec:
  replicas: 3    # Run 3 pods (instances) for high availability
                 # If one server crashes, 2 others keep serving traffic
  selector:
    matchLabels:
      app: order-service    # Kubernetes identifies these pods by this label
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: order-service:1.0.0
          ports:
            - containerPort: 8082
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"    # Use production configuration
            - name: DB_PASSWORD
              # Read from Kubernetes Secret — never hardcode passwords in YAML
              valueFrom:
                secretKeyRef:
                  name: order-service-secrets    # Name of the Kubernetes Secret
                  key: db-password
          resources:
            requests:
              cpu: "500m"        # 0.5 CPU cores — minimum guaranteed
              memory: "1Gi"      # 1 GB RAM — minimum guaranteed
            limits:
              cpu: "2"           # Max 2 CPU cores
              memory: "2Gi"      # Max 2 GB RAM
          # Liveness probe: is the JVM alive? If this fails, Kubernetes RESTARTS the pod
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            initialDelaySeconds: 60    # Wait 60s before first check (Spring Boot needs time to start)
            periodSeconds: 30          # Check every 30 seconds
            failureThreshold: 3        # 3 consecutive failures → restart

          # Readiness probe: can this pod serve traffic?
          # If this fails, Kubernetes STOPS sending traffic to this pod (but doesn't restart it)
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          # Why two probes?
          # Liveness: "Is the pod alive?" (should I restart it?)
          # Readiness: "Is the pod READY to accept traffic?" (should I send requests to it?)
          # Example: A pod is alive (JVM running) but still warming up caches.
          # Readiness fails → no traffic yet. Liveness passes → don't restart.
```

**Step 3: Create a Service (Load Balancer)**

```yaml
# order-service-svc.yaml
# A Service provides a stable address to reach pods (pods get random IPs on each restart)
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: ecommerce
spec:
  selector:
    app: order-service    # Route to pods with this label
  ports:
    - port: 80            # External port (what other services call)
      targetPort: 8082    # Internal port (what the pod listens on)
  type: ClusterIP    # Internal only — not exposed to internet directly
                     # Only accessible from inside the cluster (other services)
```

**Step 4: Auto-Scaling**

```yaml
# order-service-hpa.yaml (HorizontalPodAutoscaler)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3      # Never run fewer than 3 instances
  maxReplicas: 12     # Scale up to 12 on Black Friday
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70    # Scale up when average CPU > 70%
          # Kubernetes adds pods until average CPU drops below 70%
```

**Step 5: Apply to Kubernetes**

```bash
# Apply all YAML files — Kubernetes reads them and creates the resources
kubectl apply -f order-service-deployment.yaml
kubectl apply -f order-service-svc.yaml
kubectl apply -f order-service-hpa.yaml

# Check the pods are running
kubectl get pods -n ecommerce
# NAME                             READY   STATUS    RESTARTS   AGE
# order-service-7d9b6bc4f-abc12   1/1     Running   0          2m
# order-service-7d9b6bc4f-def34   1/1     Running   0          2m
# order-service-7d9b6bc4f-ghi56   1/1     Running   0          2m

# See logs from a pod
kubectl logs -n ecommerce order-service-7d9b6bc4f-abc12 --follow
```

---


## 17. Learning Roadmap

You now have the blueprint. Here is how to build your skills step by step.

### Phase 1: Local Development (Weeks 1–4)

**Goal**: Get one service running end-to-end on your laptop.

- [ ] Install Java 17, Maven, IntelliJ IDEA, Docker Desktop
- [ ] Create `product-service` from Spring Initializr
- [ ] Start PostgreSQL via Docker
- [ ] Implement Product entity, repository, service, controller
- [ ] Test with Postman or curl — create a product, read it back
- [ ] Add Flyway migrations
- [ ] Set up Eureka Server (1 node)
- [ ] Register product-service with Eureka

**Milestone**: POST a product, GET it back, see it on Eureka dashboard.

### Phase 2: Service-to-Service Communication (Weeks 5–8)

**Goal**: Make services talk to each other.

- [ ] Create `order-service`
- [ ] Create `inventory-service`
- [ ] Add OpenFeign to order-service
- [ ] Order Service calls Inventory Service to check stock
- [ ] Add Circuit Breaker with Resilience4j
- [ ] Set up Config Server
- [ ] Set up API Gateway with one route

**Milestone**: Order service calls inventory, gateway routes to the correct service.

### Phase 3: Messaging and Events (Weeks 9–12)

**Goal**: Decouple services with Kafka.

- [ ] Start Kafka via Docker Compose
- [ ] Order Service publishes `OrderCreated` event
- [ ] Create `notification-service` that consumes the event and logs it
- [ ] Implement Outbox Pattern in Order Service
- [ ] Implement Idempotent Consumer in Notification Service

**Milestone**: Place an order → Notification Service receives the event and prints it.

### Phase 4: Security and Observability (Weeks 13–16)

**Goal**: Secure the platform and understand what is happening.

- [ ] Set up Auth Service with Spring Authorization Server
- [ ] Configure JWT validation in API Gateway
- [ ] Add `@PreAuthorize` to controllers
- [ ] Set up Zipkin for distributed tracing
- [ ] Add structured JSON logging
- [ ] Add Prometheus metrics

**Milestone**: Protected endpoints, trace a request through 3 services in Zipkin.

### Phase 5: Production Readiness (Weeks 17–20)

**Goal**: Run on Kubernetes.

- [ ] Write Dockerfiles for all services
- [ ] Write Kubernetes Deployment, Service, HPA YAML
- [ ] Install Docker Desktop Kubernetes (or use Minikube)
- [ ] Deploy all services to local Kubernetes
- [ ] Test auto-scaling by generating load

**Milestone**: All services running in Kubernetes, auto-scaling works.

---

### Recommended Resources

| Topic | Resource |
|-------|---------|
| Java fundamentals | "Effective Java" by Joshua Bloch |
| Spring Boot | spring.io/guides (official, free) |
| Microservices theory | "Building Microservices" by Sam Newman |
| Kafka | kafka.apache.org/documentation (official) |
| Kubernetes | "Kubernetes in Action" by Marko Luksa |
| Spring Cloud | "Spring Microservices in Action" by John Carnell |

### Common Beginner Mistakes to Avoid

1. **Don't make everything async** — Start with synchronous REST calls. Add Kafka when you
   actually need decoupling. Async adds complexity.

2. **Don't start with 20 services** — Build 3 services first. Understand the communication
   patterns deeply before adding more.

3. **Don't skip health checks** — Always add `spring-boot-starter-actuator` and configure
   liveness/readiness probes. Kubernetes cannot manage your service without them.

4. **Don't hardcode configuration** — Use `application.yml` from day one. Even for local
   development, get used to the pattern.

5. **Don't ignore database transactions** — Learn what `@Transactional` does before using
   multiple services. Distributed transactions (Saga) are advanced — master local transactions first.

6. **Log everything during development** — Set `logging.level.com.ecommerce=DEBUG` while
   learning. Reduce to INFO in production.

---

*This document is a beginner's learning guide. For production-depth architecture (SLA targets,
performance tuning, security hardening), refer to the main design.md file.*

*Happy learning! Building this platform step by step will teach you everything about
enterprise Java microservices. Take it one phase at a time.* 🚀

