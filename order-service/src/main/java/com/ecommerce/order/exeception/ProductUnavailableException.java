package com.ecommerce.order.exeception;

public class ProductUnavailableException extends RuntimeException {

	public ProductUnavailableException(String message) {
		super(message);
	}

}
