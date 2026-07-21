package com.ecommerce.inventory.service;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.inventory.model.StockReservation;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID>{

}
