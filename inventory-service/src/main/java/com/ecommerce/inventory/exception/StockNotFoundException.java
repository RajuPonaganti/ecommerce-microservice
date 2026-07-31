package com.ecommerce.inventory.exception;

public class StockNotFoundException extends RuntimeException {

	public StockNotFoundException(String message) {
		super(message);
	}
}
