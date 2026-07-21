package com.ecommerce.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ecommerce.order.item.model.OrderItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order")
@Setter
@Getter
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID OrderId;
	
	private UUID userId;    // Who placed the order (reference to User Service)

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // OneToMany: one Order has many OrderItems
    // cascade ALL: when you save an Order, all its items are also saved
    // LAZY: don't load items from DB until they are actually accessed (performance)
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    
    private String couponCode;
    private String paymentRef;

    private Instant createdAt = Instant.now();
    
    @Version
    private long version;
}
