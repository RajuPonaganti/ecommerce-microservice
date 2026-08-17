package com.ecommerce.inventory.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryDTO {
	
	private UUID productId;

	private UUID waeHouseId;
	// Total physical stock: 50 units
	
	@NotNull(message = "enter productId")
	private Integer quantityOnHand;

}
