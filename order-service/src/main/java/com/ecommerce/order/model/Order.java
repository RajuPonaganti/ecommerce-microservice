package com.ecommerce.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ecommerce.order.enums.OrderStatus;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_order")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID orderId;
	
	private UUID userId;    // Who placed the order (reference to User Service)

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "torder")
    // OneToMany: one Order has many OrderItems
    // cascade ALL: when you save an Order, all its items are also saved
    // LAZY: don't load items from DB until they are actually accessed (performance)
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    
    private String couponCode;
    private String paymentRef;

    private Instant createdAt = Instant.now();
    
	@Version
    private Long version;  // boxed Long — null means new entity, Hibernate sets it on first INSERT
}
