package com.ecommerce.inventory.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_reservation")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID reservationId;

	private UUID warehouseId;
	
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	private StockReservationStatus status;

	private Instant expiresAt;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "stockReservation")
	private List<StockReservationItem> stockReservationItemm;
}
