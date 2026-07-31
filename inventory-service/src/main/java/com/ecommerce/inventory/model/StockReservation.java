package com.ecommerce.inventory.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stock_reservation")
@Setter
@Getter
public class StockReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID reservationId;

	private UUID productId;
	private UUID warehouseId;
	private int quantity;
	private UUID orderId;
	@Enumerated(EnumType.STRING)
	private StockReservationStatus status;
	private Instant expiresAt;
}
