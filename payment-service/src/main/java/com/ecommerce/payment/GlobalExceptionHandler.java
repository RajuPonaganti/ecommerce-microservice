package com.ecommerce.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.payment.exception.StockNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> map = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e->map.put(e.getField(), e.getCode()));
		return ResponseEntity.badRequest().body(map);

	}
	
	@ExceptionHandler(StockNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleStockNotFound(StockNotFoundException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		pd.setTitle("Stock Not Found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
	}

}
