package com.ecommerce.prodct.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product")
@Setter
@Getter
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID productId;
	
	// @Column specifies the database column properties
    @Column(nullable = false, length = 500)  // nullable=false means NOT NULL in SQL
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)  // e.g., 999999999.99
    private BigDecimal price;

    @Column(nullable = false)
    private String category;

    // @Enumerated stores enum as a string in the DB (not a number)
    // Using STRING is safer — if you reorder enum values, DB is not affected
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.DRAFT;
}
