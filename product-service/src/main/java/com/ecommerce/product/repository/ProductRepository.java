package com.ecommerce.product.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, UUID>{
	

}
