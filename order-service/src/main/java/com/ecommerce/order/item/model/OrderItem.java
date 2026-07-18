package com.ecommerce.order.item.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_item")
@Setter
@Getter
public class OrderItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID orderId;

	// (unique per seller)
	private String sku;

	private int quantity;

	private BigDecimal unitPrice;
	
	private UUID sellerId;
}
