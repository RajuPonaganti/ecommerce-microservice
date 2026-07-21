package com.ecommerce.product.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	Page<Product> findByCategoryAndBrand(String category, String brand, Pageable pageable);

	Page<Product> findByCategory(String category, Pageable pageable);

	Page<Product> findByBrand(String brand, Pageable pageable);
}
