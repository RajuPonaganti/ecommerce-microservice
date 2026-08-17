package com.ecommerce.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ecommerce.inventory.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

	Optional<Inventory> findByProductId(UUID productId);

	@Modifying
	@Query("""
	    UPDATE Inventory i
	    SET i.quantityAvailable = i.quantityAvailable - :quantity,
	        i.quantityReserved  = i.quantityReserved  + :quantity
	    WHERE i.productId = :productId
	      AND i.quantityAvailable >= :quantity
	""")
	int decrementStock(UUID productId, int quantity);
}
