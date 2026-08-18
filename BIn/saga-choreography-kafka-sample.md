# Saga Pattern (Choreography) — Order → Inventory → Payment → Notification

## 1. Shared Event Contracts (put in a `common-events` module, shared JAR)

```java
// ---------- Base event ----------
public abstract class BaseEvent {
    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId;
    private final Instant occurredAt = Instant.now();

    protected BaseEvent(UUID orderId) {
        this.orderId = orderId;
    }
    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public Instant getOccurredAt() { return occurredAt; }
}

// ---------- Happy path events ----------
public class OrderCreatedEvent extends BaseEvent {
    private final List<OrderItemDTO> items;
    private final BigDecimal finalAmount;
    private final UUID userId;

    public OrderCreatedEvent(UUID orderId, UUID userId, List<OrderItemDTO> items, BigDecimal finalAmount) {
        super(orderId);
        this.userId = userId;
        this.items = items;
        this.finalAmount = finalAmount;
    }
    // getters
}

public class StockReservedEvent extends BaseEvent {
    private final String reservationId; // used later to release/confirm
    public StockReservedEvent(UUID orderId, String reservationId) {
        super(orderId);
        this.reservationId = reservationId;
    }
    // getters
}

public class PaymentCompletedEvent extends BaseEvent {
    private final String paymentTransactionId;
    public PaymentCompletedEvent(UUID orderId, String paymentTransactionId) {
        super(orderId);
        this.paymentTransactionId = paymentTransactionId;
    }
    // getters
}

// ---------- Failure / compensation events ----------
public class StockReservationFailedEvent extends BaseEvent {
    private final String reason;
    public StockReservationFailedEvent(UUID orderId, String reason) {
        super(orderId); this.reason = reason;
    }
}

public class PaymentFailedEvent extends BaseEvent {
    private final String reason;
    public PaymentFailedEvent(UUID orderId, String reason) {
        super(orderId); this.reason = reason;
    }
}
```

**Topic naming convention** (do this from day 1, saves pain later):
```
order.created.v1
inventory.stock-reserved.v1
inventory.stock-reservation-failed.v1
payment.completed.v1
payment.failed.v1
```
Version suffix (`.v1`) lets you evolve the schema without breaking old consumers.

---

## 2. Order Service — publishes the saga's first event, reacts to everything after

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public UUID createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .items(request.getItems())
                .status(OrderStatus.CREATED)
                .finalAmount(request.getFinalAmount())
                .build();

        orderRepository.save(order); // 1. persist locally FIRST (source of truth)

        // 2. publish event — key = orderId so all events for this order
        //    land on the same partition, preserving order of processing
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(), order.getUserId(), request.getItems(), order.getFinalAmount());

        kafkaTemplate.send("order.created.v1", order.getId().toString(), event);

        return order.getId();
    }

    // ---- reacts to Inventory's outcome ----
    @KafkaListener(topics = "inventory.stock-reservation-failed.v1", groupId = "order-service")
    @Transactional
    public void onStockReservationFailed(StockReservationFailedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow();
        order.markCancelled(event.getReason());   // compensation: nothing paid yet, just cancel
        orderRepository.save(order);
        // optionally publish OrderCancelled -> Notification service listens too
    }

    // ---- reacts to Payment's outcome ----
    @KafkaListener(topics = "payment.completed.v1", groupId = "order-service")
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.markPaymentConfirmed(event.getPaymentTransactionId());
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
    }

    @KafkaListener(topics = "payment.failed.v1", groupId = "order-service")
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        // Compensation: stock was already reserved by Inventory Service.
        // Order Service does NOT call Inventory directly — it publishes an
        // event, and Inventory Service is responsible for releasing its own stock.
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
        order.markCancelled("Payment failed: " + event.getReason());
        orderRepository.save(order);

        kafkaTemplate.send("order.cancelled.v1", order.getId().toString(),
                new OrderCancelledEvent(order.getId(), "PAYMENT_FAILED"));
    }
}
```

**Key teaching point:** Order Service never says "hey Inventory, release the stock." It only ever publishes `OrderCancelled`. Inventory Service is the one that decides *how* to undo its own work — that's what makes this choreography and not a disguised RPC call.

---

## 3. Inventory Service — reserves stock, and knows how to un-reserve it

```java
@Service
@RequiredArgsConstructor
public class InventoryEventHandler {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.created.v1", groupId = "inventory-service")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            // Idempotency guard — Kafka gives "at-least-once" delivery,
            // so the SAME event can arrive twice. Never skip this check.
            if (reservationRepository.existsByOrderId(event.getOrderId())) {
                return; // already processed, ignore duplicate
            }

            for (OrderItemDTO item : event.getItems()) {
                inventoryRepository.decrementStock(item.getSku(), item.getQuantity());
                // decrementStock uses an atomic UPDATE ... WHERE stock >= ?
                // to avoid overselling under concurrent orders
            }

            String reservationId = UUID.randomUUID().toString();
            reservationRepository.save(new Reservation(reservationId, event.getOrderId(), event.getItems()));

