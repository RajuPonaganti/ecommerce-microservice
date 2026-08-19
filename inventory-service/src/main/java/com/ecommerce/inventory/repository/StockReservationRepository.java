package com.ecommerce.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.inventory.model.StockReservation;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

	boolean existsByOrderId(UUID orderId);

	Optional<StockReservation> findByOrderId(UUID orderId);
}
