package com.ecommerce.payment.exception;

public class StockNotFoundException extends RuntimeException {

	public StockNotFoundException(String message) {
		super(message);
	}
}