            kafkaTemplate.send("inventory.stock-reserved.v1", event.getOrderId().toString(),
                    new StockReservedEvent(event.getOrderId(), reservationId));

        } catch (InsufficientStockException ex) {
            kafkaTemplate.send("inventory.stock-reservation-failed.v1", event.getOrderId().toString(),
                    new StockReservationFailedEvent(event.getOrderId(), ex.getMessage()));
        }
    }

    // ---- compensation: triggered when a LATER step (Payment) fails ----
    @KafkaListener(topics = "order.cancelled.v1", groupId = "inventory-service")
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        Reservation reservation = reservationRepository.findByOrderId(event.getOrderId())
                .orElse(null);
        if (reservation == null) return; // nothing was reserved, nothing to undo

        for (OrderItemDTO item : reservation.getItems()) {
            inventoryRepository.incrementStock(item.getSku(), item.getQuantity()); // give stock back
        }
        reservationRepository.delete(reservation);
    }
}
```

---

## 4. Payment Service — the step most likely to fail, so it drives the compensation chain

```java
@Service
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentGatewayClient gatewayClient; // wraps Razorpay/Stripe etc.
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "inventory.stock-reserved.v1", groupId = "payment-service")
    @Transactional
    public void onStockReserved(StockReservedEvent event) {
        if (paymentRepository.existsByOrderId(event.getOrderId())) return; // idempotency

        try {
            // idempotencyKey = orderId ensures the payment gateway itself
            // won't double-charge on retry
            PaymentResult result = gatewayClient.charge(event.getOrderId().toString());

            paymentRepository.save(Payment.builder()
                    .orderId(event.getOrderId())
                    .transactionId(result.getTransactionId())
                    .status(PaymentStatus.SUCCESS)
                    .build());

            kafkaTemplate.send("payment.completed.v1", event.getOrderId().toString(),
                    new PaymentCompletedEvent(event.getOrderId(), result.getTransactionId()));

        } catch (PaymentDeclinedException ex) {
            paymentRepository.save(Payment.builder()
                    .orderId(event.getOrderId())
                    .status(PaymentStatus.FAILED)
                    .build());

            // This event is what triggers BOTH Order Service (cancel order)
            // AND, indirectly via OrderCancelled, Inventory Service (release stock)
            kafkaTemplate.send("payment.failed.v1", event.getOrderId().toString(),
                    new PaymentFailedEvent(event.getOrderId(), ex.getMessage()));
        }
    }
}
```

---

## 5. Notification Service — pure side-effect listener, never publishes saga events

```java
@Service
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final EmailClient emailClient;
    private final SmsClient smsClient;

    @KafkaListener(topics = "payment.completed.v1", groupId = "notification-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        emailClient.send(event.getOrderId(), "order-confirmed-template");
    }

    @KafkaListener(topics = "order.cancelled.v1", groupId = "notification-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        smsClient.send(event.getOrderId(), "order-cancelled-template");
    }
}
```

Notice: Notification Service is a **leaf node**. It listens but never publishes anything the saga depends on. This is deliberate — if it goes down, the saga still completes correctly (you just lose an email, which you can retry/backfill later). That's a good sanity check when designing your own topology: ask "if this service is down, does the saga still reach a consistent end state?"

---

## 6. Kafka producer config — the part people get wrong in interviews

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // acks=all -> wait for all in-sync replicas before ack.
        // Non-negotiable for saga events; you cannot afford to lose an OrderCreated.
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // dedupes producer retries
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}
```

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Manual offset commit: only advance the offset AFTER your @Transactional
        // DB write + idempotency check succeed — never before.
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.yourorg.events");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cf) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Dead-letter topic for events that fail after retries — don't let
        // a poison message block the whole partition forever
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate()),
                new FixedBackOff(1000L, 3)));
        return factory;
    }
}
```

---

## The full picture — what "consistency" actually means here

There's no global transaction. At any given moment mid-saga, the system is in a state where Order says `PAYMENT_PENDING` but Inventory has already decremented stock. That's **fine** — this is called eventual consistency. What you must guarantee is:

1. Every event has exactly one owner-service that reacts to it.
2. Every service that *changes state* on a forward event also has a listener for the compensating event that undoes it.
3. Idempotency checks exist on every consumer (Kafka is at-least-once, duplicates *will* happen).
4. `orderId` is always the Kafka message **key**, so all events for one order are strictly ordered on one partition — you never process `PaymentCompleted` before `StockReserved` for the same order.

## Interview angle (since this is common in senior Java interviews)

- **"Why not 2PC/XA across microservices?"** → Distributed locks across services kill availability and don't survive network partitions well; Saga trades strong consistency for availability + partition tolerance (this is literally the CAP trade-off, and it maps to why you covered Eureka's CAP behavior in an earlier KT).
- **"Choreography vs Orchestration — when do you switch?"** → Once you need visibility/debuggability of "which step is this order stuck on," or the flow has branching logic (retry N times, then escalate), orchestration (a `SagaOrchestrator` service explicitly calling each step and tracking a state machine) becomes easier to reason about than N services each holding a partial view.
