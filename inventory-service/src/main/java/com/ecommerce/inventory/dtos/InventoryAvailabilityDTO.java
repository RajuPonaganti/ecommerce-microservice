package com.ecommerce.inventory.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InventoryAvailabilityDTO {

	private UUID productId;
	private int quantityAvailable;
	private int requiredQuantity;
	private boolean availableForOrdering;
}
