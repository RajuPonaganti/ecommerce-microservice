package com.ecommerce.order.enums;

public enum OrderStatus {
	CREATED, // Order received but not processed yet
	PAYMENT_PENDING, // Waiting for payment confirmation
	PAYMENT_CONFIRMED, // Payment succeeded
	INVENTORY_RESERVED, // Stock is held for this order
	PROCESSING, // Being prepared in warehouse
	SHIPPED, // Handed to courier
	DELIVERED, // Customer received it
	CANCELLED, // Order cancelled
	REFUND_INITIATED // Return in progress
}
