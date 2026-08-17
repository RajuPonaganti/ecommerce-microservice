package com.ecommerce.inventory.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_reservation_item")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationItem {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID stockReservationItemId;
	
	private UUID productId;

	// (unique per seller)
	private String sku;

	private int quantity;

	private BigDecimal unitPrice;
	
	private UUID sellerId;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_reservation_id", nullable = false)
    private StockReservation stockReservation;
}
