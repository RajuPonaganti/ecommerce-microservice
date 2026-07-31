package com.ecommerce.inventory.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stock_item")
@Setter
@Getter
@Builder
public class StockItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID stockItemId;

	private UUID productId;
	
	private UUID waeHouseId;
	// Total physical stock: 50 units
	private int quantityOnHand; 
	 // sum of active reservations
	private int quantityReserved;
	//quantityAvailable = antityOnHand - quantityReserved
	private int quantityAvailable;

	// @Version is the magic annotation for optimistic locking
	// Each time this row is updated, the version number increments
	// If two threads try to update the same row simultaneously, only ONE succeeds
	// The other gets an OptimisticLockException and must retry
	@Version
	private Long version;
	
	
	public int getAvailableQuantity(){
		return this.quantityAvailable =  this.quantityOnHand-this.quantityReserved;
	}
	
	public void reserve(int quantity) {
		if(getAvailableQuantity()< quantity) 
			throw new RuntimeException("Not enough stock");
		this.quantityReserved+=quantity;
	}
}
