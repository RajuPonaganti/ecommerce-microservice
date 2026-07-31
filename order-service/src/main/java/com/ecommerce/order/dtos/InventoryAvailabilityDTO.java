package com.ecommerce.order.dtos;

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
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InventoryAvailabilityDTO [productId=");
		builder.append(productId);
		builder.append(", quantityAvailable=");
		builder.append(quantityAvailable);
		builder.append(", requiredQuantity=");
		builder.append(requiredQuantity);
		builder.append(", availableForOrdering=");
		builder.append(availableForOrdering);
		builder.append("]");
		return builder.toString();
	}
	
}
