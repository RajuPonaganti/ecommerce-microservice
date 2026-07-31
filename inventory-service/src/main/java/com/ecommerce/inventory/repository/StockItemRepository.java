package com.ecommerce.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.inventory.model.StockItem;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

	Optional<StockItem> findByProductId(UUID productId);
}
